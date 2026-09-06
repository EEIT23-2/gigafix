package com.gigafix.product.service;

import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.product.constant.RecycleStatus;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.repository.RecycleApplicationDao;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.StoresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Transactional
@Service
public class RecycleApplicationServiceImpl implements RecycleApplicationService{
    @Autowired
    private RecycleApplicationDao recycleApplicationDao;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private StoresRepository storesRepository;


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

    @Override
    public RecycleResponse createApplyForm(RecycleRequest recycleRequest) {
        RecycleApplication applyForm = new RecycleApplication();

        applyForm.setProductName(recycleRequest.getProductName());
        applyForm.setCategory(recycleRequest.getCategory());
        applyForm.setAppearance(recycleRequest.getAppearance());
        applyForm.setImageUrl(recycleRequest.getImageUrl());
        applyForm.setDescription(recycleRequest.getDescription());
        applyForm.setEstimatedPrice(recycleRequest.getEstimatedPrice());

        applyForm.setRecycleStatus(RecycleStatus.APPLIED);
        applyForm.setCreatedTime(LocalDateTime.now());
        applyForm.setLastModifiedTime(LocalDateTime.now());

        Member member = memberRepository.findById(recycleRequest.getMemberId()).orElse(null);

        if(member == null){
            return null;
        }
        applyForm.setMember(member);

        if(recycleRequest.getStoreId()!=null){
            Stores stores = storesRepository.findById(recycleRequest.getStoreId()).orElse(null);
            if(stores == null){
                return null;
            }

            applyForm.setStores(stores);
        }

        RecycleApplication saveAppyForm = recycleApplicationDao.save(applyForm);
        return toResponse(saveAppyForm);
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
