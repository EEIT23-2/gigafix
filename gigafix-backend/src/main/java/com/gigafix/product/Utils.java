package com.gigafix.product;



public class Utils {


    //靜態方法 小工具用來應用在條件查詢實作 三元運算檢查 params非空值&.trim()去除前後空白字元 !isEmpty()字串長度不是0
    public static String blankToNull(String params){
        return (params != null && !params.trim().isEmpty()) ? params : null;
    }


}
