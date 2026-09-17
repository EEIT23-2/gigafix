package com.gigafix.repair.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gigafix.repair.exception.InvalidFileFormatException;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// 維修單/技師/分店 匯出匯入共用工具：一列資料用 LinkedHashMap<欄位名稱, 字串值> 表示，
// 三種格式(json/xml/xlsx)最後都轉成同一種中介格式，上層 Service 只需要處理欄位驗證跟新增/更新
@Component
@RequiredArgsConstructor
public class TableExportImport {

	private final ObjectMapper objectMapper;

	// 依格式決定下載檔名跟 Content-Type
	public static HttpHeaders buildDownloadHeaders(String format, String baseFileName) {
		String contentType = switch (format) {
			case "json" -> MediaType.APPLICATION_JSON_VALUE;
			case "xml" -> MediaType.APPLICATION_XML_VALUE;
			case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
			default -> throw new InvalidFileFormatException("不支援的匯出格式: " + format);
		};
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + baseFileName + "." + format);
		headers.add(HttpHeaders.CONTENT_TYPE, contentType);
		return headers;
	}

	// ========== JSON ==========

	public byte[] toJson(List<? extends Map<String, String>> rows) {
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(rows);
	}

	public List<Map<String, String>> fromJson(InputStream in) {
		try {
			return objectMapper.readValue(in, new TypeReference<List<Map<String, String>>>() {
			});
		} catch (Exception e) {
			throw new InvalidFileFormatException("JSON格式錯誤，請確認檔案內容", e);
		}
	}

	// ========== XML(純文字) ==========

	public byte[] toXml(String rootTag, String rowTag, List<? extends Map<String, String>> rows) {
		try {
			DocumentBuilder builder = newSecureDocumentBuilder();
			Document doc = builder.newDocument();
			Element root = doc.createElement(rootTag);
			doc.appendChild(root);
			for (Map<String, String> row : rows) {
				Element rowEl = doc.createElement(rowTag);
				for (Map.Entry<String, String> field : row.entrySet()) {
					Element fieldEl = doc.createElement(field.getKey());
					fieldEl.setTextContent(field.getValue() == null ? "" : field.getValue());
					rowEl.appendChild(fieldEl);
				}
				root.appendChild(rowEl);
			}

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(doc), new StreamResult(out));
			return out.toByteArray();
		} catch (ParserConfigurationException | TransformerException e) {
			throw new InvalidFileFormatException("XML匯出失敗", e);
		}
	}

	public List<Map<String, String>> fromXml(InputStream in, String rowTag) {
		try {
			DocumentBuilder builder = newSecureDocumentBuilder();
			Document doc = builder.parse(in);
			NodeList rowNodes = doc.getElementsByTagName(rowTag);
			List<Map<String, String>> rows = new ArrayList<>();
			for (int i = 0; i < rowNodes.getLength(); i++) {
				Element rowEl = (Element) rowNodes.item(i);
				LinkedHashMap<String, String> row = new LinkedHashMap<>();
				NodeList children = rowEl.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					Node node = children.item(j);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						row.put(node.getNodeName(), node.getTextContent());
					}
				}
				rows.add(row);
			}
			return rows;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new InvalidFileFormatException("XML格式錯誤，請確認檔案內容", e);
		}
	}

	// 建立DocumentBuilder時關閉外部實體解析，避免上傳的XML檔案被拿來做XXE攻擊
	private DocumentBuilder newSecureDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder();
	}

	// ========== Excel ==========

	public byte[] toExcel(List<String> headers, List<? extends Map<String, String>> rows) {
		return toExcel(headers, rows, ExcelExportOptions.builder().build());
	}

	// options 可以設定：強制文字格式的欄位、鎖定不能編輯的欄位(通常是id)、下拉選單欄位、預留幾列空白列
	public byte[] toExcel(List<String> headers, List<? extends Map<String, String>> rows, ExcelExportOptions options) {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			XSSFSheet sheet = wb.createSheet("data");

			CellStyle lockedStyle = wb.createCellStyle();
			lockedStyle.setLocked(true);
			CellStyle lockedTextStyle = wb.createCellStyle();
			lockedTextStyle.setLocked(true);
			lockedTextStyle.setDataFormat(wb.createDataFormat().getFormat("@"));
			CellStyle unlockedStyle = wb.createCellStyle();
			unlockedStyle.setLocked(false);
			CellStyle unlockedTextStyle = wb.createCellStyle();
			unlockedTextStyle.setLocked(false);
			unlockedTextStyle.setDataFormat(wb.createDataFormat().getFormat("@"));

			Row headerRow = sheet.createRow(0);
			for (int c = 0; c < headers.size(); c++) {
				Cell cell = headerRow.createCell(c);
				cell.setCellValue(headers.get(c));
				cell.setCellStyle(lockedStyle);
				sheet.setColumnWidth(c, 18 * 256); // 預設欄寬加大，避免電話號碼被遮住；使用者之後也能自行拖曳調整
			}

			int totalDataRows = rows.size() + options.getExtraBlankRows();
			for (int r = 0; r < totalDataRows; r++) {
				Row excelRow = sheet.createRow(r + 1);
				Map<String, String> data = r < rows.size() ? rows.get(r) : null;
				for (int c = 0; c < headers.size(); c++) {
					String col = headers.get(c);
					boolean isLockedCol = col.equals(options.getLockedColumn());
					boolean isTextCol = options.getTextColumns().contains(col);
					Cell cell = excelRow.createCell(c);
					cell.setCellValue(data == null ? "" : data.getOrDefault(col, ""));
					cell.setCellStyle(isLockedCol ? (isTextCol ? lockedTextStyle : lockedStyle)
							: (isTextCol ? unlockedTextStyle : unlockedStyle));
				}
			}

			for (Map.Entry<String, List<String>> entry : options.getDropdownColumns().entrySet()) {
				int colIndex = headers.indexOf(entry.getKey());
				if (colIndex < 0 || totalDataRows == 0) {
					continue;
				}
				DataValidationHelper validationHelper = sheet.getDataValidationHelper();
				DataValidationConstraint constraint = validationHelper
						.createExplicitListConstraint(entry.getValue().toArray(new String[0]));
				CellRangeAddressList addressList = new CellRangeAddressList(1, totalDataRows, colIndex, colIndex);
				DataValidation validation = validationHelper.createValidation(constraint, addressList);
				validation.setShowErrorBox(true);
				validation.createErrorBox("輸入錯誤", "請從下拉選單選擇有效的選項");
				sheet.addValidationData(validation);
			}

			// 鎖定欄位要靠「保護工作表」才會生效；只保護編輯儲存格，仍允許新增/刪除列、調整欄寬列高，且不設密碼(純防呆，非安全機制)
			if (options.getLockedColumn() != null) {
				sheet.protectSheet("");
				sheet.lockInsertRows(false);
				sheet.lockDeleteRows(false);
				sheet.lockFormatColumns(false);
				sheet.lockFormatRows(false);
				addHints(wb, sheet, headers, rows.size(), options);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new InvalidFileFormatException("Excel匯出失敗", e);
		}
	}

	// 用 Excel 註解(不是儲存格內容，不會被匯入邏輯誤判成資料)提示：id欄位為什麼不能編輯、新增資料要從哪裡開始填
	private void addHints(XSSFWorkbook wb, XSSFSheet sheet, List<String> headers, int dataRowCount,
			ExcelExportOptions options) {
		int lockedColIndex = headers.indexOf(options.getLockedColumn());
		if (lockedColIndex < 0) {
			return;
		}
		Drawing<?> drawing = sheet.createDrawingPatriarch();
		CreationHelper factory = wb.getCreationHelper();

		Cell idHeaderCell = sheet.getRow(0).getCell(lockedColIndex);
		ClientAnchor idAnchor = factory.createClientAnchor();
		idAnchor.setCol1(lockedColIndex);
		idAnchor.setCol2(lockedColIndex + 3);
		idAnchor.setRow1(0);
		idAnchor.setRow2(5);
		Comment idComment = drawing.createCellComment(idAnchor);
		idComment.setString(factory.createRichTextString(
				"此欄位已鎖定，不能編輯。\n新增資料時請留空，系統會自動產生id。\n"
						+ "若要整列清空重填，請對列號按右鍵「刪除」整列；或框選時避開這一欄，只清空其他欄位。"));
		idComment.setAuthor("GigaFix");
		idHeaderCell.setCellComment(idComment);

		if (options.getExtraBlankRows() > 0) {
			int hintCol = 0;
			for (int c = 0; c < headers.size(); c++) {
				if (!headers.get(c).equals(options.getLockedColumn())) {
					hintCol = c;
					break;
				}
			}
			int hintRowIndex = dataRowCount + 1; // 資料列結束後的第一個空白列(0-based，因為第0列是標題)
			Cell hintCell = sheet.getRow(hintRowIndex).getCell(hintCol);
			ClientAnchor hintAnchor = factory.createClientAnchor();
			hintAnchor.setCol1(hintCol);
			hintAnchor.setCol2(hintCol + 3);
			hintAnchor.setRow1(hintRowIndex);
			hintAnchor.setRow2(hintRowIndex + 3);
			Comment hintComment = drawing.createCellComment(hintAnchor);
			hintComment.setString(factory.createRichTextString("請從這裡開始新增新資料，id欄位留空即可，系統會自動產生。"));
			hintComment.setAuthor("GigaFix");
			hintCell.setCellComment(hintComment);
		}
	}

	public List<Map<String, String>> fromExcel(InputStream in, List<String> headers) {
		try (Workbook wb = WorkbookFactory.create(in)) {
			Sheet sheet = wb.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			List<Map<String, String>> rows = new ArrayList<>();
			for (int r = 1; r <= sheet.getLastRowNum(); r++) {
				Row excelRow = sheet.getRow(r);
				if (excelRow == null) {
					continue;
				}
				LinkedHashMap<String, String> row = new LinkedHashMap<>();
				boolean allBlank = true;
				for (int c = 0; c < headers.size(); c++) {
					Cell cell = excelRow.getCell(c);
					String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
					if (!value.isEmpty()) {
						allBlank = false;
					}
					row.put(headers.get(c), value);
				}
				if (!allBlank) {
					rows.add(row);
				}
			}
			return rows;
		} catch (IOException e) {
			throw new InvalidFileFormatException("Excel格式錯誤，請確認檔案內容", e);
		}
	}

}
