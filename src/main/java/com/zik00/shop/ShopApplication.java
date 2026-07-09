package com.zik00.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.zik00.shop", "com.zik00.admin"})
@EntityScan(basePackages = {"com.zik00.shop", "com.zik00.admin"})
@EnableJpaRepositories(basePackages = {"com.zik00.shop", "com.zik00.admin"})
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }

}
