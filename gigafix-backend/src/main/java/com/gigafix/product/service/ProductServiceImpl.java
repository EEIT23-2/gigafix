package com.gigafix.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gigafix.common.util.ExchangeRateUtils;
import com.gigafix.product.Utils;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.ProductSaleStatus;
import com.gigafix.product.dto.ProductQueryParams;
import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.repository.ProductDao;
import com.gigafix.product.repository.RecycleApplicationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Transactional
@Service
public class ProductServiceImpl implements ProductService   {
    private static final DateTimeFormatter EXCEL_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private ProductDao productDao;
    @Autowired
    private RecycleApplicationDao recycleApplicationDao;
    @Autowired //注入jackson 反序列化需要的介面
    private ObjectMapper objectMapper;
    @Autowired //注入改匯率工具
    private ExchangeRateUtils exchangeRateUtils;
    @Autowired
    private Cloudinary cloudinary;

    //實作查詢全部商品列表
    @Override
    public Page<Product> getProducts(ProductQueryParams productQueryParams) {
        ProductCategory category = productQueryParams.getCategory();
        ProductSaleStatus saleStatus = productQueryParams.getSaleStatus();
        String search = Utils.blankToNull(productQueryParams.getSearch());
        String modelName = Utils.blankToNull(productQueryParams.getModelName());
        String color = Utils.blankToNull(productQueryParams.getColor());
        String storage = Utils.blankToNull(productQueryParams.getStorage());
        String orderBy = Utils.blankToNull(productQueryParams.getOrderBy());
        String sortParam = Utils.blankToNull(productQueryParams.getSort());
        Integer minPrice = productQueryParams.getMinPrice();
        Integer maxPrice = productQueryParams.getMaxPrice();
        Long recycleApplyId = productQueryParams.getRecycleApplyId();
        Integer limit = productQueryParams.getLimit();
        Integer offset = productQueryParams.getOffset();

        if (limit == null) {
            limit = 48; //給予分頁預設值 設定一頁48筆
        }
        if (offset == null) {
            offset = 0;
        }

        if (orderBy == null) {
            orderBy = "createdTime"; // 預設依建立時間排序
        }
        if (sortParam == null) {
            sortParam = "desc";           // 預設降冪（從新到舊） "desc"字串到時候寫在前端
        }
        //JPA的Sort 物件判斷是.asc().desc()
        Sort sort = sortParam.equalsIgnoreCase("asc") ? //acs字串到時候寫在前端
                Sort.by(orderBy).ascending() ://昇羃
                Sort.by(orderBy).descending();//降冪

        //轉化為jpa頁數
        int page = offset / limit;
        //結合為Pageable物件  參數為 頁數 ,pagesize, 排序
        Pageable pageable = PageRequest.of(page,limit,sort);
        //封裝商品列表
        Page<Product> productList = productDao.findByConditions(
                category, saleStatus, search, modelName, color, storage,
                recycleApplyId, minPrice, maxPrice, pageable
        );

        // 批次取得本頁商品的來源回收單，避免逐筆查詢造成 N+1 問題。
        List<Long> productIds = new ArrayList<>();
        for (Product product : productList.getContent()) {
            productIds.add(product.getProductId());
        }

        Map<Long, Long> recycleApplyIds = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<RecycleApplication> sourceApplications =
                    recycleApplicationDao.findAllByProductIdIn(productIds);
            for (RecycleApplication application : sourceApplications) {
                recycleApplyIds.put(
                        application.getProduct().getProductId(),
                        application.getApplyId()
                );
            }
        }
        //呼叫api獲取最新匯率
        Map<String, Double> rates = exchangeRateUtils.getLatestRatesFromTWD();
        //以下兩段為防段往機制  三元運算設定預設安全匯率
        final double usdRate = (rates != null) ? rates.getOrDefault("USD", 0.031) : 0.031;
        final double jpyRate = (rates != null) ? rates.getOrDefault("JPY", 4.65) : 4.65;
        // 直接更新本頁 Entity 的 transient 顯示欄位，避免使用 lambda 轉換整個 Page。
        for (Product product : productList.getContent()) {
            if (product.getPrice() != null) {
                product.setPriceUSD(product.getPrice() * usdRate);
                product.setPriceJPY(product.getPrice() * jpyRate);
            }
            product.setRecycleApplyId(recycleApplyIds.get(product.getProductId()));
        }
        return productList;
    }

    //實作以id查詢商品
    @Override
    public Product getProductById(Long productId) {
        Product product = productDao.findById(productId).orElse(null);
        if (product == null) {
            return null;
        }

        // recycleApplyId 是 API 顯示欄位，來源仍以回收單表的關聯為準。
        Optional<RecycleApplication> sourceApplication =
                recycleApplicationDao.findByProduct_ProductId(productId);
        if (sourceApplication.isPresent()) {
            product.setRecycleApplyId(sourceApplication.get().getApplyId());
        }
        return product;
    }

    //實作新增商品
    @Override
    public Long createProduct(ProductRequest productRequest, MultipartFile imageFile) {
        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setCategory(productRequest.getCategory());
        // 圖片由後端上傳至 Cloudinary，前端不再直接提供可寫入資料庫的網址。
        product.setImageUrl(uploadImage(imageFile, "gigafix/products"));
        product.setDescription(productRequest.getDescription());
        product.setAppearance(productRequest.getAppearance());
        product.setGrade(productRequest.getGrade());
        product.setPrice(productRequest.getPrice());
        product.setSaleStatus(productRequest.getSaleStatus());


        product.setCreatedTime(LocalDateTime.now());
        product.setLastModifiedTime(LocalDateTime.now());

        Product savedProduct  = productDao.save(product);
        return savedProduct.getProductId();


    }
    //實作修改商品
    @Override
    public void updateProduct(Long productId, ProductRequest productRequest, MultipartFile imageFile) {
        Optional<Product> product = productDao.findById(productId);
        //檢查是否有該商品 後再做修改
        if(product.isPresent()){
            Product gotProduct = product.get();
            gotProduct.setProductName(productRequest.getProductName());
            gotProduct.setCategory(productRequest.getCategory());
            // 沒有選擇新檔案時保留原圖；有新檔案才以 Cloudinary 網址取代。
            if (imageFile != null && !imageFile.isEmpty()) {
                gotProduct.setImageUrl(uploadImage(imageFile, "gigafix/products"));
            }
            gotProduct.setDescription(productRequest.getDescription());
            gotProduct.setAppearance(productRequest.getAppearance());
            gotProduct.setGrade(productRequest.getGrade());
            gotProduct.setPrice(productRequest.getPrice());
            gotProduct.setSaleStatus(productRequest.getSaleStatus());

            gotProduct.setLastModifiedTime(LocalDateTime.now());

        }else{
            return; //若有商品直接返回
        }
    }

    /** 上傳圖片並只保存 HTTPS 網址，CLOUDINARY_URL 由共用 Cloudinary Bean 自動讀取。 */
    private String uploadImage(MultipartFile imageFile, String folder) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        validateCloudinaryConfiguration();
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    imageFile.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", "image")
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException exception) {
            // Cloudinary 連線或驗證失敗時回傳明確的閘道錯誤，不再讓例外變成不明原因的 500。
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary 圖片上傳失敗，請確認 CLOUDINARY_URL 與網路連線",
                    exception
            );
        }
    }

    /** 在呼叫遠端 API 前先檢查必要憑證，避免 SDK 以 IllegalArgumentException 回傳 HTTP 500。 */
    private void validateCloudinaryConfiguration() {
        if (cloudinary.config.cloudName == null
                || cloudinary.config.apiKey == null
                || cloudinary.config.apiSecret == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "後端執行環境未設定有效的 CLOUDINARY_URL"
            );
        }
    }


    //實作刪除單筆商品
    @Override
    public void deleteProductById(Long productId) {
        recycleApplicationDao.clearProductReference(productId);
        productDao.deleteById(productId);

    }
    //實作刪除所有商品
    @Override
    public void deleteAllProducts() {
        recycleApplicationDao.clearAllProductReferences();
        productDao.deleteAll();
    }

    //匯入商品表JSON實作

    @Override
    public int importProducts() throws IOException { //外拋檔案不存在等錯誤
        ClassPathResource resource = new ClassPathResource("all2handsProduct.json");
        try (InputStream inputStream = resource.getInputStream()) {//try-with-resource關資源
            List<Product> productList = objectMapper.readValue(inputStream, new TypeReference<List<Product>>() {
            });

            for (Product product : productList) {
                product.setCreatedTime(LocalDateTime.now());
                product.setLastModifiedTime(LocalDateTime.now());
            }

            productDao.saveAll(productList);
            return productList.size();
        }
    }

    //匯出json檔實作;

    @Override
    public byte[] exportProducts() throws IOException {
        List<Product> products = productDao.findAll();//取得所有商品資訊

        //將物件列表轉換為漂亮的 JSON 字串，並轉成 byte 陣列
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(products);
    }

    // 將全部商品轉成 Office Open XML 格式，讓前端可以直接下載 .xlsx 檔。
    @Override
    public byte[] exportProductsExcel() throws IOException {
        List<Product> products = productDao.findAll(Sort.by("createdTime").descending());
        String[] headers = {
                "商品 ID", "商品名稱", "類別", "圖片網址", "商品描述", "外觀狀況",
                "等級", "價格", "販售狀態", "建立時間", "最後修改時間"
        };
        int[] columnWidths = {12, 24, 14, 45, 40, 30, 12, 14, 16, 22, 22};

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("商品資料");
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, columnWidths[index] * 256);
            }

            for (int index = 0; index < products.size(); index++) {
                Product product = products.get(index);
                Row row = sheet.createRow(index + 1);
                setNumberCell(row, 0, product.getProductId());
                setTextCell(row, 1, product.getProductName());
                setTextCell(row, 2, product.getCategory() == null ? null : product.getCategory().name());
                setTextCell(row, 3, product.getImageUrl());
                setTextCell(row, 4, product.getDescription());
                setTextCell(row, 5, product.getAppearance());
                setTextCell(row, 6, product.getGrade());
                setNumberCell(row, 7, product.getPrice());
                setTextCell(row, 8, product.getSaleStatus() == null ? null : product.getSaleStatus().name());
                setTextCell(row, 9, formatExcelDateTime(product.getCreatedTime()));
                setTextCell(row, 10, formatExcelDateTime(product.getLastModifiedTime()));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // Excel 標題列使用粗體與底色，方便使用者閱讀欄位。
    private CellStyle createExcelHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setTextCell(Row row, int columnIndex, String value) {
        row.createCell(columnIndex).setCellValue(value == null ? "" : value);
    }

    private void setNumberCell(Row row, int columnIndex, Number value) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private String formatExcelDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXCEL_DATE_TIME_FORMAT);
    }


