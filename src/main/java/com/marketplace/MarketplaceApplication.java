package com.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication()
@EnableScheduling
public class MarketplaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApplication.class, args);
    }

//    @Bean
//    CommandLineRunner initDatabase(UserRepository repository) {
//        return args -> {
//            repository.save(new User("Alice", "alice@example.com"));
//            repository.save(new User("Bob", "bob@example.com"));
//            System.out.println("Sample data initialized!");
//        };
//    }

}
