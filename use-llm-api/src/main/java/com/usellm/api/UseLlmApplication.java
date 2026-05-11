package com.usellm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.usellm.api",
        "com.usellm.memory",
        "com.usellm.client"
})
@EnableJpaRepositories(basePackages = "com.usellm.memory.repository")
@EntityScan(basePackages = "com.usellm.memory.entity")
public class UseLlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UseLlmApplication.class, args);
    }
}
