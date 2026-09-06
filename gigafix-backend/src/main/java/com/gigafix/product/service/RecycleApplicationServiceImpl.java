package com.gigafix.product.service;

import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.repository.RecycleApplicationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class RecycleApplicationServiceImpl implements RecycleApplicationService{
    @Autowired
    private RecycleApplicationDao recycleApplicationDao;



    //實作查詢單筆回收單id
    @Override
    public RecycleResponse getApplyFormById(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao.findById(applyId).orElse(null);

        if(applyForm == null){
            return null;
        }

        return toResponse(applyForm);

    }
    //用作將entity資訊填入RecycleResponse DTO類別 給查詢回收單id用
    private RecycleResponse toResponse(RecycleApplication applyForm){
        RecycleResponse response = new RecycleResponse();

        response.setApplyId(applyForm.getApplyId());
        response.setProductName(applyForm.getProductName());
        response.setCategory(applyForm.getCategory());
        response.setAppreance(applyForm.getAppearance());
        response.setImageUrl(applyForm.getImageUrl());
        response.setDescription(applyForm.getDescription());
        response.setEstimatedPrice(applyForm.getEstimatedPrice());
        response.setRecycleStatus(applyForm.getRecycleStatus());
        response.setCreatedTime(applyForm.getCreatedTime());
        response.setLastModifiedTime(applyForm.getLastModifiedTime());

        if(applyForm.getMember() !=null){
            response.setMemberId(applyForm.getMember().getId());
            response.setMemberName(applyForm.getMember().getRealName());
        }

        if(applyForm.getStores() != null){
            response.setStoreId(applyForm.getStores().getId());
            response.setStoreName(applyForm.getStores().getName());
        }

        return response;
    }

    //實作刪除一筆回收單
    @Override
    public void deleteApplyFormById(Long applyId) {
        recycleApplicationDao.deleteById(applyId);

    }
    //實作刪除所有回收單
    @Override
    public void deleteAllApplyForms() {
        recycleApplicationDao.deleteAll();
    }
}
