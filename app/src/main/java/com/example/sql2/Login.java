package com.example.sql2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public class Login extends AppCompatActivity {
    private static final String BASE_URL = "http://192.168.1.193:5000";
    private static final String LOGIN_ENDPOINT = "/login";
    private static final String SHARED_PREF_NAME = "SharedPreferences";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private EditText usernameEditText;
    private EditText passwordEditText;
    private TextView resultTextView;
    private Button loginButton;
    private CheckBox rememberMeCheckbox;
    private SharedPreferences sharedPreferences;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        usernameEditText = findViewById(R.id.input_kullaniciadi);
        passwordEditText = findViewById(R.id.input_sifre);
        resultTextView = findViewById(R.id.textView);
        loginButton = findViewById(R.id.btn_giris);
        rememberMeCheckbox = findViewById(R.id.checkbox_remember);

        compositeDisposable = new CompositeDisposable();
        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);

        boolean rememberMeChecked = getRememberMeStatusFromPreferences();
        if (rememberMeChecked) {
            String savedUsername = getSavedUsernameFromPreferences();
            String savedPassword = getSavedPasswordFromPreferences();

            usernameEditText.setText(savedUsername);
            passwordEditText.setText(savedPassword);

            rememberMeCheckbox.setChecked(true);
        }

        loginButton.setOnClickListener(v -> {
            performLogin();

            loginButton.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        loginButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();

            loginButton.animate()
                    .alpha(0.5f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        loginButton.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start();
                    })
                    .start();
        });
    }

    private void performLogin() {
        loginButton.setEnabled(false);

        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Kullanıcı adı giriniz");
            usernameEditText.requestFocus();
            loginButton.setEnabled(true);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Şifre giriniz");
            passwordEditText.requestFocus();
            loginButton.setEnabled(true);
            return;
        }

        compositeDisposable.add(loginObservable(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            resultTextView.setText(result);
                            if (result.equals("Giriş başarılı")) {
                                if (rememberMeCheckbox.isChecked()) {
                                    saveCredentialsToPreferences(username, password);
                                } else {
                                    clearCredentialsFromPreferences();
                                }

                                Intent intent = new Intent(Login.this, AnaMenu.class);
                                intent.putExtra("username", username);
                                loginButton.setEnabled(true);
                                startActivity(intent);
                            }
                        },
                        error -> {
                            resultTextView.setText("Error: " + error.getMessage());
                            loginButton.setEnabled(true);
                        },
                        () -> loginButton.setEnabled(true)
                ));
    }

    private Observable<String> loginObservable(String username, String password) {
        return Observable.fromCallable(() -> {
            try {
                URL url = new URL(BASE_URL + LOGIN_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                    out.write(String.format("{\"kullaniciadi\":\"%s\",\"sifre\":\"%s\"}", username, password));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        boolean success = jsonResponse.getBoolean("success");
                        if (success) {
                            return "Giriş başarılı";
                        } else {
                            return "Hatalı kullanıcı adı veya parola";
                        }
                    }
                } else {
                    return "Error: " + responseCode;
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL: " + e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException("IO Exception: " + e.getMessage());
            } catch (JSONException e) {
                throw new RuntimeException("JSON Exception: " + e.getMessage());
            }
        });
    }

    private boolean getRememberMeStatusFromPreferences() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    private String getSavedUsernameFromPreferences() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    private String getSavedPasswordFromPreferences() {
        return sharedPreferences.getString(KEY_PASSWORD, "");
    }

    private void saveCredentialsToPreferences(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.apply();
    }

    private void clearCredentialsFromPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}




