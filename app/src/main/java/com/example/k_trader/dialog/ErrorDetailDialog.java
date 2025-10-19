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
        
        // API 상세 에러 정보 (JSON 형태)
        if (errorCard.apiErrorDetails != null && !errorCard.apiErrorDetails.isEmpty()) {
            details.append("Full Error Details (JSON):\n").append(errorCard.apiErrorDetails).append("\n\n");
            hasAnyInfo = true;
        }
        
        // 기본 에러 메시지가 JSON 형태인 경우 별도로 표시
        if (errorCard.errorMessage != null && !errorCard.errorMessage.isEmpty()) {
            if (isJsonFormat(errorCard.errorMessage)) {
                details.append("Error Details (JSON Format):\n").append(formatJsonString(errorCard.errorMessage)).append("\n\n");
            } else {
                details.append("Error Message:\n").append(errorCard.errorMessage).append("\n\n");
            }
            hasAnyInfo = true;
        }
        
        // 추가 디버깅 정보
        if (hasAnyInfo) {
            details.append("Debug Information:\n");
            details.append("- Error occurred during API call\n");
            details.append("- Check network connection\n");
            details.append("- Verify API credentials\n");
            details.append("- Review server status\n\n");
        }
        
        // 정보가 없는 경우 - 기본 정보라도 표시
        if (!hasAnyInfo) {
            details.append("API Error Information:\n\n");
            details.append("Error Time: ").append(errorCard.errorTime != null ? errorCard.errorTime : "Unknown").append("\n");
            details.append("Error Type: ").append(errorCard.errorType != null ? errorCard.errorType : "Unknown").append("\n");
            details.append("Error Message: ").append(errorCard.errorMessage != null ? errorCard.errorMessage : "No message available").append("\n\n");
            
            // API 호출 정보가 없는 경우 안내 메시지
            details.append("Note: Detailed API information is not available.\n");
            details.append("This error may have occurred during:\n");
            details.append("- Network connection issues\n");
            details.append("- Server API calls\n");
            details.append("- Data processing errors\n\n");
            details.append("Please check your network connection and try again.");
        }
        
        return details.toString();
    }

    /**
     * 문자열이 JSON 형태인지 확인
     */
    private boolean isJsonFormat(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    /**
     * JSON 문자열을 읽기 쉽게 포맷팅
     */
    private String formatJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) return "";
        
        try {
            // 간단한 JSON 포맷팅 (들여쓰기 추가)
            String formatted = jsonString
                .replace("{", "{\n  ")
                .replace("}", "\n}")
                .replace(",", ",\n  ")
                .replace("\n  \n", "\n");
            
            return formatted;
        } catch (Exception e) {
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
