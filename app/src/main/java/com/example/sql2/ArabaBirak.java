package com.example.sql2;

import static com.example.sql2.AnaMenu.animateButton;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ArabaBirak extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String BASE_URL = "http://192.168.1.193:5000";
    private static final String ARABA_BIRAK_ENDPOINT = "/arabaBirak";
    private static final String GET_KULLANILAN_ARABALAR_ENDPOINT = "/getKullanilanArabalar";

    private Spinner spinner;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.araba_birak);

        spinner = findViewById(R.id.spinner2);
        spinner.setOnItemSelectedListener(this);

        compositeDisposable = new CompositeDisposable();
        fetchData();
    }

    private void fetchData() {
        Disposable disposable = Observable.fromCallable(this::getSpinnerItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccess, this::onError);
        compositeDisposable.add(disposable);
    }

    private void onSuccess(List<String> spinnerItems) {
        spinnerItems.add(0, "Araba seçiniz");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void onError(Throwable e) {
        Log.e("ArabaBirak", "Error fetching spinner items", e);
        Toast.makeText(this, "Failed to fetch spinner items", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private List<String> getSpinnerItems() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(BASE_URL + GET_KULLANILAN_ARABALAR_ENDPOINT).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Failed to fetch spinner items");
            }

            String responseData = response.body().string();
            JSONArray jsonArray = new JSONArray(responseData);
            List<String> items = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                items.add(jsonObject.getString("kullanilan_plaka"));
            }

            return items;
        }
    }

    public void SQL_Insert2(View v) {
        Button arabaGirisBtn = findViewById(R.id.btn_araba_giris);
        arabaGirisBtn.setEnabled(false);

        Object selectedItem = spinner.getSelectedItem();
        EditText totalKilometer = findViewById(R.id.input_km);
        String totalKilometerStr = totalKilometer.getText().toString().trim();

        if (TextUtils.isEmpty(totalKilometerStr)) {
            totalKilometer.setError("Toplam kilometre giriniz");
            totalKilometer.requestFocus();
            arabaGirisBtn.setEnabled(true);
            return;
        } else if (!TextUtils.isDigitsOnly(totalKilometerStr)) {
            totalKilometer.setError("Sadece sayı girilebilir");
            totalKilometer.requestFocus();
            arabaGirisBtn.setEnabled(true);
            return;
        } else if (selectedItem == null || selectedItem.equals("Araba seçiniz")) {
            showDialog("Araba seçiniz");
            arabaGirisBtn.setEnabled(true);
            return;
        }

        Disposable disposable = Observable.fromCallable(() -> {
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("plaka", selectedItem);
                    jsonParam.put("kilometre", totalKilometerStr);

                    HttpURLConnection conn = openConnectionForUrl(BASE_URL + ARABA_BIRAK_ENDPOINT);
                    writeJsonOutputToConnection(jsonParam, conn);

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        String response = readHttpResponseToString(conn);
                        JSONObject jsonResponse = new JSONObject(response);
                        return jsonResponse.getString("message");
                    } else {
                        throw new RuntimeException("Error: " + conn.getResponseCode());
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(message -> {
                    showDialogAndFinish("Araba teslim edildi.");
                    arabaGirisBtn.setEnabled(true);
                }, e -> {
                    setTextToTextView(R.id.txt_araba_cikis, "Error: " + e.getMessage());
                    arabaGirisBtn.setEnabled(true);
                });

        arabaGirisBtn.setOnClickListener(v1 -> animateButton(arabaGirisBtn));
        compositeDisposable.add(disposable);
    }

    private HttpURLConnection openConnectionForUrl(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        return conn;
    }

    private void writeJsonOutputToConnection(JSONObject jsonOutput, HttpURLConnection conn) throws IOException {
        try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
            os.write(jsonOutput.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readHttpResponseToString(HttpURLConnection conn) throws IOException {
        return new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A").next();
    }

    private void setTextToTextView(int viewId, String message) {
        TextView textView = findViewById(viewId);
        textView.setText(message);
    }

    private void showDialogAndFinish(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Tamam", (dialogInterface, i) -> finish())
                .show();
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }
}





