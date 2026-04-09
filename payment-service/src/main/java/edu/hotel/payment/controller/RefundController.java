package edu.hotel.payment.controller;

import edu.hotel.payment.dto.refund.RefundRequest;
import edu.hotel.payment.dto.refund.RefundResponse;
import edu.hotel.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/payments/{id}/refund")
    public ResponseEntity<RefundResponse> requestRefund(
            @PathVariable("id") Long id,
            @RequestBody RefundRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundService.requestRefund(id, request.getAmount(), request.getReason()));
    }

    @GetMapping("/refunds/{id}")
    public ResponseEntity<RefundResponse> getRefundById(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(refundService.getRefundById(id));
    }

    @PostMapping("/refunds/{id}/retry")
    public ResponseEntity<RefundResponse> retryRefund(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(refundService.retryRefund(id));
    }
}
