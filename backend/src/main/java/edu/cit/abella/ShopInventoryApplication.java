package edu.cit.abella;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Parent package, so component scanning covers shop, inventory,
// notification, supplier, events and config without an explicit
// @ComponentScan. @EnableScheduling turns on the @Scheduled jobs in the
// supplier module (queue processor + status poller).
@SpringBootApplication
@EnableScheduling
public class ShopInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopInventoryApplication.class, args);
    }
}
