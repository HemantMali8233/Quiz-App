package com.example.quizapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.example.quizapp.models.Question;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyQuizAttemptActivity extends AppCompatActivity {

    private TextView textQuizInfo, textQuestionCounter, textQuestion;
    private Button btnA, btnB, btnC, btnD, btnNext, btnPrevious;
    private ImageView btnBack;

    private List<Question> questions;
    private String subject, co, userName;
    private int userId;
    private int currentQuestionIndex = 0;
    private int newLastIndex;
    private int[] selectedOptions; // Track selections for each question
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_quiz_attempt);

        questions = (List<Question>) getIntent().getSerializableExtra("questions");
        if (questions != null) {
            selectedOptions = new int[questions.size()];
        }
        subject = getIntent().getStringExtra("subject");
        co = getIntent().getStringExtra("co");
        newLastIndex = getIntent().getIntExtra("newIndex", 0);
        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");

        initViews();
        showQuestion();

        btnBack.setOnClickListener(v -> finish());
        btnNext.setOnClickListener(v -> handleNext());
        btnPrevious.setOnClickListener(v -> handlePrevious());

        btnA.setOnClickListener(v -> selectOption(1, (Button) v));
        btnB.setOnClickListener(v -> selectOption(2, (Button) v));
        btnC.setOnClickListener(v -> selectOption(3, (Button) v));
        btnD.setOnClickListener(v -> selectOption(4, (Button) v));
    }

    private void handlePrevious() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            showQuestion();
        }
    }

    private void selectOption(int option, Button b) {
        selectedOptions[currentQuestionIndex] = option;
        highlightSelected(b);
    }

    private void initViews() {
        textQuizInfo = findViewById(R.id.textQuizInfo);
        textQuestionCounter = findViewById(R.id.textQuestionCounter);
        textQuestion = findViewById(R.id.textQuestion);
        btnA = findViewById(R.id.btnOptionA);
        btnB = findViewById(R.id.btnOptionB);
        btnC = findViewById(R.id.btnOptionC);
        btnD = findViewById(R.id.btnOptionD);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnBack = findViewById(R.id.btnBack);

        textQuizInfo.setText(subject + " - " + co);
    }

    private void showQuestion() {
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "No questions found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Question q = questions.get(currentQuestionIndex);
        textQuestionCounter.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.size());
        textQuestion.setText(q.getQuestion());
        btnA.setText("A. " + q.getOption1());
        btnB.setText("B. " + q.getOption2());
        btnC.setText("C. " + q.getOption3());
        btnD.setText("D. " + q.getOption4());

        resetOptionStyles();
        
        // Highlight previously selected option if it exists
        int selected = selectedOptions[currentQuestionIndex];
        if (selected == 1) highlightSelected(btnA);
        else if (selected == 2) highlightSelected(btnB);
        else if (selected == 3) highlightSelected(btnC);
        else if (selected == 4) highlightSelected(btnD);

        // Update button visibility and text
        if (currentQuestionIndex == 0) {
            btnPrevious.setVisibility(View.INVISIBLE);
        } else {
            btnPrevious.setVisibility(View.VISIBLE);
        }

        if (currentQuestionIndex == questions.size() - 1) {
            btnNext.setText("Submit");
        } else {
            btnNext.setText("Next");
        }
    }

    private void highlightSelected(Button b) {
        resetOptionStyles();
        b.setBackgroundResource(R.drawable.option_letter_bg); // Or a specific selected drawable
        b.setTextColor(getResources().getColor(R.color.white));
    }

    private void resetOptionStyles() {
        btnA.setBackgroundResource(R.drawable.button_option);
        btnB.setBackgroundResource(R.drawable.button_option);
        btnC.setBackgroundResource(R.drawable.button_option);
        btnD.setBackgroundResource(R.drawable.button_option);
        btnA.setTextColor(getResources().getColor(R.color.dark_text));
        btnB.setTextColor(getResources().getColor(R.color.dark_text));
        btnC.setTextColor(getResources().getColor(R.color.dark_text));
        btnD.setTextColor(getResources().getColor(R.color.dark_text));
    }

    private void handleNext() {
        if (selectedOptions[currentQuestionIndex] == 0) {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            showQuestion();
        } else {
            calculateScoreAndSubmit();
        }
    }

    private void calculateScoreAndSubmit() {
        score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (selectedOptions[i] == questions.get(i).getCorrectIndex()) {
                score++;
            }
        }
        submitQuizProgress();
    }

    private void submitQuizProgress() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        
        if (uid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 1️⃣6️⃣ DAILY QUIZ PROGRESS (Production Schema)
        Map<String, Object> progress = new HashMap<>();
        progress.put(subject + "_lastIndex", newLastIndex);

        db.collection("userQuizProgress")
                .document(uid)
                .set(progress, com.google.firebase.firestore.SetOptions.merge());

        // Also save to scores collection for leaderboard/history (Requested Schema)
        Map<String, Object> scoreMap = new HashMap<>();
        scoreMap.put("email", email);
        scoreMap.put("quizType", "Daily: " + subject);
        scoreMap.put("score", (long) score);
        scoreMap.put("timestamp", System.currentTimeMillis());
        scoreMap.put("totalQuestions", 10L);
        scoreMap.put("uid", uid);
        scoreMap.put("userName", userName);

        db.collection("scores").add(scoreMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Quiz Completed! Score: " + score + "/10", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving progress", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}