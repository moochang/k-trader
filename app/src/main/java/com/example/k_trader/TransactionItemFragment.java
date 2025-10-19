package com.example.k_trader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.k_trader.base.TradeData;
import com.example.k_trader.database.DatabaseMonitor;
import com.example.k_trader.database.ApiCallResultRepository;
import com.example.k_trader.database.ApiCallResultEntity;
import com.example.k_trader.data.TransactionDataManager;
import com.example.k_trader.dialog.TransactionDetailDialog;
import com.example.k_trader.dialog.ErrorDetailDialog;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import java.util.List;

/**
 * Transaction Item 탭을 담당하는 Fragment
 * DB 구독 시스템을 통해 실시간으로 주문 데이터를 표시
 * Created by K-Trader on 2024-12-25.
 */
public class TransactionItemFragment extends Fragment implements DatabaseMonitor.DatabaseChangeListener {

    public static final String BROADCAST_CARD_DATA = "TRADE_CARD_DATA";
    public static final String BROADCAST_ERROR_CARD = "TRADE_ERROR_CARD";
    public static final String BROADCAST_TRANSACTION_DATA = "com.example.k_trader.TRANSACTION_DATA_UPDATED";

    private RecyclerView recyclerViewCards;
    private CardAdapter cardAdapter;
    private LogReceiver logReceiver;
    private DatabaseMonitor databaseMonitor;
    private TransactionDataManager transactionDataManager;
    private ApiCallResultRepository apiCallResultRepository;
    private CompositeDisposable disposables;
    private String subscriberId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_item, container, false);
        
        // RecyclerView 초기화
        recyclerViewCards = view.findViewById(R.id.recyclerViewCards);
        recyclerViewCards.setLayoutManager(new LinearLayoutManager(getContext()));
        cardAdapter = new CardAdapter();
        recyclerViewCards.setAdapter(cardAdapter);
        
        // Database Monitor 초기화
        databaseMonitor = DatabaseMonitor.getInstance(getContext());
        subscriberId = "TransactionItemFragment_" + System.currentTimeMillis();
        
        // TransactionDataManager 초기화 및 데이터 로드
        transactionDataManager = TransactionDataManager.getInstance(getContext());
        transactionDataManager.loadTransactionData();
        
        // ApiCallResultRepository 초기화
        apiCallResultRepository = ApiCallResultRepository.getInstance(
            com.example.k_trader.database.OrderDatabase.getInstance(getContext()).apiCallResultDao());
        
        // CompositeDisposable 초기화
        disposables = new CompositeDisposable();
        
        // BroadcastReceiver 등록
        if (getContext() != null) {
            registerBroadcastReceiver();
        }
        
        // DB 구독 시작
        subscribeToDatabase();
        
        // API 호출 결과 모니터링 시작
        startApiCallResultMonitoring();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // DB 구독 해제
        if (databaseMonitor != null) {
            databaseMonitor.unsubscribe(subscriberId);
        }
        
        // BroadcastReceiver 해제
        if (logReceiver != null && getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(logReceiver);
        }
        
        // TransactionDataManager 정리
        if (transactionDataManager != null) {
            transactionDataManager.cleanup();
        }
        
        // RxJava Disposables 정리
        if (disposables != null) {
            disposables.clear();
        }
    }

    /**
     * API 호출 결과 모니터링 시작
     */
    private void startApiCallResultMonitoring() {
        if (apiCallResultRepository != null) {
            // 최근 API 호출 결과 모니터링 (1시간 이내)
            Disposable disposable = apiCallResultRepository.getRecentApiCallResults()
                .subscribe(
                    apiCallResults -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> updateCardsFromApiResults(apiCallResults));
                        }
                    },
                    throwable -> {
                        android.util.Log.e("TransactionItemFragment", "Error monitoring API call results", throwable);
                    }
                );
            
            disposables.add(disposable);
        }
    }

    /**
     * API 호출 결과를 기반으로 카드 업데이트
     */
    private void updateCardsFromApiResults(List<ApiCallResultEntity> apiCallResults) {
        if (cardAdapter == null) return;
        
        // 기존 에러 카드들 제거
        cardAdapter.clearErrorCards();
        
        // 최신 API 호출 결과들을 카드로 변환
        for (ApiCallResultEntity result : apiCallResults) {
            if (result.hasError()) {
                // 에러가 있는 경우 ErrorCard 생성 - 더 상세한 정보 포함
                String errorMessage = result.getErrorMessage();
                String apiEndpoint = result.getApiEndpoint();
                String errorCode = result.getErrorCode();
                String serverErrorMessage = result.getServerErrorMessage();
                String responseData = result.getResponseData();
                
                // API endpoint가 없는 경우 기본값 설정
                if (apiEndpoint == null || apiEndpoint.isEmpty()) {
                    apiEndpoint = "/unknown";
                }
                
                // 에러 코드가 없는 경우 기본값 설정
                if (errorCode == null || errorCode.isEmpty()) {
                    errorCode = "Unknown";
                }
                
                // 서버 에러 메시지가 없는 경우 기본 에러 메시지 사용
                if (serverErrorMessage == null || serverErrorMessage.isEmpty()) {
                    serverErrorMessage = errorMessage != null ? errorMessage : "No error message";
                }
                
                // Response data가 없는 경우 기본 에러 메시지 사용
                if (responseData == null || responseData.isEmpty()) {
                    responseData = errorMessage != null ? errorMessage : "No response data";
                }
                
                // JSON 형태의 상세 에러 정보 생성
                String jsonErrorDetails = createJsonErrorDetails(apiEndpoint, errorCode, serverErrorMessage, errorMessage);
                
                // 디버깅을 위한 로그 추가
                android.util.Log.d("TransactionItemFragment", "Creating ErrorCard with:");
                android.util.Log.d("TransactionItemFragment", "  apiEndpoint: " + apiEndpoint);
                android.util.Log.d("TransactionItemFragment", "  errorCode: " + errorCode);
                android.util.Log.d("TransactionItemFragment", "  serverErrorMessage: " + serverErrorMessage);
                android.util.Log.d("TransactionItemFragment", "  errorMessage: " + errorMessage);
                android.util.Log.d("TransactionItemFragment", "  jsonErrorDetails: " + jsonErrorDetails);
                
                CardAdapter.ErrorCard errorCard = new CardAdapter.ErrorCard(
                    String.valueOf(result.getCallTime()),
                    "API Error",
                    errorMessage != null ? errorMessage : "API call failed",
                    apiEndpoint,
                    errorCode,
                    serverErrorMessage,
                    jsonErrorDetails
                );
                cardAdapter.addErrorCard(errorCard);
            } else if (result.isSuccessfulCall()) {
                // 성공한 경우 TransactionCard 생성 (TransactionData가 있는 경우)
                if (result.getTransactionData() != null) {
                    // JSON에서 TransactionData 복원하여 카드 생성
                    // 이 부분은 필요에 따라 구현
                }
            }
        }
    }

    /**
     * JSON 형태의 상세 에러 정보 생성
     */
    private String createJsonErrorDetails(String apiEndpoint, String errorCode, String serverErrorMessage, String errorMessage) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n");
            jsonBuilder.append("  \"server_url\": \"https://api.bithumb.com").append(apiEndpoint).append("\",\n");
            jsonBuilder.append("  \"api_endpoint\": \"").append(apiEndpoint).append("\",\n");
            jsonBuilder.append("  \"error_code\": \"").append(errorCode != null ? errorCode : "Unknown").append("\",\n");
            jsonBuilder.append("  \"server_message\": \"").append(serverErrorMessage != null ? serverErrorMessage.replace("\"", "\\\"") : "No message").append("\",\n");
            jsonBuilder.append("  \"original_error\": \"").append(errorMessage != null ? errorMessage.replace("\"", "\\\"").replace("\n", "\\n") : "No error message").append("\",\n");
            jsonBuilder.append("  \"timestamp\": \"").append(System.currentTimeMillis()).append("\",\n");
            jsonBuilder.append("  \"error_type\": \"API_CALL_FAILURE\",\n");
            jsonBuilder.append("  \"api_provider\": \"Bithumb\",\n");
            jsonBuilder.append("  \"request_method\": \"GET\"\n");
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        } catch (Exception e) {
            return "{\"error\":\"Failed to create error details\",\"message\":\"" + 
                (errorMessage != null ? errorMessage.replace("\"", "\\\"") : "No message") + "\"}";
        }
    }

    /**
     * DB 구독을 시작하는 메서드
     */
    private void subscribeToDatabase() {
        if (databaseMonitor != null && getContext() != null) {
            // 모든 주문 변경사항 구독
            databaseMonitor.subscribeToAllOrders(this);
        }
    }

    /**
     * DB 변경 리스너 구현
     */
    @Override
    public void onOrdersChanged(List<TradeData> orders) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (cardAdapter != null) {
                    cardAdapter.updateOrders(orders);
                }
            });
        }
    }

    /**
     * BroadcastReceiver를 등록하는 메서드
     */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_CARD_DATA);
        filter.addAction(BROADCAST_ERROR_CARD);
        filter.addAction(BROADCAST_TRANSACTION_DATA);
        
        logReceiver = new LogReceiver();
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, filter);
        }
    }

    /**
     * Transaction Card와 Error Card 데이터를 처리하는 BroadcastReceiver
     */
    private class LogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(BROADCAST_CARD_DATA)) {
                // 카드 데이터 처리
                String transactionTime = intent.getStringExtra("transactionTime");
                String btcCurrentPrice = intent.getStringExtra("btcCurrentPrice");
                String hourlyChange = intent.getStringExtra("hourlyChange");
                String estimatedBalance = intent.getStringExtra("estimatedBalance");
                String lastBuyPrice = intent.getStringExtra("lastBuyPrice");
                String lastSellPrice = intent.getStringExtra("lastSellPrice");
                String nextBuyPrice = intent.getStringExtra("nextBuyPrice");
                
                CardAdapter.TransactionCard card = new CardAdapter.TransactionCard(
                    transactionTime, btcCurrentPrice, hourlyChange, estimatedBalance,
                    lastBuyPrice, lastSellPrice, nextBuyPrice
                );
                
                if (cardAdapter != null) {
                    cardAdapter.addCard(card);
                }
            } else if (intent.getAction() != null && intent.getAction().equals(BROADCAST_ERROR_CARD)) {
                // 에러 카드 데이터 처리
                String errorTime = intent.getStringExtra("errorTime");
                String errorType = intent.getStringExtra("errorType");
                String errorMessage = intent.getStringExtra("errorMessage");
                String apiEndpoint = intent.getStringExtra("apiEndpoint");
                String errorCode = intent.getStringExtra("errorCode");
                String serverErrorMessage = intent.getStringExtra("serverErrorMessage");
                String apiErrorDetails = intent.getStringExtra("apiErrorDetails");
                
                CardAdapter.ErrorCard errorCard = new CardAdapter.ErrorCard(
                    errorTime, errorType, errorMessage, apiEndpoint, errorCode, serverErrorMessage, apiErrorDetails
                );
                
                if (cardAdapter != null) {
                    cardAdapter.addErrorCard(errorCard);
                }
            } else if (intent.getAction() != null && intent.getAction().equals(BROADCAST_TRANSACTION_DATA)) {
                // TransactionDataManager에서 전송된 데이터 처리
                String transactionTime = intent.getStringExtra("transactionTime");
                String btcCurrentPrice = intent.getStringExtra("btcCurrentPrice");
                String hourlyChange = intent.getStringExtra("hourlyChange");
                String estimatedBalance = intent.getStringExtra("estimatedBalance");
                String lastBuyPrice = intent.getStringExtra("lastBuyPrice");
                String lastSellPrice = intent.getStringExtra("lastSellPrice");
                String nextBuyPrice = intent.getStringExtra("nextBuyPrice");
                boolean isFromServer = intent.getBooleanExtra("isFromServer", false);
                
                CardAdapter.TransactionCard card = new CardAdapter.TransactionCard(
                    transactionTime, btcCurrentPrice, hourlyChange, estimatedBalance,
                    lastBuyPrice, lastSellPrice, nextBuyPrice
                );
                
                if (cardAdapter != null) {
                    // 서버에서 온 데이터인 경우 기존 캐시 데이터를 대체
                    if (isFromServer) {
                        cardAdapter.updateLatestCard(card);
                    } else {
                        cardAdapter.addCard(card);
                    }
                }
            }
        }
    }

    /**
     * CardAdapter 클래스 - TransactionItemFragment 내부에서 사용
     * DB 기반 주문 데이터를 표시하도록 확장
     */
    public static class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final java.util.List<Object> cardList = new java.util.ArrayList<>();
        
        private static final int TYPE_TRANSACTION = 0;
        private static final int TYPE_ERROR = 1;
        private static final int TYPE_ORDER = 2;

        public static class TransactionCard {
            public String transactionTime;
            public String btcCurrentPrice;
            public String hourlyChange;
            public String estimatedBalance;
            public String lastBuyPrice;
            public String lastSellPrice;
            public String nextBuyPrice;

            public TransactionCard(String transactionTime, String btcCurrentPrice, String hourlyChange,
                                String estimatedBalance, String lastBuyPrice, String lastSellPrice, String nextBuyPrice) {
                this.transactionTime = transactionTime;
                this.btcCurrentPrice = btcCurrentPrice;
                this.hourlyChange = hourlyChange;
                this.estimatedBalance = estimatedBalance;
                this.lastBuyPrice = lastBuyPrice;
                this.lastSellPrice = lastSellPrice;
                this.nextBuyPrice = nextBuyPrice;
            }
        }

        public static class ErrorCard {
            public String errorTime;
            public String errorType;
            public String errorMessage;
            public String apiEndpoint;
            public String errorCode;
            public String serverErrorMessage;
            public String apiErrorDetails;

            public ErrorCard(String errorTime, String errorType, String errorMessage) {
                this.errorTime = errorTime;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
            }

            public ErrorCard(String errorTime, String errorType, String errorMessage, 
                           String apiEndpoint, String errorCode, String serverErrorMessage, String apiErrorDetails) {
                this.errorTime = errorTime;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
                this.apiEndpoint = apiEndpoint;
                this.errorCode = errorCode;
                this.serverErrorMessage = serverErrorMessage;
                this.apiErrorDetails = apiErrorDetails;
            }
        }

        public static class OrderCard {
            public TradeData tradeData;

            public OrderCard(TradeData tradeData) {
                this.tradeData = tradeData;
            }
        }

        public static class CardViewHolder extends RecyclerView.ViewHolder {
            TextView textTransactionTime;
            TextView textBtcCurrentPrice;
            TextView textHourlyChange;
            TextView textEstimatedBalance;
            TextView textLastBuyPrice;
            TextView textLastSellPrice;
            TextView textNextBuyPrice;
            CardAdapter adapter;

            public CardViewHolder(View itemView, CardAdapter adapter) {
                super(itemView);
                this.adapter = adapter;
                textTransactionTime = itemView.findViewById(R.id.textTransactionTime);
                textBtcCurrentPrice = itemView.findViewById(R.id.textBtcCurrentPrice);
                textHourlyChange = itemView.findViewById(R.id.textHourlyChange);
                textEstimatedBalance = itemView.findViewById(R.id.textEstimatedBalance);
                textLastBuyPrice = itemView.findViewById(R.id.textLastBuyPrice);
                textLastSellPrice = itemView.findViewById(R.id.textLastSellPrice);
                textNextBuyPrice = itemView.findViewById(R.id.textNextBuyPrice);
                
                // 클릭 리스너 설정
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        Object item = adapter.cardList.get(position);
                        if (item instanceof TransactionCard) {
                            TransactionCard card = (TransactionCard) item;
                            TransactionDetailDialog.show(itemView.getContext(), card);
                        }
                    }
                });
            }
        }

        public static class ErrorViewHolder extends RecyclerView.ViewHolder {
            TextView textErrorTime;
            TextView textErrorType;
            TextView textErrorMessage;
            CardAdapter adapter;

            public ErrorViewHolder(View itemView, CardAdapter adapter) {
                super(itemView);
                this.adapter = adapter;
                textErrorTime = itemView.findViewById(R.id.textErrorTime);
                textErrorType = itemView.findViewById(R.id.textErrorType);
                textErrorMessage = itemView.findViewById(R.id.textErrorMessage);
                
                // 클릭 리스너 설정
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        Object item = adapter.cardList.get(position);
                        if (item instanceof ErrorCard) {
                            ErrorCard card = (ErrorCard) item;
                            ErrorDetailDialog.show(itemView.getContext(), card);
                        }
                    }
                });
            }
        }

        public static class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView textOrderId;
            TextView textOrderType;
            TextView textOrderStatus;
            TextView textUnits;
            TextView textPrice;
            TextView textPlacedTime;

            public OrderViewHolder(View itemView) {
                super(itemView);
                textOrderId = itemView.findViewById(R.id.textOrderId);
                textOrderType = itemView.findViewById(R.id.textOrderType);
                textOrderStatus = itemView.findViewById(R.id.textOrderStatus);
                textUnits = itemView.findViewById(R.id.textUnits);
                textPrice = itemView.findViewById(R.id.textPrice);
                textPlacedTime = itemView.findViewById(R.id.textPlacedTime);
            }
        }

        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_TRANSACTION) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
                return new CardViewHolder(view, this);
            } else if (viewType == TYPE_ERROR) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.error_card_view, parent, false);
                return new ErrorViewHolder(view, this);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_card_view, parent, false);
                return new OrderViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof CardViewHolder) {
                TransactionCard card = (TransactionCard) cardList.get(position);
                CardViewHolder cardHolder = (CardViewHolder) holder;
                cardHolder.textTransactionTime.setText(card.transactionTime);
                cardHolder.textBtcCurrentPrice.setText(card.btcCurrentPrice);
                cardHolder.textHourlyChange.setText(card.hourlyChange);
                cardHolder.textEstimatedBalance.setText(card.estimatedBalance);
                cardHolder.textLastBuyPrice.setText(card.lastBuyPrice);
                cardHolder.textLastSellPrice.setText(card.lastSellPrice);
                cardHolder.textNextBuyPrice.setText(card.nextBuyPrice);
            } else if (holder instanceof ErrorViewHolder) {
                ErrorCard card = (ErrorCard) cardList.get(position);
                ErrorViewHolder errorHolder = (ErrorViewHolder) holder;
                errorHolder.textErrorTime.setText(card.errorTime);
                errorHolder.textErrorType.setText(card.errorType);
                errorHolder.textErrorMessage.setText(card.errorMessage);
            } else if (holder instanceof OrderViewHolder) {
                OrderCard card = (OrderCard) cardList.get(position);
                OrderViewHolder orderHolder = (OrderViewHolder) holder;
                TradeData tradeData = card.tradeData;
                
                orderHolder.textOrderId.setText(tradeData.getId());
                orderHolder.textOrderType.setText(tradeData.getType().toString());
                orderHolder.textOrderStatus.setText(tradeData.getStatus().toString());
                orderHolder.textUnits.setText(String.format(java.util.Locale.getDefault(), "%.4f", tradeData.getUnits()));
                orderHolder.textPrice.setText(String.format(java.util.Locale.getDefault(), "%,d", tradeData.getPrice()));
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault());
                orderHolder.textPlacedTime.setText(sdf.format(new java.util.Date(tradeData.getPlacedTime())));
            }
        }

        @Override
        public int getItemViewType(int position) {
            Object item = cardList.get(position);
            if (item instanceof TransactionCard) {
                return TYPE_TRANSACTION;
            } else if (item instanceof ErrorCard) {
                return TYPE_ERROR;
            } else if (item instanceof OrderCard) {
                return TYPE_ORDER;
            }
            return TYPE_TRANSACTION;
        }

        @Override
        public int getItemCount() {
            return cardList.size();
        }

        public void addCard(TransactionCard card) {
            cardList.add(0, card); // 최신 카드를 맨 위에 추가
            notifyItemInserted(0);
        }

        public void addErrorCard(ErrorCard card) {
            cardList.add(0, card); // 최신 에러 카드를 맨 위에 추가
            notifyItemInserted(0);
        }

        /**
         * 모든 ErrorCard 제거
         */
        public void clearErrorCards() {
            for (int i = cardList.size() - 1; i >= 0; i--) {
                if (cardList.get(i) instanceof ErrorCard) {
                    cardList.remove(i);
                    notifyItemRemoved(i);
                }
            }
        }

        /**
         * 최신 TransactionCard를 업데이트 (서버 데이터로 대체)
         */
        public void updateLatestCard(TransactionCard card) {
            // 기존 TransactionCard가 있는지 확인하고 첫 번째 것을 교체
            for (int i = 0; i < cardList.size(); i++) {
                if (cardList.get(i) instanceof TransactionCard) {
                    cardList.set(i, card);
                    notifyItemChanged(i);
                    return;
                }
            }
            // TransactionCard가 없으면 새로 추가
            addCard(card);
        }


        public void updateOrders(List<TradeData> orders) {
            int oldSize = cardList.size();
            cardList.clear();
            
            for (TradeData order : orders) {
                OrderCard orderCard = new OrderCard(order);
                cardList.add(orderCard);
            }
            
            // 데이터셋 크기에 따라 적절한 알림 사용
            if (oldSize == 0) {
                // 처음 로드되는 경우
                notifyItemRangeInserted(0, cardList.size());
            } else if (cardList.isEmpty()) {
                // 모든 데이터가 삭제된 경우
                notifyItemRangeRemoved(0, oldSize);
            } else if (oldSize == cardList.size()) {
                // 크기가 같은 경우 - 개별 아이템 변경으로 처리
                notifyItemRangeChanged(0, cardList.size());
            } else {
                // 크기가 다른 경우에만 전체 데이터셋 교체
                notifyDataSetChanged();
            }
        }
    }

    /**
     * RecyclerView를 맨 아래로 스크롤하는 메서드
     */
    public void scrollToBottom() {
        if (recyclerViewCards != null && cardAdapter != null) {
            int itemCount = cardAdapter.getItemCount();
            if (itemCount > 0) {
                // RecyclerView가 레이아웃이 완료된 후 스크롤 실행
                recyclerViewCards.post(() -> recyclerViewCards.smoothScrollToPosition(itemCount - 1));
            }
        }
    }
}
