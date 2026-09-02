package com.gigafix.product.service;

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



    //實作查詢單筆回收單
    @Override
    public RecycleApplication getApplyFormById(Long applyId) {
        return recycleApplicationDao.findById(applyId).orElse(null);

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
