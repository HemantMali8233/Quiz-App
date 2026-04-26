package com.example.quizapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Shows a simple splash screen, then moves to Login.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_TIME = 2000L; // 2 seconds
    private static final long THREE_DAYS_MS = 3L * 24L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            checkLoginSession();
        }, SPLASH_TIME);
    }

    private void checkLoginSession() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            // Not logged in to Firebase
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE);
        long lastLoginTime = prefs.getLong("lastLoginTime", 0);
        int userId = prefs.getInt("userId", -1);
        String userName = prefs.getString("userName", null);

        long currentTime = System.currentTimeMillis();

        // Still check for 3-day expiration or missing local data
        if (userId != -1 && userName != null && (currentTime - lastLoginTime) < THREE_DAYS_MS) {
            // Session is still valid
            Intent i = new Intent(SplashActivity.this, MainActivity.class);
            i.putExtra("userId", userId);
            i.putExtra("userName", userName);
            startActivity(i);
        } else {
            // Session expired locally or user doesn't exist in SQLite
            // But they ARE logged in to Firebase, so LoginActivity will handle profile sync
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }
}
