package com.gigafix.product.service;

import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;

public interface RecycleApplicationService {

    //以id查詢單筆回收單
    RecycleApplication getApplyFormById(Long applyId);

    //刪除單筆回收單
    void deleteApplyFormById(Long applyId);

    //刪除所有回收單
    void deleteAllApplyForms();
}
