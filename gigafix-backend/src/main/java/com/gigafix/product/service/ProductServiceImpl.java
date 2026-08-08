package com.gigafix.product.service;

import com.gigafix.product.Utils;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.ProductSaleStatus;
import com.gigafix.product.dto.ProductQueryParams;
import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class ProductServiceImpl implements ProductService   {
    @Autowired
    private ProductDao productDao;
    @Autowired
    private ObjectMapper objectMapper;

    //實作查詢全部商品列表
    @Override
    public List<Product> getProducts(ProductQueryParams productQueryParams) {
        ProductCategory category = productQueryParams.getCategory();
        String search = Utils.blankToNull(productQueryParams.getSearch());
        String modelName = Utils.blankToNull(productQueryParams.getModelName());
        String color = Utils.blankToNull(productQueryParams.getColor());
        String storage = Utils.blankToNull(productQueryParams.getStorage());
        String orderBy = Utils.blankToNull(productQueryParams.getOrderBy());
        String sortParam = Utils.blankToNull(productQueryParams.getSort());
        Integer minPrice = productQueryParams.getMinPrice();
        Integer maxPrice = productQueryParams.getMaxPrice();

        if (orderBy == null) {
            orderBy = "createdDate"; // 預設依建立時間排序
        }
        if (sortParam == null) {
            sortParam = "desc";           // 預設降冪（從新到舊） "desc"字串到時候寫在前端
        }
        //JPA的Sort 物件判斷是.asc().desc()
        Sort sort = sortParam.equalsIgnoreCase("asc") ? //acs字串到時候寫在前端
                Sort.by(orderBy).ascending() ://昇羃
                Sort.by(orderBy).descending();//降冪

        return productDao.findByConditions(category,search,modelName,color,storage,minPrice,maxPrice,sort);
    }

    //實作以id查詢商品
    @Override
    public Product getProductById(Long productId) {
        return productDao.findById(productId).orElse(null);
    }

    //實作新增商品
    @Override
    public Long createProduct(ProductRequest productRequest) {
        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setCategory(productRequest.getCategory());
        product.setImageUrl(productRequest.getImageUrl());
        product.setDescription(productRequest.getDescription());
        product.setAppearance(productRequest.getAppearance());
        product.setGrade(productRequest.getGrade());
        product.setPrice(productRequest.getPrice());
        product.setSaleStatus(productRequest.getSaleStatus());


        product.setCreatedDate(LocalDateTime.now());
        product.setLastModifiedDate(LocalDateTime.now());

        Product savedProduct  = productDao.save(product);
        return savedProduct.getProductId();


    }
    //實作修改商品
    @Override
    public void updateProduct(Long productId, ProductRequest productRequest) {
        Optional<Product> product = productDao.findById(productId);
        //檢查是否有該商品 後再做修改
        if(product.isPresent()){
            Product gotProduct = product.get();
            gotProduct.setProductName(productRequest.getProductName());
            gotProduct.setCategory(productRequest.getCategory());
            gotProduct.setImageUrl(productRequest.getImageUrl());
            gotProduct.setDescription(productRequest.getDescription());
            gotProduct.setAppearance(productRequest.getAppearance());
            gotProduct.setGrade(productRequest.getGrade());
            gotProduct.setPrice(productRequest.getPrice());
            gotProduct.setSaleStatus(productRequest.getSaleStatus());

            gotProduct.setLastModifiedDate(LocalDateTime.now());

            Product updatedProduct = productDao.save(gotProduct);
        }else{
            return; //若有商品直接返回
        }
    }


    //實作刪除單筆商品
    @Override
    public void deleteProductById(Long productId) {
        productDao.deleteById(productId);

    }
    //實作所有商品
    @Override
    public void deleteAllProducts() {
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
                product.setCreatedDate(LocalDateTime.now());
                product.setLastModifiedDate(LocalDateTime.now());
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

