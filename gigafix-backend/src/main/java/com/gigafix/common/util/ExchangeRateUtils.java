package com.gigafix.common.util;

import com.gigafix.product.dto.ExchangeRateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ExchangeRateUtils {//此類別是jack用來展示串第三方API商品管理更換幣別用
    @Autowired
    private RestTemplate restTemplate;

    private final String ApiKey = "faf7f9c9db7f09d42c7ddd25";
    private final String BaseUrl = "https://exchangerate-api.com";

    public Map<String,Double> getLatestRatesFromTWD(){
        //字串組合以請求外部網址
        String url = BaseUrl + ApiKey + "/latest/TWD";
        //發送get請求外部API
        try {
            ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);
            if(response !=null && "success".equals(response.getResult())){
                return response.getConversionRate();//若回應不是空值且取得結果為'成功'就回傳轉換幣別之匯率
            }
        } catch (RestClientException e) {
            System.out.println("呼叫此匯率失敗囉!why?"+e.getMessage());
        }return null;


    }
}
