package com.plcoding.androidlibrary;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pub.secure.ZerogatewayApiClient;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ZerogatewayApiClient zerogatewayApiClient = new ZerogatewayApiClient(this);
        zerogatewayApiClient.initiatePayment("pk_aBcD1234EfGh5326", 100.0, "USD", "sk_xYzAbC1234567890DefGhIjKlMnOpRErS",
                new ZerogatewayApiClient.PaymentApiCallback() {
                    @Override
                    public void onSuccess(String transactionId) {
                        Log.d("onSuccess", "onSuccess: "+transactionId);
                        Toast.makeText(MainActivity.this, "Payment Successful "+transactionId, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message, int errorCode) {
                        Log.d("onError", "onError: "+message);
                        Toast.makeText(MainActivity.this, ""+message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}