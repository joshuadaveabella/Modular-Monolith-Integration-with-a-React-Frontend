package edu.cit.abella;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Living in the parent package edu.cit.abella means component scanning
// automatically covers both edu.cit.abella.shop and edu.cit.abella.inventory
// without needing an explicit @ComponentScan.
@SpringBootApplication
public class ShopInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopInventoryApplication.class, args);
    }
}
