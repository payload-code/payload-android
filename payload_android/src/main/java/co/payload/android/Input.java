package co.payload.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.view.ViewParent;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class Input extends LinearLayout {
    private static final Map<String, Map<String, String>> type_map = new HashMap<String, Map<String, String>>(){{
        put("payment", new HashMap<String, String>(){{
            put("card_number", "payment_method[card][card_number]");
            put("expiry", "payment_method[card][expiry]");
            put("card_code", "payment_method[card][card_code]");
            put("cardholder", "payment_method[account_holder]");
            put("account_holder", "payment_method[account_holder]");
        }});

        put("payment_method", new HashMap<String, String>(){{
            put("card_number", "card[card_number]");
            put("expiry", "card[expiry]");
            put("card_code", "card[card_code]");
            put("cardholder", "account_holder");
            put("account_holder", "account_holder");
        }});
    }};

    public static String mapAttr(String type, String attribute) {
        Map<String, String> map = type_map.get(type);

        if (map.containsKey(attribute))
            return map.get(attribute);

        return attribute;
    }

    String type;
    List<EditText> inputs;

    public Input(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        type = this.getTag().toString();

        if (type.equals("pl:card")) {
            inflate(context, R.layout.input_card, this);
            inputs = new ArrayList<EditText>() {{
                add((EditText) findViewById(R.id.card_number));
                add((EditText) findViewById(R.id.expiry));
                add((EditText) findViewById(R.id.cvv));
            }};
        } else {
            inputs = new ArrayList<EditText>() {{
                add(new EditText(context, attrs));
            }};

            this.addView(inputs.get(0));
        }

        for (int i = 0; i < inputs.size(); i++) {
            EditText input = inputs.get(i);
            String tag = input.getTag().toString();

            if (tag.equals("pl:card_number"))
                input.addTextChangedListener(new InputMask("0000 0000 0000 0000"));

            if (tag.equals("pl:expiry"))
                input.addTextChangedListener(new InputMask("00/00"));

            if (tag.equals("pl:card_code"))
                input.addTextChangedListener(new InputMask("000"));
        }
    }
}
