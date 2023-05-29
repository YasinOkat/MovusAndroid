package com.example.sql2;

import static com.example.sql2.AnaMenu.animateButton;

import android.app.AlertDialog;
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

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ArabaAl extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String BASE_URL = "http://192.168.1.193:5000";
    private static final String GET_ARABALAR_ENDPOINT = "/getArabalar";
    private static final String INSERT_DATA_ENDPOINT = "/insertData";
    private static final String DELETE_DATA_ENDPOINT = "/deleteData";

    private Spinner spinner;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.araba_al);

        spinner = findViewById(R.id.spinner1);
        spinner.setOnItemSelectedListener(this);

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(fetchData());
    }

    private Disposable fetchData() {
        return Observable.fromCallable(this::getSpinnerItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccess, this::onError);
    }

    private void onSuccess(List<String> spinnerItems) {
        spinnerItems.add(0, "Araba seçiniz");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void onError(Throwable e) {
        Log.e("ArabaAl", "Error fetching spinner items", e);
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
        Request request = new Request.Builder().url(BASE_URL + GET_ARABALAR_ENDPOINT).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Failed to fetch spinner items");
            }

            String responseData = response.body().string();
            JSONArray jsonArray = new JSONArray(responseData);
            List<String> items = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                items.add(jsonObject.getString("Plaka"));
            }

            return items;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }

    public void SQL_Insert(View v) {
        Button cikisBtn = findViewById(R.id.btn_araba_cikis);
        cikisBtn.setEnabled(false);

        String selectedItem = (String) spinner.getSelectedItem();
        String gidecek_yer_str = getInputText(R.id.input_gidilen_yer);
        String gidis_amaci_str = getInputText(R.id.input_gidisamaci);

        if (TextUtils.isEmpty(gidecek_yer_str)) {
            showErrorAndFocus(R.id.input_gidilen_yer, "Gideceğiniz yer giriniz");
            cikisBtn.setEnabled(true);
            return;
        }

        if (TextUtils.isEmpty(gidis_amaci_str)) {
            showErrorAndFocus(R.id.input_gidisamaci, "Gidiş amacı giriniz");
            cikisBtn.setEnabled(true);
            return;
        }

        if (selectedItem == null || selectedItem.equals("Araba seçiniz")) {
            showDialog();
            cikisBtn.setEnabled(true);
            return;
        }

        Disposable disposable = Observable.fromCallable(this::getSpinnerItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(spinnerItems -> {
                    if (!spinnerItems.contains(selectedItem)) {
                        new AlertDialog.Builder(this)
                                .setMessage("Arabayı senden önce başka biri almış.")
                                .show();
                    } else {
                        Disposable innerDisposable = Observable.fromCallable(() -> {
                                    JSONObject jsonParam = new JSONObject();
                                    jsonParam.put("plaka", selectedItem);
                                    jsonParam.put("ad", getIntent().getExtras().getString("kullanici_al"));
                                    jsonParam.put("hedef", gidecek_yer_str);
                                    jsonParam.put("amac", gidis_amaci_str);

                                    HttpURLConnection conn = openConnectionForUrl(BASE_URL + INSERT_DATA_ENDPOINT);
                                    writeJsonOutputToConnection(jsonParam, conn);

                                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                        String response = readHttpResponseToString(conn);
                                        JSONObject jsonResponse = new JSONObject(response);
                                        return jsonResponse.getString("message");
                                    } else {
                                        throw new RuntimeException("Error: " + conn.getResponseCode());
                                    }
                                })
                                .flatMap(message -> {
                                    JSONObject jsonParam = new JSONObject();
                                    jsonParam.put("plaka", selectedItem);

                                    HttpURLConnection conn = openConnectionForUrl(BASE_URL + DELETE_DATA_ENDPOINT);
                                    writeJsonOutputToConnection(jsonParam, conn);

                                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                        return Observable.just(message);
                                    } else {
                                        throw new RuntimeException("Error: " + conn.getResponseCode());
                                    }
                                })
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(message -> {
                                    showDialogAndFinish();
                                    cikisBtn.setEnabled(true);
                                }, e -> {
                                    setTextToTextView(R.id.txt_araba_cikis, "Error: " + e.getMessage());
                                    cikisBtn.setEnabled(true);
                                });

                        compositeDisposable.add(innerDisposable);
                    }
                }, e -> {
                    Log.e("ArabaAl", "Error fetching spinner items", e);
                    Toast.makeText(this, "Failed to fetch spinner items", Toast.LENGTH_SHORT).show();
                    cikisBtn.setEnabled(true);
                });

        cikisBtn.setOnClickListener(v1 -> animateButton(cikisBtn));
        compositeDisposable.add(disposable);
    }

    private String getInputText(int viewId) {
        EditText editText = findViewById(viewId);
        return editText.getText().toString().trim();
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

    private void showErrorAndFocus(int viewId, String message) {
        EditText editText = findViewById(viewId);
        editText.setError(message);
        editText.requestFocus();
    }

    private void setTextToTextView(int viewId, String message) {
        TextView textView = findViewById(viewId);
        textView.setText(message);
    }

    private void showDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Araba seçiniz")
                .show();
    }

    private void showDialogAndFinish() {
        new AlertDialog.Builder(this)
                .setMessage("Çıkış yapıldı.")
                .setCancelable(false)
                .setPositiveButton("Tamam", (dialogInterface, i) -> finish())
                .show();
    }
}

