package com.gigafix.product.constant;

public enum RecycleStatus {
    // 0: 已取消 (客戶預約後七天未到店，或主動取消預約)
    CANCELLED(0, "已取消"),

    // 1: 已預約 (線上填寫完手機狀況並預約到店交件)
    APPLIED(1, "已預約交件"),

    // 2: 檢測評估中 (客戶已將手機帶至門市，門市人員現場檢測並填入預估價)
    INSPECTING(2, "現場檢測評估中"),

    // 3: 待簽署同意 (客戶確認並準備簽署同意書 這邊可以發email通知)
    WAITING_FOR_AGREEMENT(3, "待簽署同意"),

    // 4: 資料清除中 (簽署完成後，現場或後台進行手機原廠重置與隱私清除)
    WIPING(4, "資料清除中"),

    // 5: 完成回收 (回收單結案)
    COMPLETED(5, "完成回收");

    private final int code;
    private final String description;

    RecycleStatus(int code,String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