//以下商業邏輯實作是給 訂單人員呼叫用的



    //鎖定商品（購買尚未付款時,由訂單人員呼叫）
    @Override
    public void reserveProduct(Long productId) {
        //用 Optional 泛型容器接住結果
        Optional<Product> product = productDao.findById(productId);

        if (product.isPresent() == false) {
            throw new IllegalArgumentException("無此商品,編號: " + productId);
        }

        // 確定有商品後 用 get()解藕取出
        Product reserveProduct = product.get();

        // 防呆：只有在「可販售」狀態下，才能被保留鎖定
        if (reserveProduct.getSaleStatus() != ProductSaleStatus.AVAILABLE) {
            throw new IllegalStateException("商品目前無法被保留（可能已被售出或下架）");
        }

        reserveProduct.setSaleStatus(ProductSaleStatus.RESERVED);
        productDao.save(reserveProduct);

    }
    //釋放商品鎖定（取消訂單時,由訂單人員呼叫）
    @Override
    public void releaseProduct(Long productId) {
        Optional<Product> product = productDao.findById(productId);

        if (product.isPresent() == false) {
            throw new IllegalArgumentException("無此商品,編號: " + productId);
        }

        Product releaseProduct = product.get();

        // 防呆 只有在reserved(保留)的狀態下 才能釋放回avaliable(可販售)
        if (releaseProduct.getSaleStatus() == ProductSaleStatus.RESERVED) {
            releaseProduct.setSaleStatus(ProductSaleStatus.AVAILABLE);
            productDao.save(releaseProduct);
        }
    }
    //確認售出（當付款成功時,由訂單人員呼叫）
    @Override
    public void sellProduct(Long productId) {
        Optional<Product> product = productDao.findById(productId);

        if (product.isPresent() == false) {
            throw new IllegalArgumentException("無此商品,編號: " + productId);
        }

        Product sellProduct = product.get();

        // 防呆 如果早就是sold(售出) 狀態
        if (sellProduct.getSaleStatus() == ProductSaleStatus.SOLD) {
            throw new IllegalStateException("商品已售出,無法重複購買");
        }

        sellProduct.setSaleStatus(ProductSaleStatus.SOLD);
        productDao.save(sellProduct);


    }



}
