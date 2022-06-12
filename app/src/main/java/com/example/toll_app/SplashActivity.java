package com.example.toll_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {

    private int delay = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        Handler handler = new Handler();

        handler.postDelayed(() -> {
            Intent mainIntent = new Intent(this,MainActivity.class);
            startActivity(mainIntent);
            finish();
        }, delay);
    }
}