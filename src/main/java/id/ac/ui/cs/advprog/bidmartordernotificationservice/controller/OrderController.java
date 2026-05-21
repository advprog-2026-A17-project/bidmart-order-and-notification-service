package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.OpenDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateOrderStatusRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.ForbiddenOrderActionException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.EventOrderCreationResult;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.OrderAccessPolicy;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderAccessPolicy orderAccessPolicy;
    private final String internalServiceToken;

    public OrderController(
            OrderService orderService,
            OrderAccessPolicy orderAccessPolicy,
            @Value("${app.internal-service-token}") String internalServiceToken
    ) {
        this.orderService = orderService;
        this.orderAccessPolicy = orderAccessPolicy;
        this.internalServiceToken = internalServiceToken;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestHeader("X-User-Id") String userId
    ) {
        List<OrderResponse> orders = orderService.listOrdersForUser(userId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateOrderRequest request
    ) {
        orderAccessPolicy.requireSeller(userId, request.sellerId());
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
        orderAccessPolicy.requireParticipant(order, userId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        BidmartOrder order = orderService.getOrder(orderId);
        orderAccessPolicy.requireSeller(order, userId);
        return ResponseEntity.ok(OrderResponse.from(orderService.updateShipping(orderId, request)));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmReceipt(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId
    ) {
        BidmartOrder order = orderService.getOrder(orderId);
        orderAccessPolicy.requireBuyer(order, userId);
        return ResponseEntity.ok(OrderResponse.from(orderService.confirmReceipt(orderId)));
    }

    @PostMapping("/{orderId}/dispute")
    public ResponseEntity<OrderResponse> openDispute(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId,
            @RequestBody OpenDisputeRequest request
    ) {
        BidmartOrder order = orderService.getOrder(orderId);
        orderAccessPolicy.requireBuyer(order, userId);
        return ResponseEntity.ok(OrderResponse.from(orderService.openDispute(orderId, request)));
    }

    @PostMapping("/{orderId}/dispute/resolve")
    public ResponseEntity<OrderResponse> resolveDispute(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @PathVariable String orderId,
            @RequestBody ResolveDisputeRequest request
    ) {
        orderAccessPolicy.requireAdmin(rolesHeader);
        return ResponseEntity.ok(OrderResponse.from(orderService.resolveDispute(orderId, request, userId)));
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
}
