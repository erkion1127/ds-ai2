package com.dsai.rag.common.exception;

public class RagException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public RagException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public RagException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RagException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public enum ErrorCode {
        DOCUMENT_NOT_FOUND("Document not found"),
        EMBEDDING_FAILED("Failed to generate embeddings"),
        VECTOR_STORE_ERROR("Vector store operation failed"),
        INGESTION_ERROR("Document ingestion failed"),
        RETRIEVAL_ERROR("Document retrieval failed"),
        LLM_ERROR("LLM processing failed"),
        INVALID_REQUEST("Invalid request"),
        INTERNAL_ERROR("Internal server error");
        
        private final String message;
        
        ErrorCode(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}