package com.example.quizapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.Question;
import com.example.quizapp.models.Score;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Result screen shows score, quiz type and stores result in database.
 */
public class ResultActivity extends AppCompatActivity {

    private TextView statTotal, statCorrect, statWrong, textScorePoints;
    private ImageView buttonHome;
    private TextView tabPlayAgain, tabStandings, tabSummary;
    private LinearLayout leaderboardContainer, summaryContainer;
    private View layoutStandingsSection, layoutSummarySection, tabIndicator;

    private int userId;
    private String userName;
    private String quizType;
    private int score;
    private int totalQuestions;
    private List<Question> questions;
    private int[] userAnswers;

    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();

        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");
        quizType = getIntent().getStringExtra("quizType");
        score = getIntent().getIntExtra("score", 0);
        totalQuestions = getIntent().getIntExtra("totalQuestions", 0);
        
        // Receive questions and answers
        questions = (List<Question>) getIntent().getSerializableExtra("questions");
        userAnswers = getIntent().getIntArrayExtra("userAnswers");

        initViews();
        displayResults();
        saveScore();
        populateLeaderboard();
        populateSummary();
        setupListeners();
    }

    private void initViews() {
        statTotal = findViewById(R.id.statTotal);
        statCorrect = findViewById(R.id.statCorrect);
        statWrong = findViewById(R.id.statWrong);
        textScorePoints = findViewById(R.id.textScorePoints);
        buttonHome = findViewById(R.id.buttonHome);
        tabPlayAgain = findViewById(R.id.tabPlayAgain);
        tabStandings = findViewById(R.id.tabStandings);
        tabSummary = findViewById(R.id.tabSummary);
        leaderboardContainer = findViewById(R.id.leaderboardContainer);
        summaryContainer = findViewById(R.id.summaryContainer);
        layoutStandingsSection = findViewById(R.id.layoutStandingsSection);
        layoutSummarySection = findViewById(R.id.layoutSummarySection);
        tabIndicator = findViewById(R.id.tabIndicator);
    }

    private void displayResults() {
        int wrong = totalQuestions - score;
        statTotal.setText(String.valueOf(totalQuestions));
        statCorrect.setText(String.format(Locale.getDefault(), "%02d", score));
        statWrong.setText(String.format(Locale.getDefault(), "%02d", wrong));

        // Assuming 10 points per correct answer
        int points = score * 10;
        textScorePoints.setText("You've scored +" + points + " points");
    }

    private void saveScore() {
        String ts = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        dbHelper.insertScore(userId, quizType, score, totalQuestions, ts);

        // 1️⃣3️⃣ SAVE QUIZ SCORE TO FIRESTORE (Requested Schema)
        Map<String, Object> scoreMap = new HashMap<>();
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        
        scoreMap.put("email", email);
        scoreMap.put("quizType", quizType);
        scoreMap.put("score", (long) score); // Ensure it's a number/long
        scoreMap.put("timestamp", System.currentTimeMillis());
        scoreMap.put("totalQuestions", (long) totalQuestions); // Ensure it's a number/long
        scoreMap.put("uid", FirebaseAuth.getInstance().getUid());
        scoreMap.put("userName", userName);

        db.collection("scores").add(scoreMap);
    }
    

    private void populateLeaderboard() {
        leaderboardContainer.removeAllViews();
        List<Score> topScores = dbHelper.getLeaderboard();
        
        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (int i = 0; i < topScores.size(); i++) {
            Score s = topScores.get(i);
            View itemView = inflater.inflate(R.layout.item_leaderboard, leaderboardContainer, false);
            
            TextView textRank = itemView.findViewById(R.id.textRank);
            TextView textName = itemView.findViewById(R.id.textPlayerName);
            TextView textPercent = itemView.findViewById(R.id.textPercentage);
            
            int rank = i + 1;
            String rankStr;
            if (rank == 1) rankStr = "1st";
            else if (rank == 2) rankStr = "2nd";
            else if (rank == 3) rankStr = "3rd";
            else rankStr = rank + "th";
            
            textRank.setText(rankStr);
            textName.setText(s.getUserName());
            textPercent.setText(s.getPercentage() + "%");
            
            leaderboardContainer.addView(itemView);
        }
    }

    private void populateSummary() {
        if (questions == null || userAnswers == null) return;

        summaryContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int userAnswer = userAnswers[i];
            int correctAnswer = q.getCorrectIndex();

            View itemView = inflater.inflate(R.layout.item_summary, summaryContainer, false);

            TextView textQuestion = itemView.findViewById(R.id.textQuestion);
            TextView textUserAnswer = itemView.findViewById(R.id.textUserAnswer);
            TextView textCorrectAnswer = itemView.findViewById(R.id.textCorrectAnswer);
            ImageView imageStatus = itemView.findViewById(R.id.imageStatus);

            textQuestion.setText((i + 1) + ". " + q.getQuestion());

            String userOpt = getOptionText(q, userAnswer);
            String correctOpt = getOptionText(q, correctAnswer);

            if (userAnswer == 0) {
                textUserAnswer.setText("Your Answer: Not Attempted");
                textUserAnswer.setTextColor(getResources().getColor(android.R.color.darker_gray));
                imageStatus.setImageResource(R.drawable.ic_cross); // or a different icon for unattempted
                imageStatus.setColorFilter(getResources().getColor(android.R.color.darker_gray));
            } else if (userAnswer == correctAnswer) {
                textUserAnswer.setText("Your Answer: " + userOpt);
                textUserAnswer.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                imageStatus.setImageResource(R.drawable.ic_check); // Checkmark icon
                imageStatus.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                textUserAnswer.setText("Your Answer: " + userOpt);
                textUserAnswer.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                imageStatus.setImageResource(R.drawable.ic_cross); // X icon
                imageStatus.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
            }

            textCorrectAnswer.setText("Correct Answer: " + correctOpt);
            summaryContainer.addView(itemView);
        }
    }

    private String getOptionText(Question q, int index) {
        switch (index) {
            case 1: return q.getOption1();
            case 2: return q.getOption2();
            case 3: return q.getOption3();
            case 4: return q.getOption4();
            default: return "None";
        }
    }

    private void setupListeners() {
        buttonHome.setOnClickListener(v -> {
            Intent i = new Intent(ResultActivity.this, MainActivity.class);
            i.putExtra("userId", userId);
            i.putExtra("userName", userName);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });

        tabPlayAgain.setOnClickListener(v -> {
            Intent i = new Intent(ResultActivity.this, QuizActivity.class);
            i.putExtra("userId", userId);
            i.putExtra("userName", userName);
            i.putExtra("quizType", quizType);
            startActivity(i);
            finish();
        });

        tabStandings.setOnClickListener(v -> {
            switchTab(true);
        });

        tabSummary.setOnClickListener(v -> {
            switchTab(false);
        });
    }

    private void switchTab(boolean isStandings) {
        int purpleColor = getResources().getColor(R.color.card_purple);
        int grayColor = getResources().getColor(R.color.gray_text);

        if (isStandings) {
            layoutStandingsSection.setVisibility(View.VISIBLE);
            layoutSummarySection.setVisibility(View.GONE);
            tabStandings.setTextColor(purpleColor);
            tabSummary.setTextColor(grayColor);
            // Animate indicator
            tabIndicator.animate().translationX(0).setDuration(200);
        } else {
            layoutStandingsSection.setVisibility(View.GONE);
            layoutSummarySection.setVisibility(View.VISIBLE);
            tabStandings.setTextColor(grayColor);
            tabSummary.setTextColor(purpleColor);
            // Animate indicator - move to the middle (roughly)
            float moveX = tabSummary.getLeft();
            tabIndicator.animate().translationX(moveX).setDuration(200);
        }
    }
}
