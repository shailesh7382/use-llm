package com.usellm.core.port;

import com.usellm.core.model.Message;

import java.util.List;

/**
 * Port interface for conversation memory operations.
 */
public interface MemoryPort {

    void addMessage(String conversationId, Message message);

    List<Message> getMessages(String conversationId);

    List<Message> getRecentMessages(String conversationId, int maxMessages);

    List<Message> getMessagesWithinTokenBudget(String conversationId, int maxTokens);

    void clearConversation(String conversationId);

    void deleteConversation(String conversationId);

    boolean conversationExists(String conversationId);

    int getMessageCount(String conversationId);

    int estimateTotalTokens(String conversationId);
}
