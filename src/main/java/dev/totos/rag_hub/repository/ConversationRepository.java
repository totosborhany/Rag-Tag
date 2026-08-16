package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.Conversation;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId")
    Page<Conversation> findByUserId(@Param("userId") UUID userId , Pageable pageable);


    @Query("SELECT c FROM Conversation c WHERE c.id = :id AND c.user.id = :userId")
    Optional<Conversation> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    @Query("SELECT COUNT(c) > 0 FROM Conversation c WHERE c.id = :id AND c.user.id = :userId")
    boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

}