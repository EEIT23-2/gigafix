package com.gigafix.product.service;


import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.RecycleApplication;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RecycleApplicationService {

    //查詢回收單列表
    Page<RecycleResponse> getApplyForms(RecycleQueryParams recycleQueryParams);

    //以id查詢單筆回收單
    RecycleResponse getApplyFormById(Long applyId);

    //新增回收單
    RecycleResponse createApplyForm(RecycleRequest recycleRequest);

    //修改回收單
    void updateApplyForm(Long applyId,RecycleRequest recycleRequest);
    //刪除單筆回收單
    void deleteApplyFormById(Long applyId);

    //刪除所有回收單
    void deleteAllApplyForms();
}
