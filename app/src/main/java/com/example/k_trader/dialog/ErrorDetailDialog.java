package com.example.k_trader.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.example.k_trader.R;
import com.example.k_trader.TransactionItemFragment;

/**
 * ErrorCard 상세 정보를 보여주는 다이얼로그
 */
public class ErrorDetailDialog extends Dialog {
    
    private TransactionItemFragment.CardAdapter.ErrorCard errorCard;
    private Context context;

    public ErrorDetailDialog(@NonNull Context context, 
                           TransactionItemFragment.CardAdapter.ErrorCard errorCard) {
        super(context);
        this.context = context;
        this.errorCard = errorCard;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 커스텀 레이아웃 설정
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_error_detail, null);
        setContentView(view);
        
        // 데이터 바인딩
        bindErrorData(view);
        
        // 다이얼로그 설정
        setTitle("에러 상세 정보");
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    /**
     * Error 데이터를 UI에 바인딩
     */
    private void bindErrorData(View view) {
        TextView textErrorTime = view.findViewById(R.id.textDetailErrorTime);
        TextView textErrorType = view.findViewById(R.id.textDetailErrorType);
        TextView textErrorMessage = view.findViewById(R.id.textDetailErrorMessage);
        TextView textDetailApiError = view.findViewById(R.id.textDetailApiError);
        
        // 데이터 설정
        textErrorTime.setText(errorCard.errorTime != null ? 
            errorCard.errorTime : "정보 없음");
        textErrorType.setText(errorCard.errorType != null ? 
            errorCard.errorType : "정보 없음");
        textErrorMessage.setText(errorCard.errorMessage != null ? 
            errorCard.errorMessage : "정보 없음");
        
        // 서버 API 상세 에러 정보 표시
        String apiErrorDetails = getApiErrorDetails();
        textDetailApiError.setText(apiErrorDetails);
        
        // 시간 포맷팅 (타임스탬프인 경우)
        if (errorCard.errorTime != null && 
            errorCard.errorTime.matches("\\d+")) {
            try {
                long timestamp = Long.parseLong(errorCard.errorTime);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                textErrorTime.setText(sdf.format(new java.util.Date(timestamp)));
            } catch (NumberFormatException e) {
                // 타임스탬프가 아닌 경우 원본 텍스트 유지
            }
        }
        
        // 에러 타입에 따른 색상 설정
        setErrorTypeColor(textErrorType, errorCard.errorType);
    }

