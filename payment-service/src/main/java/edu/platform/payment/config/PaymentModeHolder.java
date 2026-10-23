package edu.platform.payment.config;

import edu.platform.payment.domain.PaymentMode;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class PaymentModeHolder {

    private final AtomicReference<PaymentMode> mode =
            new AtomicReference<>(PaymentMode.SUCCESS);
    private final AtomicReference<Integer> delaySeconds =
            new AtomicReference<>(10);

    public PaymentMode get() { return mode.get(); }

    public void set(PaymentMode m) { mode.set(m); }

    public int delaySeconds() { return delaySeconds.get(); }

    public void setDelaySeconds(int seconds) {
        if (seconds < 0) throw new IllegalArgumentException("delaySeconds < 0");
        delaySeconds.set(seconds);
    }
}