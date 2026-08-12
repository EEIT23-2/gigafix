package com.gigafix.product.dto;

import java.util.Map;

public class ExchangeRateResponse {

    private String result;
    private String originCurrency;
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
