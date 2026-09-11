package com.gigafix.product.service;


import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleAgreementRequest;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.RecycleApplication;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.util.List;

public interface RecycleApplicationService {

    //查詢回收單列表 並且也可依memberId等條件篩選
    Page<RecycleResponse> getApplyForms(RecycleQueryParams recycleQueryParams);

    //以id查詢單筆回收單
    RecycleResponse getApplyFormById(Long applyId);

    //前台會員只查自己的回收單列表
    Page<RecycleResponse> getMemberApplyForms(
            Long memberId,
            RecycleQueryParams recycleQueryParams
    );

    //前台會員查詢自己的單筆回收單
    RecycleResponse getMemberApplyFormById(
            Long memberId,
            Long applyId
    );

    //新增回收單
    RecycleResponse createApplyForm( Long memberId, RecycleRequest recycleRequest);
    //修改狀態api 回收單手機檢測中
    /**
     * 將已預約交件的回收單推進到現場檢測階段。
     * 找不到資料時回傳 null，狀態不允許轉換時拋出 IllegalStateException。
     */
    RecycleResponse markAsInspecting(Long applyId);

    /** 產生 5 分鐘有效的 OTP 並寄到回收單所屬會員信箱。 */
    boolean sendAgreementOtp(Long applyId);

    /** 驗證 OTP 與電子簽名，成功後將狀態更新為待簽署同意。 */
    RecycleResponse confirmAgreement(Long applyId, RecycleAgreementRequest request);

    //修改回收單
    void updateApplyForm(Long applyId,RecycleRequest recycleRequest);
    //刪除單筆回收單
    void deleteApplyFormById(Long applyId);

    //刪除所有回收單
    void deleteAllApplyForms();

    //匯出全部回收單 JSON
    byte[] exportApplyForms() throws IOException;
}
