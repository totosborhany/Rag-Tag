package dev.totos.rag_hub.controllers;

import dev.totos.rag_hub.entity.Document;
import dev.totos.rag_hub.entity.DocumentStatus;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.DocumentResponse;
import dev.totos.rag_hub.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService documentService;
    DocumentController(DocumentService documentService){
    this.documentService=documentService;
}
    @PostMapping
    public ResponseEntity<Map<String,Object>> UploadDocument( Principal principal, @RequestParam("file") List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new ApiException("Please upload at least one file", HttpStatus.BAD_REQUEST);
        }
        UUID userId = UUID.fromString(principal.getName());
        List<Document> documentList = documentService.ingestFile(userId,files);
        List<DocumentResponse> documentResponse = new ArrayList<>();
        for(Document document:documentList){
        documentResponse.add(new DocumentResponse(document.getFileName(), LocalDateTime.now(),document.getFileType(),document.getFileSize(), DocumentStatus.COMPLETED));
        }

        Map<String,Object> response = Map.of("Message :",documentList.size()+ " documents have been successfully uploaded",
                "Documents : ",documentResponse);
        return ResponseEntity.status(201).body(response);
    }
    @GetMapping
    public ResponseEntity<Map<String,Object>> findMyDocuments( Principal principal){
        UUID userId = UUID.fromString(principal.getName()) ;
        List<Document> myDocuments = new ArrayList<>();
        List<DocumentResponse> finalDocuments = new ArrayList<>();

        myDocuments = documentService.findMyDocuments(userId);
        for(Document document:myDocuments){
            finalDocuments.add(new DocumentResponse(document.getFileName(), document.getUploadedAt(),document.getFileType(),document.getFileSize(), document.getStatus()));
        }
        Map<String,Object> response = Map.of("Message :",finalDocuments.size()+ " documents you have ",
                "Documents : ",finalDocuments);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Map<String,Object>> findMyDocument( Principal principal,@PathVariable UUID id) {
        UUID userId = UUID.fromString(principal.getName()) ;
        Document returnedDocument = documentService.findMyDocument(userId,id);

        DocumentResponse finalDocument = new DocumentResponse(returnedDocument.getFileName(), returnedDocument.getUploadedAt(),returnedDocument.getFileType(),returnedDocument.getFileSize(),returnedDocument.getStatus());

        Map<String,Object> response = Map.of("Message :","Document successfully returned successfully",
                "Document : ",finalDocument);
        return ResponseEntity.status(200).body(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMyDocument( Principal principal,@PathVariable UUID id) {
        UUID userId = UUID.fromString(principal.getName()) ;
         documentService.findMyDocumentAndDelete(userId,id);

        Map<String,Object> response = Map.of("Message :","Document successfully Deleted");
        return ResponseEntity.noContent().build();
    }
}
