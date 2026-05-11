package com.usellm.memory;

import com.usellm.core.model.Message;
import com.usellm.core.model.Role;
import com.usellm.memory.service.ConversationMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ConversationMemoryServiceTest.TestConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConversationMemoryServiceTest {

    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"com.usellm.memory"})
    static class TestConfig {}

    @Autowired
    private ConversationMemoryService memoryService;

    private static final String CONV_ID = "test-conversation-1";

    @BeforeEach
    void setUp() {
        if (memoryService.conversationExists(CONV_ID)) {
            memoryService.deleteConversation(CONV_ID);
        }
    }

    @Test
    void shouldAddAndRetrieveMessages() {
        memoryService.addMessage(CONV_ID, Message.system("You are helpful."));
        memoryService.addMessage(CONV_ID, Message.user("Hello!"));
        memoryService.addMessage(CONV_ID, Message.assistant("Hi there!"));

        List<Message> messages = memoryService.getMessages(CONV_ID);
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getRole()).isEqualTo(Role.SYSTEM);
        assertThat(messages.get(1).getRole()).isEqualTo(Role.USER);
        assertThat(messages.get(2).getRole()).isEqualTo(Role.ASSISTANT);
    }

    @Test
    void shouldRespectTokenBudget() {
        memoryService.addMessage(CONV_ID, Message.system("System prompt."));
        for (int i = 0; i < 10; i++) {
            memoryService.addMessage(CONV_ID, Message.user("User message number " + i + " with some content."));
            memoryService.addMessage(CONV_ID, Message.assistant("Assistant reply number " + i + " with some response."));
        }

        List<Message> trimmed = memoryService.getMessagesWithinTokenBudget(CONV_ID, 100);
        assertThat(trimmed).isNotEmpty();
        // System message should always be included
        assertThat(trimmed.get(0).getRole()).isEqualTo(Role.SYSTEM);
        // Token count should be within budget
        int totalTokens = trimmed.stream().mapToInt(Message::estimateTokens).sum();
        assertThat(totalTokens).isLessThanOrEqualTo(100);
    }

    @Test
    void shouldRespectSlidingWindow() {
        memoryService.addMessage(CONV_ID, Message.system("System prompt."));
        for (int i = 0; i < 15; i++) {
            memoryService.addMessage(CONV_ID, Message.user("Message " + i));
        }

        List<Message> recent = memoryService.getRecentMessages(CONV_ID, 5);
        assertThat(recent.size()).isLessThanOrEqualTo(6); // 5 + 1 system
        assertThat(recent.get(0).getRole()).isEqualTo(Role.SYSTEM);
    }

    @Test
    void shouldClearConversation() {
        memoryService.addMessage(CONV_ID, Message.user("Hello"));
        memoryService.addMessage(CONV_ID, Message.assistant("Hi"));

        memoryService.clearConversation(CONV_ID);
        assertThat(memoryService.getMessageCount(CONV_ID)).isEqualTo(0);
    }

    @Test
    void shouldEstimateTokens() {
        memoryService.addMessage(CONV_ID, Message.user("Hello world"));
        int tokens = memoryService.estimateTotalTokens(CONV_ID);
        assertThat(tokens).isGreaterThan(0);
    }
}
