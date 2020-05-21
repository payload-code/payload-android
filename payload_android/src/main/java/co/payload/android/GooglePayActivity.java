package co.payload.android;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class GooglePayActivity extends Activity {
    GooglePay gpay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gpay = GooglePay.instance;
        gpay.openPaymentRequest((Activity)this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        gpay.onActivityResult(requestCode, resultCode, data, this);
    }

}
