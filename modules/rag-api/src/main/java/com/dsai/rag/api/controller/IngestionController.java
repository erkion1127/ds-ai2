package com.dsai.rag.api.controller;

import com.dsai.rag.common.dto.BaseResponse;
import com.dsai.rag.ingestion.service.DocumentIngestionService;
import com.dsai.rag.model.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
@Tag(name = "Ingestion", description = "Document Ingestion API")
public class IngestionController {
    
    private final DocumentIngestionService ingestionService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and ingest document", description = "Upload a document for processing and indexing")
    public ResponseEntity<BaseResponse<Document>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received file for ingestion: {}", file.getOriginalFilename());
        
        try {
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            Document document = ingestionService.ingestDocument(tempFile);
            
            Files.deleteIfExists(tempFile);
            
            return ResponseEntity.ok(BaseResponse.success(document));
            
        } catch (Exception e) {
            log.error("Document ingestion failed", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("INGESTION_FAILED", e.getMessage()));
        }
    }
    
    @PostMapping("/file")
    @Operation(summary = "Ingest local file", description = "Ingest a document from local file path")
    public ResponseEntity<BaseResponse<Document>> ingestFile(
            @RequestParam("path") String filePath) {
        
        log.info("Ingesting file from path: {}", filePath);
        
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("FILE_NOT_FOUND", "File does not exist: " + filePath));
            }
            
            Document document = ingestionService.ingestDocument(path);
            return ResponseEntity.ok(BaseResponse.success(document));
            
        } catch (Exception e) {
            log.error("Document ingestion failed", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("INGESTION_FAILED", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", description = "Delete a document and all its chunks from the index")
    public ResponseEntity<BaseResponse<String>> deleteDocument(
            @PathVariable String documentId) {
        
        log.info("Deleting document: {}", documentId);
        
        try {
            ingestionService.deleteDocument(documentId);
            return ResponseEntity.ok(BaseResponse.success("Document deleted successfully"));
        } catch (Exception e) {
            log.error("Document deletion failed", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("DELETION_FAILED", e.getMessage()));
        }
    }
}