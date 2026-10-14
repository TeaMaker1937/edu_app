package edu.platform.education.shared.demo;

import edu.platform.lib.NotificationTextBuilder;
import edu.platform.lib.PriceCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo/lib")
@Tag(name = "Демо библиотеки", description = "Проверка работы внешней platform-lib")
public class PriceDemoController {

    @Operation(summary = "Расчёт цены со скидкой")
    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> price(
            @RequestParam double price,
            @RequestParam double discount) {
        double result = PriceCalculator.calculateDiscountedPrice(price, discount);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("price", price);
        body.put("discount", discount);
        body.put("result", result);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Текст уведомления о записи на курс")
    @GetMapping("/notification-text")
    public ResponseEntity<Map<String, String>> notificationText(@RequestParam String title) {
        return ResponseEntity.ok(Map.of(
                "message", NotificationTextBuilder.buildEnrollmentMessage(title)));
    }
}