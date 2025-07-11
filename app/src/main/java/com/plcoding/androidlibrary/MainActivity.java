package com.plcoding.androidlibrary;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pub.secure.LootPeApiClient;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LootPeApiClient lootPeApiClient = new LootPeApiClient(this);
        lootPeApiClient.initiatePayment("pk_aBcD1234EfGh5326", 100.0, "USD", "sk_xYzAbC1234567890DefGhIjKlMnOpRErS",
                new LootPeApiClient.PaymentApiCallback() {
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