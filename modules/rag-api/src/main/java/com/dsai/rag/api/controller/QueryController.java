package com.dsai.rag.api.controller;

import com.dsai.rag.common.dto.BaseResponse;
import com.dsai.rag.core.service.RagOrchestrator;
import com.dsai.rag.model.QueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
@Tag(name = "Query", description = "RAG Query API")
public class QueryController {
    
    private final RagOrchestrator ragOrchestrator;
    
    @PostMapping
    @Operation(summary = "Execute RAG query", description = "Process a query using the RAG pipeline")
    public ResponseEntity<BaseResponse<RagOrchestrator.RagResponse>> query(
            @Valid @RequestBody QueryRequest request) {
        
        log.info("Received query: {}", request.getQuery());
        
        try {
            RagOrchestrator.RagResponse response = ragOrchestrator.query(request);
            return ResponseEntity.ok(BaseResponse.success(response));
        } catch (Exception e) {
            log.error("Query processing failed", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("QUERY_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the query service is healthy")
    public ResponseEntity<BaseResponse<String>> health() {
        return ResponseEntity.ok(BaseResponse.success("Query service is healthy"));
    }
}