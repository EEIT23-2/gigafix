package com.gigafix.product.controller;


import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.service.RecycleApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RecycleApplicationController {
    @Autowired
    private RecycleApplicationService recycleApplicationService;

    //Id搜尋單筆回收單的路由controller
    @GetMapping("/recycle-applications/{applyId}")
    public ResponseEntity<RecycleApplication> getApplyFormById(@PathVariable Long applyId){
        RecycleApplication recycleApplication = recycleApplicationService.getApplyFormById(applyId);
        //回傳狀態 ,若找不到 回傳404並用.build()建body
        if(recycleApplication !=null){
            return ResponseEntity.status(HttpStatus.OK).body(recycleApplication);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    //刪除一筆回收單
    @DeleteMapping("/recycle-applications/{applyId}")
    public ResponseEntity<Void> deleteApplyForm(@PathVariable Long applyId){
        recycleApplicationService.deleteApplyFormById(applyId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //刪除所有回收單
    @DeleteMapping("/products")
    public ResponseEntity<Void> deleteAllApplyForms(){//因不回傳任何Product物件 以Void泛型解偶
        recycleApplicationService.deleteAllApplyForms();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

}
