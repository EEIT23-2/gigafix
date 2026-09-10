package com.gigafix.product.service;

import com.gigafix.member.entity.Member;
import com.gigafix.member.exception.MemberNotFoundException;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.product.Utils;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.RecycleStatus;
import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.repository.RecycleApplicationDao;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.StoresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    //實作查詢回收單列表

    @Override
    public Page<RecycleResponse> getApplyForms(RecycleQueryParams recycleQueryParams) {
        // 後台可以使用查詢參數中的 memberId
        return queryApplyForms(
                recycleQueryParams.getMemberId(),
                recycleQueryParams
        );
    }
    //實作以會員id查詢回收單列表
    @Override
    public Page<RecycleResponse> getMemberApplyForms(Long memberId, RecycleQueryParams recycleQueryParams) {
        //前台強制使用登入會員的Id
        return queryApplyForms(memberId, recycleQueryParams);
    }

    private Page<RecycleResponse> queryApplyForms(Long memberId, RecycleQueryParams recycleQueryParams) {
        String productName = Utils.blankToNull(recycleQueryParams.getProductName());
        String appeareance = Utils.blankToNull(recycleQueryParams.getAppearance());
        String orderBy = Utils.blankToNull(recycleQueryParams.getOrderBy());
        String sortParam = Utils.blankToNull(recycleQueryParams.getSort());
        ProductCategory category = recycleQueryParams.getProductCategory();
        RecycleStatus recycleStatus = recycleQueryParams.getRecycleStatus();
        Integer limit = recycleQueryParams.getLimit();
        Integer offset = recycleQueryParams.getOffset();

        if (limit == null) {
            limit = 20;
        }
        if(offset == null){
            offset = 0;
        }

        if(orderBy == null){
           orderBy = "createdTime"; // 預設依建立時間排序
        }
        if(sortParam == null){
           sortParam = "desc"; // 預設降冪（從新到舊） "desc"字串到時候寫在前端
        }

        //JPA的Sort 物件判斷是.asc().desc()
        Sort sort = sortParam.equalsIgnoreCase("asc") ? //acs字串到時候寫在前端
                Sort.by(orderBy).ascending() ://昇羃
                Sort.by(orderBy).descending();//降冪
        //轉化為jpa頁數
        int page = offset / limit;
        //結合為Pageable物件  參數為 頁數 ,pagesize, 排序
        Pageable pageable = PageRequest.of(page,limit,sort);
        Page<RecycleApplication> applyFormPage  = recycleApplicationDao.findByConditions(memberId, productName, appeareance, category, recycleStatus, pageable);
        //利用 .map() 把裡面的每一筆 Entity 轉成 DTO，這時型態會自動變成 Page<RecycleResponse>
        Page<RecycleResponse> applyFormList = applyFormPage.map(this::toResponse);

        return applyFormList;
    }

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
        response.setAppearance(applyForm.getAppearance());
        response.setImageUrl(applyForm.getImageUrl());
        response.setDescription(applyForm.getDescription());
        response.setEstimatedPrice(applyForm.getEstimatedPrice());
        response.setRecycleStatus(applyForm.getRecycleStatus());
        response.setCreatedTime(applyForm.getCreatedTime());
        response.setLastModifiedTime(applyForm.getLastModifiedTime());

        if(applyForm.getMember() !=null){
            response.setMemberId(applyForm.getMember().getId());
            response.setMemberName(applyForm.getMember().getRealName());
            response.setContactPhone(applyForm.getMember().getPhone());
        }

        if(applyForm.getStores() != null){
            response.setStoreId(applyForm.getStores().getId());
            response.setStoreName(applyForm.getStores().getName());
        }

        return response;
    }

    //實作前台以會員id查詢回收單列表

    @Override
    public RecycleResponse getMemberApplyFormById(Long memberId, Long applyId) {
        RecycleApplication applyForm =
                recycleApplicationDao
                        .findByApplyIdAndMember_Id(applyId, memberId)
                        .orElse(null);

        if (applyForm == null) {
            return null;
        }
        return toResponse(applyForm);
    }

    //實作新增回收單
    @Override
    public RecycleResponse createApplyForm(Long memberId, RecycleRequest recycleRequest) {
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

        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

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

    //實作修改回收單


    @Override
    public void updateApplyForm(Long applyId, RecycleRequest recycleRequest) {
        Optional<RecycleApplication> applyForm = recycleApplicationDao.findById(applyId);
        if(applyForm.isPresent()){
            RecycleApplication gotApplyForm = applyForm.get();
            gotApplyForm.setProductName(recycleRequest.getProductName());
            gotApplyForm.setCategory(recycleRequest.getCategory());
            gotApplyForm.setAppearance(recycleRequest.getAppearance());
            gotApplyForm.setImageUrl(recycleRequest.getImageUrl());
            gotApplyForm.setDescription(recycleRequest.getDescription());
            gotApplyForm.setEstimatedPrice(recycleRequest.getEstimatedPrice());

            gotApplyForm.setLastModifiedTime(LocalDateTime.now());
            RecycleApplication updatedApplyForm = recycleApplicationDao.save(gotApplyForm);
        }else{
            return;
        }

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
