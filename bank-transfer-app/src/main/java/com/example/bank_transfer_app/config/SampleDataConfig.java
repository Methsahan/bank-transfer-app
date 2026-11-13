package com.example.bank_transfer_app.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.Repository.CustomerRepository;

@Configuration
public class SampleDataConfig {

    @Bean
    CommandLineRunner initDatabase(
            CustomerRepository customerRepository) {

        return args -> {

            if (customerRepository.count() == 0) {

                Customer customer1 = new Customer();

                customer1.setAccountNumber("100001");
                customer1.setFullName("Kamal");
                customer1.setEmail("kamal@gmail.com");
                customer1.setBalance(new BigDecimal("50000.00"));

                Customer customer2 = new Customer();

                customer2.setAccountNumber("100002");
                customer2.setFullName("Nimal");
                customer2.setEmail("nimal@gmail.com");
                customer2.setBalance(new BigDecimal("25000.00"));

                Customer customer3 = new Customer();

                customer3.setAccountNumber("100003");
                customer3.setFullName("Sunil");
                customer3.setEmail("sunil@gmail.com");
                customer3.setBalance(new BigDecimal("10000.00"));

                customerRepository.save(customer1);
                customerRepository.save(customer2);
                customerRepository.save(customer3);
            }
        };
    }
}