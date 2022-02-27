package com.example.k_trader;

import org.junit.Test;

import static org.junit.Assert.*;

public class MainPageTest {
    @Test
    public void getProfitPrice_isCorrect() throws Exception {
        int ret = MainPage.getProfitPrice(47654321);
        assertEquals(400000, ret);
    }
}
