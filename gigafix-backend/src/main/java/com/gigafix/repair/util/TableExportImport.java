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
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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

	public byte[] toJson(List<LinkedHashMap<String, String>> rows) {
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(rows);
	}

	public List<LinkedHashMap<String, String>> fromJson(InputStream in) {
		try {
			return objectMapper.readValue(in, new TypeReference<List<LinkedHashMap<String, String>>>() {
			});
		} catch (Exception e) {
			throw new InvalidFileFormatException("JSON格式錯誤，請確認檔案內容", e);
		}
	}

	// ========== XML(純文字) ==========

	public byte[] toXml(String rootTag, String rowTag, List<LinkedHashMap<String, String>> rows) {
		try {
			DocumentBuilder builder = newSecureDocumentBuilder();
			Document doc = builder.newDocument();
			Element root = doc.createElement(rootTag);
			doc.appendChild(root);
			for (LinkedHashMap<String, String> row : rows) {
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

	public List<LinkedHashMap<String, String>> fromXml(InputStream in, String rowTag) {
		try {
			DocumentBuilder builder = newSecureDocumentBuilder();
			Document doc = builder.parse(in);
			NodeList rowNodes = doc.getElementsByTagName(rowTag);
			List<LinkedHashMap<String, String>> rows = new ArrayList<>();
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

	public byte[] toExcel(List<String> headers, List<LinkedHashMap<String, String>> rows) {
		try (Workbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("data");
			Row headerRow = sheet.createRow(0);
			for (int c = 0; c < headers.size(); c++) {
				headerRow.createCell(c).setCellValue(headers.get(c));
			}
			int r = 1;
			for (LinkedHashMap<String, String> row : rows) {
				Row excelRow = sheet.createRow(r++);
				for (int c = 0; c < headers.size(); c++) {
					String value = row.get(headers.get(c));
					excelRow.createCell(c).setCellValue(value == null ? "" : value);
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new InvalidFileFormatException("Excel匯出失敗", e);
		}
	}

	public List<LinkedHashMap<String, String>> fromExcel(InputStream in, List<String> headers) {
		try (Workbook wb = WorkbookFactory.create(in)) {
			Sheet sheet = wb.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			List<LinkedHashMap<String, String>> rows = new ArrayList<>();
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
