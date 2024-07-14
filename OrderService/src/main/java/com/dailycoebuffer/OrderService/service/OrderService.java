package com.dailycoebuffer.OrderService.service;

import com.dailycoebuffer.OrderService.model.OrderRequest;
import com.dailycoebuffer.OrderService.model.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}
