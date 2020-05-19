package co.payload.android;

import android.text.TextWatcher;
import android.text.Editable;


public class InputMask implements TextWatcher {
    private final String mask;
    private boolean masking = false;
    private int delta = 0;

    public InputMask(String mask) {
        this.mask = mask;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (masking)
            return;

        if (delta < 0)
            return;

        masking = true;

        int len = editable.length();
        if (len > mask.length()) {
            editable.delete(mask.length(), len);
        } else if (len > 0 && len < mask.length()) {
            if (mask.charAt(len) != '0') {
                editable.append(mask.charAt(len));
            } else if (mask.charAt(len-1) != '0') {
                editable.insert(len-1, mask, len-1, len);
            }
        }
        masking = false;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        delta = after - count;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}

}
