package com.gigafix.product.controller;


import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.dto.RecycleAgreementRequest;
import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.service.RecycleApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gigafix.common.util.SecurityUtils;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;

@RestController
public class RecycleApplicationController {
    @Autowired
    private RecycleApplicationService recycleApplicationService;

    //查詢所有回收單的路由
    @GetMapping("/api/admin/recycle-applications")
    public ResponseEntity<Page<RecycleResponse>> getApplyForms(@Valid RecycleQueryParams recycleQueryParams){
        Page<RecycleResponse> resultList = recycleApplicationService.getApplyForms(recycleQueryParams);
        return ResponseEntity.status(HttpStatus.OK).body(resultList);
    }

    //將全部回收單匯出為 JSON 檔
    @GetMapping("/api/admin/recycle-applications/export")
    public ResponseEntity<byte[]> exportApplyForms() throws IOException {
        byte[] jsonBytes = recycleApplicationService.exportApplyForms();

        HttpHeaders headers = new HttpHeaders();
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=recycle-applications.json"
        );
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .body(jsonBytes);
    }

    //Id搜尋單筆回收單的路由controller
    @GetMapping("/api/admin/recycle-applications/{applyId}")
    public ResponseEntity<RecycleResponse> getApplyFormById(@PathVariable Long applyId){
        RecycleResponse response = recycleApplicationService.getApplyFormById(applyId);
        //回傳狀態 ,若找不到 回傳404並用.build()建body
        if(response !=null){
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 前台登入會員查詢自己的回收單列表
    @GetMapping("/api/gigafix/members/me/recycle-applications")
    public ResponseEntity<Page<RecycleResponse>> getMyApplyForms(
            Authentication authentication,
            @Valid RecycleQueryParams recycleQueryParams
    ) {
        Long memberId =
                SecurityUtils.getCurrentMember(authentication).getId();

        Page<RecycleResponse> result =
                recycleApplicationService.getMemberApplyForms(
                        memberId,
                        recycleQueryParams
                );

        return ResponseEntity.ok(result);
    }

    //前台登入會員查詢自己id下的單筆回收單
    @GetMapping("/api/gigafix/members/me/recycle-applications/{applyId}")
    public ResponseEntity<RecycleResponse> getMyApplyFormById(
            Authentication authentication,
            @PathVariable Long applyId
    ) {
        Long memberId =
                SecurityUtils.getCurrentMember(authentication).getId();

        RecycleResponse response =
                recycleApplicationService.getMemberApplyFormById(
                        memberId,
                        applyId
                );

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    //id新增回收單的路由
    @PostMapping("/api/gigafix/members/me/recycle-applications")      //@Valid 是為了讓@NotNull生效
    public ResponseEntity<RecycleResponse> createApplyForm(Authentication authentication, @RequestBody @Valid RecycleRequest recycleRequest) {

        Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
        RecycleResponse response = recycleApplicationService.createApplyForm(memberId,recycleRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        //修改維修單的路由
    // 後台確認手機已到店後，將單一回收單推進到現場檢測評估階段。
    @PatchMapping("/api/admin/recycle-applications/{applyId}/status/inspecting")
    public ResponseEntity<RecycleResponse> markApplyFormAsInspecting(@PathVariable Long applyId) {
        try {
            RecycleResponse response = recycleApplicationService.markAsInspecting(applyId);

            if (response == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(response);
        } catch (IllegalStateException exception) {
            // 回收單若已取消或進入後續階段，以 409 表示與目前狀態衝突。
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // 後台完成估價後觸發 OTP；驗證碼會寄到該回收單會員的信箱並保存 5 分鐘。
    @PostMapping("/api/admin/recycle-applications/{applyId}/agreement-otp")
    public ResponseEntity<Void> sendAgreementOtp(@PathVariable Long applyId) {
        try {
            boolean sent = recycleApplicationService.sendAgreementOtp(applyId);
            return sent
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // 前台會員提交 OTP 與 Canvas 簽名；存檔後直接進入資料清除中，並且只能簽署自己的回收單。
    @PostMapping("/api/gigafix/members/me/recycle-applications/{applyId}/agreement")
    public ResponseEntity<RecycleResponse> confirmAgreement(
            Authentication authentication,
            @PathVariable Long applyId,
            @RequestBody @Valid RecycleAgreementRequest request
    ) {
        try {
            Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
            RecycleResponse response = recycleApplicationService.confirmAgreement(
                    memberId,
                    applyId,
                    request
            );
            return response == null
                    ? ResponseEntity.notFound().build()
                    : ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // 工程師確認資料清除完成後結案，同時新增一筆可販售的商品庫存並通知會員。
    @PatchMapping("/api/admin/recycle-applications/{applyId}/status/completed")
    public ResponseEntity<RecycleResponse> completeRecycle(@PathVariable Long applyId) {
        try {
            RecycleResponse response = recycleApplicationService.completeRecycle(applyId);
            return response == null
                    ? ResponseEntity.notFound().build()
                    : ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            // 外觀或估價資料無法建立商品時，回傳 400 讓後台修正回收單內容。
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException exception) {
            // 只有 WIPING 狀態能結案，其他狀態以 409 表示流程衝突。
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

        @PutMapping("/api/admin/recycle-applications/{applyId}")
        public ResponseEntity<RecycleResponse> updateApplyForm(@PathVariable Long applyId,
                                                     @RequestBody @Valid RecycleRequest recycleRequest){
            RecycleResponse applyForm = recycleApplicationService.getApplyFormById(applyId);
            if (applyForm ==null){   //先檢查是否有此id再做修改
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }//若找不到 回傳404給前端

            recycleApplicationService.updateApplyForm(applyId,recycleRequest);
            RecycleResponse updatedApplyForm = recycleApplicationService.getApplyFormById(applyId);

            return ResponseEntity.status(HttpStatus.OK).body(updatedApplyForm);
        }

    //刪除一筆回收單
    @DeleteMapping("/api/admin/recycle-applications/{applyId}")
    public ResponseEntity<Void> deleteApplyForm(@PathVariable Long applyId){
        recycleApplicationService.deleteApplyFormById(applyId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //刪除所有回收單
    @DeleteMapping("/api/admin/recycle-applications")
    public ResponseEntity<Void> deleteAllApplyForms(){//因不回傳任何Product物件 以Void泛型解偶
        recycleApplicationService.deleteAllApplyForms();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

}
