package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateOrderStatusRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.ForbiddenOrderActionException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.EventOrderCreationResult;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final String internalServiceToken;

    public OrderController(
            OrderService orderService,
            @Value("${app.internal-service-token:local-dev-internal-token}") String internalServiceToken
    ) {
        this.orderService = orderService;
        this.internalServiceToken = internalServiceToken;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateOrderRequest request
    ) {
        requireActor(userId, request.sellerId(), "Only the seller can create this order");
        BidmartOrder order = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.getId())
                .toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId
    ) {
        BidmartOrder order = orderService.getOrder(orderId);
        if (!userId.equals(order.getSellerId()) && !userId.equals(order.getBuyerId())) {
            throw new ForbiddenOrderActionException("Only seller or buyer can view this order");
        }
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        BidmartOrder order = orderService.getOrder(orderId);
        requireActor(userId, order.getSellerId(), "Only the seller can update order shipping status");
        return ResponseEntity.ok(OrderResponse.from(orderService.updateShipping(orderId, request)));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmReceipt(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId
    ) {
        BidmartOrder order = orderService.getOrder(orderId);
        requireActor(userId, order.getBuyerId(), "Only the buyer can confirm receipt");
        return ResponseEntity.ok(OrderResponse.from(orderService.confirmReceipt(orderId)));
    }

    @PostMapping("/events/auction-won")
    public ResponseEntity<OrderResponse> createFromAuctionWon(
            @RequestHeader("X-Internal-Service-Token") String token,
            @RequestBody AuctionWonEventRequest request
    ) {
        if (!internalServiceToken.equals(token)) {
            throw new ForbiddenOrderActionException("Invalid internal service token");
        }
        EventOrderCreationResult result = orderService.createOrderFromAuctionWon(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OrderResponse.from(result.order()));
    }

    private void requireActor(String actualUserId, String requiredUserId, String message) {
        if (!requiredUserId.equals(actualUserId)) {
            throw new ForbiddenOrderActionException(message);
        }
    }
}
