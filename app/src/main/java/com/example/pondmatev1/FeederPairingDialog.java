package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FeederPairingDialog extends DialogFragment {

    private TextView tvCurrentConnection;
    private OkHttpClient client;
    private String feederId;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.feeder_pairing_dialog, null);
        tvCurrentConnection = view.findViewById(R.id.tvCurrentConnection);

        client = new OkHttpClient();
        feederId = getFeederId(requireContext());

        fetchCurrentPairing();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view)
                .setTitle("Feeder Pairing Status")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String getFeederId(Context context) {
        return "feeder_001";
    }

    private void fetchCurrentPairing() {
        String url = "https://pondmate.alwaysdata.net/get_feeder_assignment.php?feeder_id=" + feederId;
        Log.d("FeederPairing", "Fetching pairing for: " + feederId);
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            tvCurrentConnection.setText("❌ Failed to load feeder pairing."));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                tvCurrentConnection.setText("⚠️ Error: " + response.code()));
                    }
                    return;
                }

                String result = response.body().string();
                try {
                    JSONObject json = new JSONObject(result);
                    String pondName = json.optString("pond_name", null);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (pondName != null && !pondName.equals("null") && !pondName.isEmpty()) {
                                tvCurrentConnection.setText("Connected to: " + pondName);
                            } else {
                                tvCurrentConnection.setText("❌ No pond connected ❌");
                            }
                        });
                    }

                } catch (JSONException e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                tvCurrentConnection.setText("⚠️ Invalid response format."));
                    }
                    Log.e("FeederPairingDialog", "JSON parsing error", e);
                }
            }
        });
    }
}
