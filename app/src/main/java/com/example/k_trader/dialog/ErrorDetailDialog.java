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
        TextView textErrorDescription = view.findViewById(R.id.textDetailErrorDescription);
        
        // 데이터 설정
        textErrorTime.setText(errorCard.errorTime != null ? 
            errorCard.errorTime : "정보 없음");
        textErrorType.setText(errorCard.errorType != null ? 
            errorCard.errorType : "정보 없음");
        textErrorMessage.setText(errorCard.errorMessage != null ? 
            errorCard.errorMessage : "정보 없음");
        
        // 에러 타입에 따른 상세 설명 설정
        String errorDescription = getErrorDescription(errorCard.errorType);
        textErrorDescription.setText(errorDescription);
        
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
     * 에러 타입에 따른 상세 설명 반환
     */
    private String getErrorDescription(String errorType) {
        if (errorType == null) return "알 수 없는 에러입니다.";
        
        switch (errorType.toLowerCase()) {
            case "network error":
                return "네트워크 연결에 문제가 발생했습니다. 인터넷 연결을 확인해주세요.";
            case "api error":
                return "서버 API 호출 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            case "validation error":
                return "입력 데이터에 문제가 있습니다. 입력값을 확인해주세요.";
            case "authentication error":
                return "인증에 실패했습니다. API 키를 확인해주세요.";
            case "rate limit error":
                return "API 호출 제한에 도달했습니다. 잠시 후 다시 시도해주세요.";
            case "server error":
                return "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            default:
                return "예상치 못한 오류가 발생했습니다. 문제가 지속되면 고객지원에 문의해주세요.";
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
