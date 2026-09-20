package com.gigafix.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminOrderStatisticsResponse {

    private long totalOrders;
    private long completedOrders;
    private long pendingShipmentOrders;
    private long totalRevenue;
    private long pendingOrders;
    private long cancelledOrders;
}