    /**
     * 서버 API 상세 에러 정보 반환
     */
    private String getApiErrorDetails() {
        StringBuilder details = new StringBuilder();
        boolean hasAnyInfo = false;
        
        // 디버깅을 위한 로그 추가
        android.util.Log.d("", "ErrorCard data:");
        android.util.Log.d("ErrorDetailDialog", "  apiEndpoint: " + errorCard.apiEndpoint);
        android.util.Log.d("ErrorDetailDialog", "  errorCode: " + errorCard.errorCode);
        android.util.Log.d("ErrorDetailDialog", "  serverErrorMessage: " + errorCard.serverErrorMessage);
        android.util.Log.d("ErrorDetailDialog", "  apiErrorDetails: " + errorCard.apiErrorDetails);
        android.util.Log.d("ErrorDetailDialog", "  errorMessage: " + errorCard.errorMessage);
        
        // API Endpoint 정보 (서버 URL 포함)
        if (errorCard.apiEndpoint != null && !errorCard.apiEndpoint.isEmpty()) {
            details.append("Server URL: https://api.bithumb.com").append(errorCard.apiEndpoint).append("\n\n");
            details.append("API Endpoint: ").append(errorCard.apiEndpoint).append("\n\n");
            hasAnyInfo = true;
        }
        
        // 에러 코드 정보
        if (errorCard.errorCode != null && !errorCard.errorCode.isEmpty()) {
            details.append("Error Code: ").append(errorCard.errorCode).append("\n\n");
            hasAnyInfo = true;
        }
        
        // 서버 에러 메시지
        if (errorCard.serverErrorMessage != null && !errorCard.serverErrorMessage.isEmpty()) {
            details.append("Server Error Message:\n").append(errorCard.serverErrorMessage).append("\n\n");
            hasAnyInfo = true;
        }
        
        // API 상세 에러 정보 (JSON 형태) - 강제로 표시
        if (errorCard.apiErrorDetails != null && !errorCard.apiErrorDetails.isEmpty()) {
            details.append("Full Error Details (JSON):\n").append(errorCard.apiErrorDetails).append("\n\n");
            hasAnyInfo = true;
        } else {
            // apiErrorDetails가 없는 경우 강제로 JSON 생성하여 표시
            String forcedJson = createForcedJsonErrorDetails();
            if (forcedJson != null && !forcedJson.isEmpty()) {
                details.append("Full Error Details (JSON):\n").append(forcedJson).append("\n\n");
                hasAnyInfo = true;
            }
        }
        
        // 기본 에러 메시지가 JSON 형태인 경우 별도로 표시
        if (errorCard.errorMessage != null && !errorCard.errorMessage.isEmpty()) {
            android.util.Log.d("ErrorDetailDialog", "Checking errorMessage for JSON: " + errorCard.errorMessage);
            if (isJsonFormat(errorCard.errorMessage)) {
                android.util.Log.d("ErrorDetailDialog", "errorMessage is JSON format");
                details.append("Error Details (JSON Format):\n").append(formatJsonString(errorCard.errorMessage)).append("\n\n");
            } else {
                android.util.Log.d("ErrorDetailDialog", "errorMessage is not JSON format");
                details.append("Error Message:\n").append(errorCard.errorMessage).append("\n\n");
            }
            hasAnyInfo = true;
        }
        

        // 정보가 없는 경우 - 기본 정보라도 표시
        if (!hasAnyInfo) {
            details.append("API Error Information:\n\n");
            details.append("Error Time: ").append(errorCard.errorTime != null ? errorCard.errorTime : "Unknown").append("\n");
            details.append("Error Type: ").append(errorCard.errorType != null ? errorCard.errorType : "Unknown").append("\n");
            details.append("Error Message: ").append(errorCard.errorMessage != null ? errorCard.errorMessage : "No message available").append("\n\n");
        }
        
        return details.toString();
    }

