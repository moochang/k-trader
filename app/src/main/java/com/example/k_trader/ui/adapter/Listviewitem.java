package com.example.k_trader.ui.adapter;

import com.example.k_trader.base.TradeData;

/**
 * Created by 김무창 on 2017-12-20.
 */

public class Listviewitem {
    private int icon;
    private String name;
    private int bgColor;
    private TradeData data;

    public int getIcon() {return icon;}
    public String getName() {return name;}
    public int getColor() {return bgColor;}
    public TradeData getData() {return data;}

    public void setBgColor(int bgColor) {this.bgColor = bgColor;}
    public void setTradeData(TradeData data) {this.data = data;}

    public Listviewitem(int icon, String name){
        this.icon = icon;
        this.name = name;
    }
}
