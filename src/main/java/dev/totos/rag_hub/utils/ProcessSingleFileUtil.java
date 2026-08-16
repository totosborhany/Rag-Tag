package dev.totos.rag_hub.utils;

import dev.totos.rag_hub.entity.Document;
import dev.totos.rag_hub.entity.DocumentStatus;
import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.repository.UserRepository;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ProcessSingleFileUtil {
    private  final UserRepository userRepository;
    private final VectorStore vectorStore;
    ProcessSingleFileUtil(UserRepository userRepository, VectorStore vectorStore){
        this.userRepository=userRepository;
        this.vectorStore=vectorStore;
    }
    @Async
    @Transactional
    public CompletableFuture<Document> processSingleFile(MultipartFile file, UUID userId, FileSystemResource resource, File tempFile) throws IOException {
        Document finalDocument;
        try {
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> rawDocuments = tikaDocumentReader.get();
            TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(500).withMinChunkSizeChars(400).build();
            List<org.springframework.ai.document.Document> chunkedDocuments = textSplitter.apply(rawDocuments);
            if(chunkedDocuments.size()==0){
                throw new ApiException(file.getOriginalFilename() + " contains no extractable text", HttpStatus.UNPROCESSABLE_ENTITY);            }
            for (org.springframework.ai.document.Document doc : chunkedDocuments) {
                doc.getMetadata().put("userId", userId.toString());
                doc.getMetadata().put("fileName", file.getOriginalFilename());
                doc.getMetadata().put("type","NORMAL");
            }
            User user = userRepository.findById(userId).orElseThrow(() -> new ApiException("User not found", HttpStatus.BAD_REQUEST));
            vectorStore.add(chunkedDocuments);
            finalDocument = new Document(user, file.getOriginalFilename(), file.getContentType(), chunkedDocuments.size(), DocumentStatus.COMPLETED, file.getSize());

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }

        }

        return   CompletableFuture.completedFuture(finalDocument);
    }
}
