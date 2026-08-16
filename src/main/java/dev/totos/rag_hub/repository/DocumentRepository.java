package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    long deleteByUserId(UUID userId);
//    @Query("SELECT d FROM Document d WHERE d.userId = :userId")
//    List<Document> findByUserId(@Param("userId") UUID userId);
Page<Document> findByUserId(UUID userId, Pageable pageable);
    @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND d.id = :id")
    Optional<Document> findDocumentByIdAndUserId(@Param("userId") UUID userId, @Param("id") UUID id);
    @Modifying
    @Transactional
    @Query("DELETE FROM Document d WHERE d.id = :id AND d.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
