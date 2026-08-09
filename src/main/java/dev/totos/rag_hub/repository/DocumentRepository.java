package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    long deleteByUserId(UUID userId);
    @Query("SELECT d.fileName,d.fileType,d.fileSize,d.uploadedAt,d.status FROM Document d WHERE d.user.id = :userId")
    List<Document> findByUserId(@Param("userId") UUID userId);
    @Query("SELECT d.fileName,d.fileType,d.fileSize,d.uploadedAt,d.status FROM Document d WHERE d.user.id = :userId and d.id=:id")
    Optional<Document> findDocumentByIdAndUserId(@Param("userId") UUID userId, @Param("id") UUID id);
    @Modifying
    @Transactional
    @Query("DELETE FROM Document d WHERE d.id = :id AND d.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
