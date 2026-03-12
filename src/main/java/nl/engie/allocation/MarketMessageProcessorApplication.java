package nl.engie.allocation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MarketMessageProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketMessageProcessorApplication.class, args);
    }
}
