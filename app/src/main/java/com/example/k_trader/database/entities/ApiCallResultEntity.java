package com.example.k_trader.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.ColumnInfo;

/**
 * API 호출 결과를 저장하는 Entity
 */
@Entity(tableName = "api_call_results")
public class ApiCallResultEntity {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;
    
    @ColumnInfo(name = "call_time")
    public long callTime;
    
    @ColumnInfo(name = "api_endpoint")
    public String apiEndpoint;
    
    @ColumnInfo(name = "is_success")
    public boolean isSuccess;
    
    @ColumnInfo(name = "response_data")
    public String responseData;
    
    @ColumnInfo(name = "error_code")
    public String errorCode;
    
    @ColumnInfo(name = "error_message")
    public String errorMessage;
    
    @ColumnInfo(name = "server_error_message")
    public String serverErrorMessage;
    
    @ColumnInfo(name = "transaction_data")
    public String transactionData;
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public ApiCallResultEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCallTime() {
        return callTime;
    }

    public void setCallTime(long callTime) {
        this.callTime = callTime;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getServerErrorMessage() {
        return serverErrorMessage;
    }

    public void setServerErrorMessage(String serverErrorMessage) {
        this.serverErrorMessage = serverErrorMessage;
    }

    public String getTransactionData() {
        return transactionData;
    }

    public void setTransactionData(String transactionData) {
        this.transactionData = transactionData;
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

    /**
     * 최근 호출인지 확인 (1시간 이내)
     */
    public boolean isRecentCall() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - callTime) < (60 * 60 * 1000);
    }

    /**
     * 에러가 있는지 확인
     */
    public boolean hasError() {
        return !isSuccess || errorCode != null || errorMessage != null;
    }

    /**
     * 성공한 호출인지 확인
     */
    public boolean isSuccessfulCall() {
        return isSuccess && !hasError();
    }
}
