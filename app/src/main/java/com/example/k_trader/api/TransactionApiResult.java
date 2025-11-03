package com.example.k_trader.api;

import com.example.k_trader.data.TransactionData;
import java.util.List;
import java.util.ArrayList;

/**
 * Transaction API 호출 결과를 담는 클래스
 * SRP: API 호출 결과 데이터 구조만 담당
 */
public class TransactionApiResult {
    private TransactionData transactionData;
    private List<ApiError> errors;

    public TransactionApiResult() {
        this.errors = new ArrayList<>();
    }

    public TransactionData getTransactionData() {
        return transactionData;
    }

    public void setTransactionData(TransactionData transactionData) {
        this.transactionData = transactionData;
    }

    public List<ApiError> getErrors() {
        return errors;
    }

    public void addError(String apiEndpoint, String errorMessage) {
        errors.add(new ApiError(apiEndpoint, errorMessage));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return "에러 없음";
        }

        StringBuilder summary = new StringBuilder();
        for (ApiError error : errors) {
            summary.append(error.getApiEndpoint()).append(": ").append(error.getErrorMessage()).append("\n");
        }
        return summary.toString().trim();
    }

    public boolean isSuccess() {
        return transactionData != null && !hasErrors();
    }

    public String getPrimaryErrorType() {
        if (errors.isEmpty()) {
            return "No Error";
        }
        return errors.get(0).getApiEndpoint();
    }

    /**
     * API 에러 정보를 담는 클래스
     */
    public static class ApiError {
        private String apiEndpoint;
        private String errorMessage;

        public ApiError(String apiEndpoint, String errorMessage) {
            this.apiEndpoint = apiEndpoint;
            this.errorMessage = errorMessage;
        }

        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
