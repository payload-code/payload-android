package co.payload.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.app.Dialog;
import android.app.DialogFragment;
import android.view.WindowManager;
import android.view.Gravity;
import android.text.SpannableString;
import android.widget.TextView;
import android.text.style.AlignmentSpan;
import android.text.Layout;
import android.util.Log;

import java.text.NumberFormat;
import java.lang.Runnable;

import co.payload.pl;
import co.payload.arm.ARMObject;

public class CheckoutModal extends DialogFragment {

    private ProgressBar loading = null;
    public Payload.Checkout checkout;
    private pl.Payment pmt;
    private View view;

    public CheckoutModal(Payload.Checkout checkout, pl.Payment pmt) {
        this.checkout = checkout;
        this.pmt = pmt;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        SpannableString title = new SpannableString("Complete Payment");
        title.setSpan(
                new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                title.length(),
                0
            );
        getDialog().setTitle(title);
        return inflater.inflate(R.layout.checkout_modal, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        this.view = view;
        this.view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        this.checkout.req.obj.set("payment_method[type]", "card");

        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        lp.dimAmount = 0.7f;
        getDialog().getWindow().setAttributes(lp);
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        TextView titleView = (TextView)getDialog().findViewById(android.R.id.title);
        TextView description = (TextView) view.findViewById(R.id.description);
        TextView amount = (TextView) view.findViewById(R.id.amount);
        Button pay_btn = (Button) view.findViewById(R.id.pay);
        TextView error = (TextView) view.findViewById(R.id.error);

        if (titleView != null){
            titleView.setGravity(Gravity.CENTER);
        }

        if (this.pmt.getStr("description") != null)
            description.setText(this.pmt.getStr("description"));


        if (this.pmt.getStr("amount") != null) {
            NumberFormat currency = NumberFormat.getCurrencyInstance();
            amount.setText(currency.format(Float.parseFloat(this.pmt.getStr("amount"))));
        }

        loading = (ProgressBar) view.findViewById(R.id.progress_bar);
        loading.setVisibility(8);

        pay_btn.setOnClickListener(new View.OnClickListener() {
              public void onClick(View v) {
                loading.setVisibility(0);
                pay_btn.setEnabled(false);

                error.setVisibility(View.GONE);

                checkout.form.submit();
              }
         });

         this.checkout.initForm();

         this.checkout.error((Exception err) -> {
            loading.setVisibility(8);
            pay_btn.setEnabled(true);
         }).declined((pl.Payment pmt) -> {
            loading.setVisibility(8);
            pay_btn.setEnabled(true);

            error.setText(pmt.getStr("status_message"));
            error.setVisibility(View.VISIBLE);

         });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL,R.style.CheckoutModal);
    }

    public View getView() {
        return view.findViewById(R.id.checkout);
    }


}

