package com.homedepot.supplychain.enterpriselabormanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class EnterpriseLaborManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseLaborManagementApplication.class, args);
    }
}
