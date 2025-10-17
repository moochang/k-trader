package com.example.k_trader_eth.base;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class NumberTextWatcherForThousand implements TextWatcher {
    EditText editText;

    public NumberTextWatcherForThousand(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        String s = null;

        try {
            editText.removeTextChangedListener(this);
            s = String.format("%,d", Long.parseLong(editable.toString().replaceAll(",", "")));
            editText.setText(s);
            editText.setSelection(s.length());
            editText.addTextChangedListener(this);
        } catch (NumberFormatException e) {
            editText.addTextChangedListener(this);
        }
    }
}
