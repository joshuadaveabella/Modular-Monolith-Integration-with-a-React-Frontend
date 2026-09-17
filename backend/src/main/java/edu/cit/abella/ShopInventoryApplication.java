package edu.cit.abella;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Parent package, so component scanning picks up shop, inventory,
// notification, events and config without an explicit @ComponentScan.
@SpringBootApplication
public class ShopInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopInventoryApplication.class, args);
    }
}
