package com.usellm.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "llm.client.base-url=http://localhost:11434/v1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UseLlmApplicationTest {

    @Test
    void contextLoads() {
    }
}
