package com.example.k_trader.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Ignore;

/**
 * Transaction 에러 정보를 저장하는 Entity
 */
@Entity(tableName = "transaction_errors")
public class ErrorEntity {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;
    
    @ColumnInfo(name = "error_time")
    public long errorTime;
    
    @ColumnInfo(name = "error_type")
    public String errorType;
    
    @ColumnInfo(name = "error_message")
    public String errorMessage;
    
    @ColumnInfo(name = "error_code")
    public String errorCode;
    
    @ColumnInfo(name = "transaction_context")
    public String transactionContext;
    
    @ColumnInfo(name = "stack_trace")
    public String stackTrace;
    
    @ColumnInfo(name = "is_resolved")
    public boolean isResolved;
    
    @ColumnInfo(name = "api_error_details")
    public String apiErrorDetails;
    
    @ColumnInfo(name = "resolution_note")
    public String resolutionNote;
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public ErrorEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isResolved = false;
    }

    @Ignore
    public ErrorEntity(long errorTime, String errorType, String errorMessage) {
        this();
        this.errorTime = errorTime;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    @Ignore
    public ErrorEntity(long errorTime, String errorType, String errorMessage, 
                      String errorCode, String transactionContext) {
        this(errorTime, errorType, errorMessage);
        this.errorCode = errorCode;
        this.transactionContext = transactionContext;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getErrorTime() {
        return errorTime;
    }

    public void setErrorTime(long errorTime) {
        this.errorTime = errorTime;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(String transactionContext) {
        this.transactionContext = transactionContext;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getApiErrorDetails() {
        return apiErrorDetails;
    }

    public void setApiErrorDetails(String apiErrorDetails) {
        this.apiErrorDetails = apiErrorDetails;
    }

    /**
     * 에러가 최근 발생한 것인지 확인 (24시간 이내)
     */
    public boolean isRecentError() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - errorTime) < (24 * 60 * 60 * 1000);
    }

    /**
     * 에러 심각도 반환
     */
    public ErrorSeverity getSeverity() {
        if (errorType == null) return ErrorSeverity.UNKNOWN;
        
        switch (errorType.toLowerCase()) {
            case "network error":
                return ErrorSeverity.MEDIUM;
            case "api error":
                return ErrorSeverity.HIGH;
            case "validation error":
                return ErrorSeverity.LOW;
            case "authentication error":
                return ErrorSeverity.CRITICAL;
            case "rate limit error":
                return ErrorSeverity.MEDIUM;
            case "server error":
                return ErrorSeverity.HIGH;
            default:
                return ErrorSeverity.UNKNOWN;
        }
    }

    /**
     * 에러 심각도 열거형
     */
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL, UNKNOWN
    }
}
