package co.payload.android;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
import android.view.View;
import android.content.ContextWrapper;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.*;

import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import co.payload.Exceptions;

public class GooglePay extends Payload.PaymentPromise<GooglePay> {

    public static final int PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;
    public static final String DIRECT_TOKENIZATION_PUBLIC_KEY =
        "BBDgdWwx73wlWwOUT7L1qjVI3fUoD9LM74L/gYFdO+Yv0JP4kJ7Hj1z/62bPnMk86kyMIFxe29FtZzC4olS68B0=";
    public static final BigDecimal CENTS_IN_A_UNIT = new BigDecimal(100d);
    private PaymentsClient paymentsClient;
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;

    private View btn = null;
    private Activity activity = null;

    static GooglePay instance;

    public GooglePay(Payload.Req req, View view) {
        super(req);
        btn = view;
        activity = getActivity(view);
        paymentsClient = createPaymentsClient(activity);
        instance = this;
    }

    public static PaymentsClient createPaymentsClient(Activity activity) {
        Wallet.WalletOptions walletOptions =
            new Wallet.WalletOptions.Builder().setEnvironment(PAYMENTS_ENVIRONMENT).build();
        return Wallet.getPaymentsClient(activity, walletOptions);
    }

    public void requestPayment(View view) {
        btn.setClickable(false);
        instance = this;
        Intent myIntent = new Intent(activity, GooglePayActivity.class);
        activity.startActivity(myIntent);
    }

    public void openPaymentRequest(Activity activity) {
        double price = Float.parseFloat(this.req.obj.getStr("amount"));
        long priceCents = Math.round(price * CENTS_IN_A_UNIT.longValue());

        Optional<JSONObject> paymentDataRequestJson = getPaymentDataRequest(priceCents);
        if (!paymentDataRequestJson.isPresent()) {
          return;
        }

        PaymentDataRequest request =
            PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());

        if (request != null) {
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(request),
                activity,
                LOAD_PAYMENT_DATA_REQUEST_CODE
            );
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data, Activity activity) {
        activity.finish();

        switch (requestCode) {
          case LOAD_PAYMENT_DATA_REQUEST_CODE:
              switch (resultCode) {

                case Activity.RESULT_OK:
                    PaymentData paymentData = PaymentData.getFromIntent(data);
                    handlePaymentSuccess(paymentData);
                    break;

                case Activity.RESULT_CANCELED:
                    // The user cancelled the payment attempt
                    try {
                        throw new Exceptions.PayloadError(new JSONObject());
                    } catch( Exception e ) {
                        this.failure(e);
                    }
                    break;

                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(data);
                    Log.w("loadPaymentData failed", String.format("Error code: %d", status.getStatusCode()));
                    this.failure(new Exceptions.PayloadError(new JSONObject()));
                    break;
              }

              // Re-enables the Google Pay payment button.
              btn.setClickable(true);
        }
    }

    public void possiblyShowGooglePayButton() {

        final Optional<JSONObject> isReadyToPayJson = getIsReadyToPayRequest();
        if (!isReadyToPayJson.isPresent()) {
            return;
        }

        btn.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  requestPayment(view);
              }
            }
        );

        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
        Task<Boolean> task = paymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(activity,
            new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(Task<Boolean> task) {
                    if (task.isSuccessful()) {
                        if (task.getResult()) {
                            btn.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(activity, "Unavailable!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w("isReadyToPay failed", task.getException());
                    }
             }
         });
    }

    private static Activity getActivity(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        final String paymentInfo = paymentData.toJson();
        if (paymentInfo == null) {
            return;
        }

        try {
            JSONObject paymentMethodData = new JSONObject(paymentInfo).getJSONObject("paymentMethodData");

            final JSONObject tokenizationData = paymentMethodData.getJSONObject("tokenizationData");
            final String tokenizationType = tokenizationData.getString("type");
            final JSONObject token = new JSONObject(tokenizationData.getString("token"));

            this.req.obj.set("googlepay", token);

            new Payload.Run().execute(this.req);

        } catch (JSONException e) {
            throw new RuntimeException("The selected garment cannot be parsed from the list of elements");
        }
    }

    public static Optional<JSONObject> getIsReadyToPayRequest() {
        try {
            JSONObject isReadyToPayRequest = getBaseRequest();
            isReadyToPayRequest.put(
                "allowedPaymentMethods", new JSONArray().put(getBaseCardPaymentMethod()));

            return Optional.of(isReadyToPayRequest);

        } catch (JSONException e) {
            return Optional.empty();
        }
    }

    private static JSONObject getBaseRequest() throws JSONException {
        return new JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0);
    }

    private static JSONObject getBaseCardPaymentMethod() throws JSONException {
        JSONObject cardPaymentMethod = new JSONObject()
            .put("type", "CARD")
            .put("parameters", new JSONObject(){{
                put("allowedCardNetworks", new JSONArray(){{
                    put("AMEX");
                    put("DISCOVER");
                    put("MASTERCARD");
                    put("VISA");
                }});
                put("allowedAuthMethods", new JSONArray(){{
                    //put("PAN_ONLY");
                    put("CRYPTOGRAM_3DS");
                }});
            }});

        return cardPaymentMethod;
    }

    private static JSONObject getCardPaymentMethod() throws JSONException {
        JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
        cardPaymentMethod.put("tokenizationSpecification", getDirectTokenizationSpecification());

        return cardPaymentMethod;
    }

    private static JSONObject getDirectTokenizationSpecification()
          throws JSONException {

        return new JSONObject()
            .put("type", "DIRECT")
            .put("parameters", new JSONObject(){{
                put("protocolVersion", "ECv2");
                put("publicKey", DIRECT_TOKENIZATION_PUBLIC_KEY);
            }});
    }

    private static JSONObject getTransactionInfo(String price) throws JSONException {
        JSONObject transactionInfo = new JSONObject();
        transactionInfo.put("totalPrice", price);
        transactionInfo.put("totalPriceStatus", "FINAL");
        transactionInfo.put("countryCode", "US");
        transactionInfo.put("currencyCode", "USD");
        transactionInfo.put("checkoutOption", "COMPLETE_IMMEDIATE_PURCHASE");

        return transactionInfo;
    }

    private static JSONObject getMerchantInfo() throws JSONException {
        return new JSONObject().put("merchantName", "Example Merchant");
    }

    public static String centsToString(long cents) {
        return new BigDecimal(cents)
            .divide(CENTS_IN_A_UNIT, RoundingMode.HALF_EVEN)
            .setScale(2, RoundingMode.HALF_EVEN)
            .toString();
    }

    public static Optional<JSONObject> getPaymentDataRequest(long priceCents) {

        final String price = centsToString(priceCents);

        try {
            JSONObject paymentDataRequest = getBaseRequest();
            paymentDataRequest.put(
                "allowedPaymentMethods", new JSONArray().put(getCardPaymentMethod()));
            paymentDataRequest.put("transactionInfo", getTransactionInfo(price));
            //paymentDataRequest.put("merchantInfo", getMerchantInfo());

            return Optional.of(paymentDataRequest);

        } catch (JSONException e) {
            return Optional.empty();
        }
    }

}
