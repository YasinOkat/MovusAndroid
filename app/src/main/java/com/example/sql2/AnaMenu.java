package com.example.sql2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AnaMenu extends AppCompatActivity {

    private String getUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ana_menu);

        retrieveIntentExtras();

        Button arabaAlButton = findViewById(R.id.btn_araba_al);
        arabaAlButton.setOnClickListener(v -> {
            animateButton(arabaAlButton);
            openArabaAlActivity();
        });

        Button arabaBirakButton = findViewById(R.id.btn_araba_birak);
        arabaBirakButton.setOnClickListener(v -> {
            animateButton(arabaBirakButton);
            openArabaBirakActivity();
        });

        Button arabaListesiButton = findViewById(R.id.btn_araba_liste);
        arabaListesiButton.setOnClickListener(v -> {
            animateButton(arabaListesiButton);
            openArabaListeActivity();
        });
    }

    private void retrieveIntentExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            getUsername = extras.getString("username");
        }
    }

    private void openArabaAlActivity() {
        Intent intent = new Intent(this, ArabaAl.class);
        intent.putExtra("kullanici_al", getUsername);
        startActivity(intent);
    }

    private void openArabaBirakActivity() {
        Intent intent = new Intent(this, ArabaBirak.class);
        intent.putExtra("kullanici_al", getUsername);
        startActivity(intent);
    }

    private void openArabaListeActivity() {
        Intent intent = new Intent(this, ArabaListe.class);
        startActivity(intent);
    }

    public static void animateButton(Button button){
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();

        button.animate()
                .alpha(0.5f)
                .setDuration(200)
                .withEndAction(() -> {
                    button.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }
}
