package com.usellm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.usellm.api",
        "com.usellm.memory",
        "com.usellm.client"
})
public class UseLlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UseLlmApplication.class, args);
    }
}
