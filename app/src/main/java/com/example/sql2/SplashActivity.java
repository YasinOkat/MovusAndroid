package com.example.sql2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Animation animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ImageView splashScreenLogo = findViewById(R.id.splash_screen_logo);
        // TextView welcome = findViewById(R.id.splash_screen_text);
        splashScreenLogo.startAnimation(animFadeIn);
        // welcome.startAnimation(animFadeIn);

        new Handler().postDelayed(() -> {
            Intent i = new Intent(SplashActivity.this, Login.class);
            startActivity(i);
            finish();
        }, SPLASH_TIME_OUT);
    }
}
