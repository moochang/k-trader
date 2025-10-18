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

/**
 * Transaction Item 탭을 담당하는 Fragment
 * Created by K-Trader on 2024-12-25.
 */
public class TransactionItemFragment extends Fragment {

    public static final String BROADCAST_CARD_DATA = "TRADE_CARD_DATA";
    public static final String BROADCAST_ERROR_CARD = "TRADE_ERROR_CARD";

    private RecyclerView recyclerViewCards;
    private CardAdapter cardAdapter;
    private LogReceiver logReceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_item, container, false);
        
        // RecyclerView 초기화
        recyclerViewCards = view.findViewById(R.id.recyclerViewCards);
        recyclerViewCards.setLayoutManager(new LinearLayoutManager(getContext()));
        cardAdapter = new CardAdapter();
        recyclerViewCards.setAdapter(cardAdapter);
        
        // BroadcastReceiver 등록
        registerBroadcastReceiver();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // BroadcastReceiver 해제
        if (logReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(logReceiver);
        }
    }

    /**
     * BroadcastReceiver를 등록하는 메서드
     */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_CARD_DATA);
        filter.addAction(BROADCAST_ERROR_CARD);
        
        logReceiver = new LogReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, filter);
    }

    /**
     * Transaction Card와 Error Card 데이터를 처리하는 BroadcastReceiver
     */
    private class LogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_CARD_DATA)) {
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
            } else if (intent.getAction().equals(BROADCAST_ERROR_CARD)) {
                // 에러 카드 데이터 처리
                String errorTime = intent.getStringExtra("errorTime");
                String errorType = intent.getStringExtra("errorType");
                String errorMessage = intent.getStringExtra("errorMessage");
                
                CardAdapter.ErrorCard errorCard = new CardAdapter.ErrorCard(
                    errorTime, errorType, errorMessage
                );
                
                if (cardAdapter != null) {
                    cardAdapter.addErrorCard(errorCard);
                }
            }
        }
    }

    /**
     * CardAdapter 클래스 - TransactionItemFragment 내부에서 사용
     */
    public static class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final java.util.List<Object> cardList = new java.util.ArrayList<>();
        
        private static final int TYPE_TRANSACTION = 0;
        private static final int TYPE_ERROR = 1;

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

            public ErrorCard(String errorTime, String errorType, String errorMessage) {
                this.errorTime = errorTime;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
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

            public CardViewHolder(View itemView) {
                super(itemView);
                textTransactionTime = itemView.findViewById(R.id.textTransactionTime);
                textBtcCurrentPrice = itemView.findViewById(R.id.textBtcCurrentPrice);
                textHourlyChange = itemView.findViewById(R.id.textHourlyChange);
                textEstimatedBalance = itemView.findViewById(R.id.textEstimatedBalance);
                textLastBuyPrice = itemView.findViewById(R.id.textLastBuyPrice);
                textLastSellPrice = itemView.findViewById(R.id.textLastSellPrice);
                textNextBuyPrice = itemView.findViewById(R.id.textNextBuyPrice);
            }
        }

        public static class ErrorViewHolder extends RecyclerView.ViewHolder {
            TextView textErrorTime;
            TextView textErrorType;
            TextView textErrorMessage;

            public ErrorViewHolder(View itemView) {
                super(itemView);
                textErrorTime = itemView.findViewById(R.id.textErrorTime);
                textErrorType = itemView.findViewById(R.id.textErrorType);
                textErrorMessage = itemView.findViewById(R.id.textErrorMessage);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_TRANSACTION) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
                return new CardViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.error_card_view, parent, false);
                return new ErrorViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
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
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (cardList.get(position) instanceof TransactionCard) {
                return TYPE_TRANSACTION;
            } else {
                return TYPE_ERROR;
            }
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
    }

    /**
     * RecyclerView를 맨 아래로 스크롤하는 메서드
     */
    public void scrollToBottom() {
        if (recyclerViewCards != null && cardAdapter != null) {
            int itemCount = cardAdapter.getItemCount();
            if (itemCount > 0) {
                recyclerViewCards.smoothScrollToPosition(itemCount - 1);
            }
        }
    }

    /**
     * Auto scroll 기능을 활성화/비활성화하는 메서드
     */
    public void setAutoScroll(boolean enabled) {
        // Auto scroll 기능은 카드가 추가될 때마다 자동으로 스크롤하는 기능
        // 현재는 카드가 추가될 때마다 맨 위에 추가되므로 별도의 auto scroll 로직이 필요하지 않음
        // 필요시 나중에 구현 가능
    }
}
