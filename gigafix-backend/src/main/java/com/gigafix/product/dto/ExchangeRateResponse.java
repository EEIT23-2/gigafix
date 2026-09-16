package com.gigafix.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// 第三方 API 還會回傳更新時間與文件網址等欄位，只接住目前換算需要的內容。
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRateResponse {

    private String result;

    @JsonProperty("base_code")
    private String originCurrency;

    @JsonProperty("conversion_rates")
    private Map<String,Double> conversionRate;  //接住幣別&匯率對應關係 key是幣別 value是匯率

    public Map<String, Double> getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Map<String, Double> conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getOriginCurrency() {
        return originCurrency;
    }

    public void setOriginCurrency(String originCurrency) {
        this.originCurrency = originCurrency;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
