package com.pub.secure;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class LootPeApiClient {

    private final String BASE_URL = "https://loot.pe/v1/";
    private WeakReference<Context> contextRef;
    private PaymentApiCallback callback;
    private ActivityResultLauncher<Intent> paymentLauncher;
    private boolean isLauncherInitialized = false;
    ProgressDialog progressDialog;

    public interface PaymentApiCallback {
        void onSuccess(String transactionId);
        void onError(String message, int errorCode);
    }

    public LootPeApiClient(Context context) {
        if (!(context instanceof FragmentActivity)) {
            throw new IllegalArgumentException("Context must be a FragmentActivity");
        }
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        this.contextRef = new WeakReference<>(context);
        initializePaymentLauncher((FragmentActivity) context);
    }

    private void initializePaymentLauncher(FragmentActivity activity) {
        try {
            paymentLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (callback == null) return;

                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            String transactionId = result.getData().getStringExtra("transactionId");
                            if (transactionId != null) {
                                callback.onSuccess(transactionId);
                            } else {
                                callback.onError("Invalid transaction ID", -1);
                            }
                        } else {
                            callback.onError("Payment failed or was canceled", -2);
                        }
                    });
            isLauncherInitialized = true;
        } catch (Exception e) {
            Log.e("ZerogatewayApiClient", "Failed to initialize payment launcher", e);
            isLauncherInitialized = false;
        }
    }

    public void initiatePayment(String publicKey, double amount, String currency, String secretKey, PaymentApiCallback callback) {
        Context context = contextRef.get();
        if (context == null) {
            callback.onError("Context is not available", -3);
            return;
        }

        if (!isLauncherInitialized) {
            callback.onError("Payment system not ready", -4);
            return;
        }

        if (amount <= 0) {
            callback.onError("Amount must be greater than zero", 199);
            return;
        }

        this.callback = callback;
        progressDialog.show();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("public_key", publicKey);
            jsonObject.put("amount", amount);
            if (currency != null) jsonObject.put("currency", currency);
            jsonObject.put("callback_url", "https://loot.pe/payment-success");

            AndroidNetworking.post(BASE_URL + "payment/initiate")
                    .addHeaders("Authorization", "Bearer " + secretKey)
                    .addJSONObjectBody(jsonObject)
                    .setTag("initiatePayment")
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                InitiatePaymentResponse initiateResponse = new Gson().fromJson(response.toString(), InitiatePaymentResponse.class);
                                if (initiateResponse.isSuccess() && initiateResponse.getPaymentUrl() != null) {
                                    launchPaymentActivity(initiateResponse.getPaymentUrl());
                                } else {
                                    callback.onError(initiateResponse.getMessage() != null ?
                                            initiateResponse.getMessage() : "Payment initiation failed", 400);
                                }
                            } catch (Exception e) {
                                callback.onError("Error parsing response: " + e.getMessage(), -5);
                            }
                            progressDialog.dismiss();
                        }

                        @Override
                        public void onError(ANError error) {
                            handleError(error, callback);
                            progressDialog.dismiss();
                        }
                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            callback.onError("Error creating request: " + e.getMessage(), -6);
        }
    }

    private void launchPaymentActivity(String paymentUrl) {
        Context context = contextRef.get();
        if (context == null) {
            if (callback != null) {
                callback.onError("Context not available", -7);
            }
            return;
        }

        try {
            Intent intent = new Intent(context, PaymentActivity.class);
            intent.putExtra("payment_url", paymentUrl);
            paymentLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("ZerogatewayApiClient", "Failed to launch payment activity", e);
            if (callback != null) {
                callback.onError("Failed to start payment process", -8);
            }
        }
    }

    private void handleError(ANError error, PaymentApiCallback callback) {
        if (error.getErrorBody() != null) {
            try {
                JSONObject errorJson = new JSONObject(error.getErrorBody());
                String message = errorJson.optString("message", "An unknown error occurred");
                callback.onError(message, error.getErrorCode());
            } catch (Exception e) {
                callback.onError("Server error: " + error.getErrorBody(), error.getErrorCode());
            }
        } else {
            callback.onError(error.getMessage() != null ?
                            error.getMessage() : "Network request failed",
                    error.getErrorCode());
        }
    }

    public static class InitiatePaymentResponse {
        private boolean success;
        private String message;
        private String payment_url;

        public String getPaymentUrl() { return payment_url; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}