package com.dailycoebuffer.OrderService.service;

import com.dailycoebuffer.OrderService.entity.Order;
import com.dailycoebuffer.OrderService.exception.CustomException;
import com.dailycoebuffer.OrderService.external.client.PaymentService;
import com.dailycoebuffer.OrderService.external.client.ProductService;
import com.dailycoebuffer.OrderService.external.request.PaymentRequest;
import com.dailycoebuffer.OrderService.model.OrderRequest;
import com.dailycoebuffer.OrderService.model.OrderResponse;
import com.dailycoebuffer.OrderService.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public long placeOrder(OrderRequest orderRequest) {

        //Order Entity -> Save the data with status order created.
        //Product Service -> Block products (Reduce the quantity)
        //Payment Service -> Payments -> Success -> Complete, else Cancelled

        log.info("Placing Order Request: {}", orderRequest);

        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Creating Order with Status CREATED");

        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();

        order = orderRepository.save(order);

        log.info("Calling Payment Service to complete the payment");

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();

        String orderStatus = null;
        try{
            paymentService.doPayment(paymentRequest);
            log.info("Payment done Successfully. Changing the order status to Placed");
            orderStatus = "PLACED";
        }catch (Exception e){
            log.error("Error occurred in payment. Changing the order status to failed");
            orderStatus = "FAILED";
        }

        order.setOrderStatus(orderStatus);

        orderRepository.save(order);

        log.info("Order Place Successfully with Order Id: {}", order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Get Order Details for Order Id: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order Not Found of Order Id: "+ orderId,
                        "NOT_FOUND",
                        404));

        log.info("Invoking Product Service to fetch the product for id {}", order.getProductId());
        OrderResponse.ProductDetails productDetails = restTemplate.getForObject("http://PRODUCT-SERVICE/product/" + order.getProductId(),
                OrderResponse.ProductDetails.class);

//        log.info("Getting payment information from the payment service");
//        OrderResponse.PaymentDetails paymentDetails = restTemplate.getForObject("http://PAYMENTSERVICE/payment/order/"+ order.getId(),
//                OrderResponse.PaymentDetails.class);

        OrderResponse.ProductDetails productResponse = OrderResponse.ProductDetails.builder()
                .productName(productDetails.getProductName())
                .productId(productDetails.getProductId())
                .build();

//        OrderResponse.PaymentDetails paymentResponse = OrderResponse.PaymentDetails.builder()
//                .paymentId(paymentDetails.getPaymentId())
//                .paymentStatus(paymentDetails.getPaymentStatus())
//                .paymentDate(paymentDetails.getPaymentDate())
//                .paymentMode(paymentDetails.getPaymentMode())
//                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(order.getId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .productDetails(productResponse)
                .build();


        return orderResponse;
    }
}
