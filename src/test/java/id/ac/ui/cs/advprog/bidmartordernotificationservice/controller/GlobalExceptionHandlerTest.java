package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.ForbiddenOrderActionException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.NotificationNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void handleOrderNotFoundShouldReturn404Payload() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new OrderNotFoundException("order missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("Order not found: order missing", response.getBody().get("message"));
    }

    @Test
    void handleNotificationNotFoundShouldReturn404Payload() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new NotificationNotFoundException("notif missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Notification not found: notif missing", response.getBody().get("message"));
    }

    @Test
    void handleForbiddenShouldReturn403Payload() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(new ForbiddenOrderActionException("forbidden action"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Forbidden", response.getBody().get("error"));
        assertEquals("forbidden action", response.getBody().get("message"));
    }

    @Test
    void handleBadRequestShouldReturn400Payload() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("bad input", response.getBody().get("message"));
    }
}
