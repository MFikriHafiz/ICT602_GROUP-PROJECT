package com.example.medialert;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen; // Keep this for API 31+ splash screen API

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreenActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DURATION = 1500; // 2 seconds
    private FirebaseAuth mAuth;

    // UI elements from your splash.xml (though mostly handled by SplashScreen API)
    private ImageView logoImage;
    private TextView appName;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition for Android 12+ (API 31+)
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash); // Set content view to your provided splash.xml

        // Initialize UI elements (for potential use or pre-API 31 behavior)
        logoImage = findViewById(R.id.logoImage);
        appName = findViewById(R.id.appName);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        // Handler to delay and navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is signed in, navigate to Home
                Intent intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
                startActivity(intent);
            } else {
                // No user is signed in, navigate to Login
                Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            finish(); // Close the splash activity
        }, SPLASH_SCREEN_DURATION);
    }
}