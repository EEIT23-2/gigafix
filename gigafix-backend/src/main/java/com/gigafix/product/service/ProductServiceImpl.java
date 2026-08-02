package com.gigafix.product.service;

import com.gigafix.product.Utils;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.dto.ProductQueryParams;
import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class ProductServiceImpl implements ProductService   {
    @Autowired
    private ProductDao productDao;

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

    @Override
    public void deleteProductById(Long productId) {
        productDao.deleteById(productId);
    }
}

