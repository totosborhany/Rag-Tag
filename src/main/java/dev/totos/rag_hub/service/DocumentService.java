package dev.totos.rag_hub.service;

import dev.totos.rag_hub.entity.Document;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.repository.DocumentRepository;
import dev.totos.rag_hub.utils.ProcessSingleFileUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
@Service
public class DocumentService {
private final DocumentRepository documentRepository;
   private final ProcessSingleFileUtil ProcessSingleFileUtil;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "txt", "docx");
    private final VectorStore vectorStore;
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
DocumentService(DocumentRepository documentRepository, ProcessSingleFileUtil ProcessSingleFileUtil,VectorStore vectorStore){
    this.documentRepository=documentRepository;
    this.ProcessSingleFileUtil = ProcessSingleFileUtil;
    this.vectorStore=vectorStore;
}
    @Transactional
    @RateLimiter(name="ragVectorLimiter")
   public List<Document> ingestFile(UUID userId, List<MultipartFile> files) throws IOException {
        List<CompletableFuture<Document>> documents = new ArrayList<>();
        for(MultipartFile file :files) {
            validateFile(file);
            if(!(file.isEmpty())){

                File tempFile = null;
                tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
                file.transferTo(tempFile);
                FileSystemResource resource = new FileSystemResource(tempFile);
               documents.add(ProcessSingleFileUtil.processSingleFile(file,userId, resource,tempFile));
            }
        }
        List<Document> finalDocuments = documents.stream().map(CompletableFuture::join).toList();
        documentRepository.saveAll(finalDocuments);
        return finalDocuments;
   }

    private void validateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new ApiException("Invalid file name: " + filename, HttpStatus.BAD_REQUEST);
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        String contentType = file.getContentType();

        boolean isExtensionValid = ALLOWED_EXTENSIONS.contains(extension);
        boolean isMimeTypeValid = contentType != null && ALLOWED_MIME_TYPES.contains(contentType);

        if (!isExtensionValid && !isMimeTypeValid) {
            throw new ApiException(
                    "File '" + filename + "' is not supported. Only PDF, TXT, and DOCX files are allowed.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
    public List<Document> findMyDocuments(UUID userId){
    List <Document> myDocuments = new ArrayList<>();
    myDocuments= documentRepository.findByUserId(userId);
    return myDocuments;
    }
    public Document findMyDocument(UUID userId,UUID id){
    Document myDocument = documentRepository.findDocumentByIdAndUserId(userId,id) .orElseThrow(() -> new ApiException("Document not found or access denied", HttpStatus.NOT_FOUND));
    return myDocument;
}
    @Transactional
    public void findMyDocumentAndDelete(UUID userId, UUID documentId) {
        Document document = documentRepository.findDocumentByIdAndUserId(userId, documentId)
                .orElseThrow(() -> new ApiException("Document not found or access denied", HttpStatus.NOT_FOUND));
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(
                b.and(
                      b.eq("userId", userId.toString()),
                        b.eq("fileName", document.getFileName())
                ).build()
        );

        documentRepository.delete(document);
    }
}
