package edu.platform.payment.api;

import edu.platform.payment.config.PaymentModeHolder;
import edu.platform.payment.domain.PaymentMode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentModeHolder modeHolder;

    public PaymentController(PaymentModeHolder modeHolder) {
        this.modeHolder = modeHolder;
    }

    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getMode() {
        return ResponseEntity.ok(Map.of(
                "mode", modeHolder.get(),
                "delaySeconds", modeHolder.delaySeconds()));
    }

    @PostMapping("/mode/{mode}")
    public ResponseEntity<Map<String, Object>> setMode(
            @PathVariable PaymentMode mode,
            @RequestParam(required = false, defaultValue = "10") int delaySeconds) {
        modeHolder.set(mode);
        if (mode == PaymentMode.DELAY) {
            modeHolder.setDelaySeconds(delaySeconds);
        }
        return ResponseEntity.ok(Map.of(
                "mode", modeHolder.get(),
                "delaySeconds", modeHolder.delaySeconds()));
    }
}