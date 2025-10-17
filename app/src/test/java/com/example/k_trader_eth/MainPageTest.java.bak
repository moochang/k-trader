package com.example.k_trader;

import org.junit.Test;

import static org.junit.Assert.*;

public class MainPageTest {
    @Test
    public void getProfitPrice_isCorrect_million() throws Exception {
        int ret = MainPage.getProfitPrice(4765432);
        assertEquals(40000, ret);
    }

    @Test
    public void getProfitPrice_isCorrect_10million() throws Exception {
        int ret = MainPage.getProfitPrice(47654321);
        assertEquals(400000, ret);
    }

    @Test
    public void getProfitPrice_isCorrect_100million() throws Exception {
        int ret = MainPage.getProfitPrice(157654321);
        assertEquals(1500000, ret);
    }

    @Test
    public void getSlotIntervalPrice_isCorrect_100million() throws Exception {
        int price = 154801000;
        int ret = price - (price % MainPage.getSlotIntervalPrice(price));
        assertEquals(154500000, ret);
    }
}
