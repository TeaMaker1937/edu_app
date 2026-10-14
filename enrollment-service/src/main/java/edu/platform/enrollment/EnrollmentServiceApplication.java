package edu.platform.enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EnrollmentServiceApplication {

    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();   // ← ЭТО
        SpringApplication.run(EnrollmentServiceApplication.class, args);
    }
}