    /**
     * 강제로 JSON 에러 정보 생성 (apiErrorDetails가 없는 경우)
     */
    private String createForcedJsonErrorDetails() {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n");
            
            // API endpoint 정보
            if (errorCard.apiEndpoint != null && !errorCard.apiEndpoint.isEmpty()) {
                jsonBuilder.append("  \"server_url\": \"https://api.bithumb.com").append(errorCard.apiEndpoint).append("\",\n");
                jsonBuilder.append("  \"api_endpoint\": \"").append(errorCard.apiEndpoint).append("\",\n");
            } else {
                jsonBuilder.append("  \"server_url\": \"https://api.bithumb.com/unknown\",\n");
                jsonBuilder.append("  \"api_endpoint\": \"/unknown\",\n");
            }
            
            // 에러 코드
            if (errorCard.errorCode != null && !errorCard.errorCode.isEmpty()) {
                jsonBuilder.append("  \"error_code\": \"").append(errorCard.errorCode).append("\",\n");
            } else {
                jsonBuilder.append("  \"error_code\": \"Unknown\",\n");
            }
            
            // 서버 에러 메시지
            if (errorCard.serverErrorMessage != null && !errorCard.serverErrorMessage.isEmpty()) {
                jsonBuilder.append("  \"server_message\": \"").append(errorCard.serverErrorMessage.replace("\"", "\\\"")).append("\",\n");
            } else {
                jsonBuilder.append("  \"server_message\": \"No message\",\n");
            }
            
            // 기본 에러 메시지
            if (errorCard.errorMessage != null && !errorCard.errorMessage.isEmpty()) {
                jsonBuilder.append("  \"original_error\": \"").append(errorCard.errorMessage.replace("\"", "\\\"").replace("\n", "\\n")).append("\",\n");
            } else {
                jsonBuilder.append("  \"original_error\": \"No error message\",\n");
            }
            
            // 타임스탬프
            jsonBuilder.append("  \"timestamp\": \"").append(System.currentTimeMillis()).append("\",\n");
            jsonBuilder.append("  \"error_type\": \"API_CALL_FAILURE\",\n");
            jsonBuilder.append("  \"api_provider\": \"Bithumb\",\n");
            jsonBuilder.append("  \"request_method\": \"GET\"\n");
            jsonBuilder.append("}");
            
            return jsonBuilder.toString();
        } catch (Exception e) {
            return "{\"error\":\"Failed to create forced JSON\",\"message\":\"Error in JSON creation\"}";
        }
    }

    /**
     * 문자열이 JSON 형태인지 확인
     */
    private boolean isJsonFormat(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String trimmed = text.trim();
        android.util.Log.d("ErrorDetailDialog", "Checking JSON format for: " + trimmed);
        
        // 기본 JSON 형태 체크
        boolean isJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
                        (trimmed.startsWith("[") && trimmed.endsWith("]"));
        
        // 추가로 JSON 키워드가 포함되어 있는지 체크
        if (!isJson) {
            isJson = trimmed.contains("\"status\"") || 
                    trimmed.contains("\"message\"") || 
                    trimmed.contains("\"error\"") ||
                    trimmed.contains("\"name\"") ||
                    trimmed.contains("\"code\"");
        }
        
        android.util.Log.d("ErrorDetailDialog", "Is JSON format: " + isJson);
        return isJson;
    }
    
    /**
     * JSON 문자열을 읽기 쉽게 포맷팅
     */
    private String formatJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) return "";
        
        try {
            android.util.Log.d("ErrorDetailDialog", "Formatting JSON: " + jsonString);
            
            // 간단한 JSON 포맷팅 (들여쓰기 추가)
            String formatted = jsonString
                .replace("{", "{\n  ")
                .replace("}", "\n}")
                .replace(",", ",\n  ")
                .replace("\n  \n", "\n");
            
            // 추가 포맷팅 개선
            formatted = formatted
                .replace("\"status\"", "\n  \"status\"")
                .replace("\"message\"", "\n  \"message\"")
                .replace("\"error\"", "\n  \"error\"")
                .replace("\"name\"", "\n  \"name\"")
                .replace("\"code\"", "\n  \"code\"");
            
            android.util.Log.d("ErrorDetailDialog", "Formatted JSON: " + formatted);
            return formatted;
        } catch (Exception e) {
            android.util.Log.e("ErrorDetailDialog", "Error formatting JSON", e);
            return jsonString; // 포맷팅 실패 시 원본 반환
        }
    }

    /**
     * 에러 타입에 따른 색상 설정
     */
    private void setErrorTypeColor(TextView textView, String errorType) {
        if (errorType == null) return;
        
        switch (errorType.toLowerCase()) {
            case "network error":
                textView.setTextColor(context.getResources().getColor(R.color.network_error_color));
                break;
            case "api error":
                textView.setTextColor(context.getResources().getColor(R.color.api_error_color));
                break;
            case "validation error":
                textView.setTextColor(context.getResources().getColor(R.color.validation_error_color));
                break;
            case "authentication error":
                textView.setTextColor(context.getResources().getColor(R.color.auth_error_color));
                break;
            case "rate limit error":
                textView.setTextColor(context.getResources().getColor(R.color.rate_limit_error_color));
                break;
            case "server error":
                textView.setTextColor(context.getResources().getColor(R.color.server_error_color));
                break;
            default:
                textView.setTextColor(context.getResources().getColor(R.color.default_error_color));
                break;
        }
    }

    /**
     * 정적 팩토리 메서드로 다이얼로그 생성
     */
    public static void show(Context context, 
                          TransactionItemFragment.CardAdapter.ErrorCard errorCard) {
        ErrorDetailDialog dialog = new ErrorDetailDialog(context, errorCard);
        dialog.show();
    }
}
