package com.gigafix.product.controller;


import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.service.RecycleApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecycleApplicationController {
    @Autowired
    private RecycleApplicationService recycleApplicationService;

    //查詢所有回收單的路由
    @GetMapping("/recycle-applications")
    public ResponseEntity<Page<RecycleResponse>> getApplyForms(@Valid RecycleQueryParams recycleQueryParams){
        Page<RecycleResponse> resultList = recycleApplicationService.getApplyForms(recycleQueryParams);
        return ResponseEntity.status(HttpStatus.OK).body(resultList);
    }

    //Id搜尋單筆回收單的路由controller
    @GetMapping("/recycle-applications/{applyId}")
    public ResponseEntity<RecycleResponse> getApplyFormById(@PathVariable Long applyId){
        RecycleResponse response = recycleApplicationService.getApplyFormById(applyId);
        //回傳狀態 ,若找不到 回傳404並用.build()建body
        if(response !=null){
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    //id新增回收單的路由
    @PostMapping("/recycle-applications")      //@Valid 是為了讓@NotNull生效
    public ResponseEntity<RecycleResponse> createApplyForm(@RequestBody @Valid RecycleRequest recycleRequest) {
        RecycleResponse response = recycleApplicationService.createApplyForm(recycleRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        //修改維修單的路由
        @PutMapping("/recycle-applications/{applyId}")
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
    @DeleteMapping("/recycle-applications/{applyId}")
    public ResponseEntity<Void> deleteApplyForm(@PathVariable Long applyId){
        recycleApplicationService.deleteApplyFormById(applyId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //刪除所有回收單
    @DeleteMapping("/recycle-applications")
    public ResponseEntity<Void> deleteAllApplyForms(){//因不回傳任何Product物件 以Void泛型解偶
        recycleApplicationService.deleteAllApplyForms();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

}
