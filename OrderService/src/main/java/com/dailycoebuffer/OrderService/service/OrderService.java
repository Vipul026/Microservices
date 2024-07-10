package com.dailycoebuffer.OrderService.service;

import com.dailycoebuffer.OrderService.model.OrderRequest;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);
}
