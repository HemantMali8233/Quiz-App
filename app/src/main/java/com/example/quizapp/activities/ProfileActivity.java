package com.example.quizapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapp.R;
import com.example.quizapp.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private TextView textProfileName, textProfileEmail, textTotalPoints, textQuizzesPlayed;
    private TextView textBestETI, textBestManagement;
    private ImageView btnBack;
    private Button btnLogout;
    private DatabaseHelper dbHelper;
    private int userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");

        initViews();
        loadProfileData();

        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> {
            // 🚀 FULL LOGOUT: Clear both Firebase and local session
            FirebaseAuth.getInstance().signOut();
            
            getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void initViews() {
        textProfileName = findViewById(R.id.textProfileName);
        textProfileEmail = findViewById(R.id.textProfileEmail);
        textTotalPoints = findViewById(R.id.textTotalPoints);
        textQuizzesPlayed = findViewById(R.id.textQuizzesPlayed);
        textBestETI = findViewById(R.id.textBestETI);
        textBestManagement = findViewById(R.id.textBestManagement);
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadProfileData() {
        textProfileName.setText(userName);
        
        // Use Firebase Auth to get the actual logged-in user's email
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : "user" + userId + "@quizhub.com";
        textProfileEmail.setText(email);

        int points = dbHelper.getOverallScore(userId);
        int quizzes = dbHelper.getQuizCount(userId);
        int bestETI = dbHelper.getBestScoreBySubject(userId, "ETI");
        int bestMgmt = dbHelper.getBestScoreBySubject(userId, "Management");

        textTotalPoints.setText(String.valueOf(points));
        textQuizzesPlayed.setText(String.valueOf(quizzes));
        textBestETI.setText(bestETI + " pts");
        textBestManagement.setText(bestMgmt + " pts");
    }
}