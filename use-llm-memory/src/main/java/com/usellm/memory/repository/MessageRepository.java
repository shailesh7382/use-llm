package com.usellm.memory.repository;

import com.usellm.memory.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.conversationId = :conversationId ORDER BY m.position ASC")
    List<MessageEntity> findByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.conversationId = :conversationId ORDER BY m.position DESC")
    List<MessageEntity> findByConversationIdOrderByPositionDesc(@Param("conversationId") String conversationId);

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.conversation.conversationId = :conversationId")
    int countByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT COALESCE(MAX(m.position), -1) FROM MessageEntity m WHERE m.conversation.conversationId = :conversationId")
    int findMaxPositionByConversationId(@Param("conversationId") String conversationId);
}
