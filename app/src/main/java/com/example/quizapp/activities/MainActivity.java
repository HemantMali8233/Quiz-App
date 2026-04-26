package com.example.quizapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.quizapp.R;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.Score;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textGreeting, textUserName, textUserPoints, textQuote;
    private TextView textKingName, textKingSubTitle, textKingQuizType, textKingMarks, textKingDate, textKingTime, textKingDuration;
    private CardView cardJoinQuiz, cardLeaderboard, cardStudyMCQ, cardDailyQuiz;
    private Button btnJoinDailyQuiz;
    private BottomNavigationView bottomNavigation;
    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");

        initViews();
        setupUserHeader();
        setupWeeklyKing();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        textGreeting = findViewById(R.id.textGreeting);
        textUserName = findViewById(R.id.textUserName);
        textUserPoints = findViewById(R.id.textUserPoints);

        textKingName = findViewById(R.id.textKingName);
        textKingSubTitle = findViewById(R.id.textKingSubTitle);
        textKingQuizType = findViewById(R.id.textKingQuizType);
        textKingMarks = findViewById(R.id.textKingMarks);
        textKingDate = findViewById(R.id.textKingDate);
        textKingTime = findViewById(R.id.textKingTime);
        textKingDuration = findViewById(R.id.textKingDuration);
        textQuote = findViewById(R.id.textQuote);

        cardJoinQuiz = findViewById(R.id.cardJoinQuiz);
        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        cardStudyMCQ = findViewById(R.id.cardStudyMCQ);
        cardDailyQuiz = findViewById(R.id.cardDailyQuiz);
        btnJoinDailyQuiz = findViewById(R.id.btnJoinDailyQuiz);
        
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupUserHeader() {
        textGreeting.setText("Welcome back");
        textUserName.setText(userName != null ? userName : "User");
        
        int overallScore = dbHelper.getOverallScore(userId);
        textUserPoints.setText(String.valueOf(overallScore));
    }

    private void setupWeeklyKing() {
        // 1️⃣5️⃣ FETCH CURRENT USER'S PERSONAL BEST FROM FIRESTORE
        String currentEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : null;
        
        if (currentEmail == null) {
            loadKingFromLocalDB();
            return;
        }

        // Set descriptive label for the container
        textQuote.setText("Your Best: Overall Career Performance");

        db.collection("scores")
                .whereEqualTo("email", currentEmail)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot kingDoc = queryDocumentSnapshots.getDocuments().get(0);
                        if (kingDoc != null) {
                            Long scoreVal = kingDoc.getLong("score");
                            String quizType = kingDoc.getString("quizType");
                            Long total = kingDoc.getLong("totalQuestions");
                            Object timestampObj = kingDoc.get("timestamp");

                            if (textKingName != null) textKingName.setText(userName != null ? userName : "You");
                            if (textKingSubTitle != null) textKingSubTitle.setText("Personal Best: " + (scoreVal != null ? scoreVal : 0) + " Points");
                            if (textKingQuizType != null) textKingQuizType.setText("Quiz: " + (quizType != null ? quizType : "--"));
                            if (textKingMarks != null) textKingMarks.setText("Marks: " + (scoreVal != null ? scoreVal : 0) + " / " + (total != null ? total : 0));
                            
                            if (timestampObj != null) {
                                android.util.Log.d("MainActivity", "Firestore timestamp found: " + timestampObj.toString() + " type: " + timestampObj.getClass().getSimpleName());
                                long ts = 0;
                                if (timestampObj instanceof Long) ts = (Long) timestampObj;
                                else if (timestampObj instanceof String) {
                                    try { ts = Long.parseLong((String) timestampObj); } catch (Exception e) {}
                                }
                                
                                if (ts > 0) {
                                    String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date(ts));
                                    String timeStr = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date(ts));
                                    android.util.Log.d("MainActivity", "Formatted Cloud Date: " + dateStr + " Time: " + timeStr);
                                    if (textKingDate != null) textKingDate.setText("Date: " + dateStr);
                                    if (textKingTime != null) textKingTime.setText("Time: " + timeStr);
                                }
                            }
                            
                            // Set accurate duration based on quiz marks if not explicitly stored
                            String duration = "20 mins";
                            if (total != null) {
                                if (total >= 70) duration = "90 mins";
                                else if (total >= 50) duration = "60 mins";
                                else if (total >= 30) duration = "45 mins";
                            }
                            if (textKingDuration != null) textKingDuration.setText("Duration: " + duration);
                        }
                    } else {
                        // Fallback to SQLite if Firestore is empty
                        loadKingFromLocalDB();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to SQLite on error
                    loadKingFromLocalDB();
                });
    }

    private void loadKingFromLocalDB() {
        android.util.Log.d("MainActivity", "Loading data from Local DB");
        // Fetch personal best for the current logged-in user from local DB
        List<Score> scores = dbHelper.getScoresForUser(userId);
        
        // Descriptive label for the container even on fallback
        if (textQuote != null) textQuote.setText("Your Best: Overall Career Performance");

        if (scores != null && !scores.isEmpty()) {
            // scores is already ordered by ID DESC, let's find the max score manually or fetch sorted
            Score personalBest = scores.get(0);
            for (Score s : scores) {
                if (s.getScore() > personalBest.getScore()) {
                    personalBest = s;
                }
            }

            textKingName.setText(userName != null ? userName : "You");
            textKingSubTitle.setText("Personal Best: " + personalBest.getScore() + " Points");
            textKingQuizType.setText("Quiz: " + personalBest.getQuizType());
            textKingMarks.setText("Marks: " + personalBest.getScore() + " / " + personalBest.getTotalQuestions());
            
            String ts = personalBest.getTimestamp();
            android.util.Log.d("MainActivity", "Local timestamp raw: " + ts);
            if (ts != null && ts.contains(" ")) {
                // If it's the old SQLite format "yyyy-MM-dd HH:mm:ss" or "dd MMM yyyy HH:mm"
                try {
                    // Try parsing multiple common formats
                    java.util.Date date = null;
                    String[] formats = {"yyyy-MM-dd HH:mm:ss", "dd MMM yyyy HH:mm", "dd MMM yyyy HH:mm:ss"};
                    
                    for (String fmt : formats) {
                        try {
                            java.text.SimpleDateFormat sdfSource = new java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault());
                            date = sdfSource.parse(ts);
                            if (date != null) break;
                        } catch (Exception ignored) {}
                    }

                    if (date != null) {
                        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date);
                        String timeStr = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(date);
                        android.util.Log.d("MainActivity", "Formatted Local Date: " + dateStr + " Time: " + timeStr);
                        textKingDate.setText("Date: " + dateStr);
                        textKingTime.setText("Time: " + timeStr);
                    } else {
                        // Manual split if parsing fails
                        String[] parts = ts.split(" ");
                        textKingDate.setText("Date: " + parts[0]);
                        textKingTime.setText("Time: " + (parts.length > 1 ? parts[1] : "--"));
                    }
                } catch (Exception e) {
                    textKingDate.setText("Date: " + ts);
                }
            } else if (ts != null) {
                // If it's just a timestamp string (numeric)
                try {
                    long t = Long.parseLong(ts);
                    String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date(t));
                    String timeStr = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date(t));
                    android.util.Log.d("MainActivity", "Formatted Local (Numeric) Date: " + dateStr + " Time: " + timeStr);
                    textKingDate.setText("Date: " + dateStr);
                    textKingTime.setText("Time: " + timeStr);
                } catch (Exception e) {
                    textKingDate.setText("Date: " + ts);
                }
            }
            
            long total = personalBest.getTotalQuestions();
            String duration = "20 mins";
            if (total >= 70) duration = "90 mins";
            else if (total >= 50) duration = "60 mins";
            else if (total >= 30) duration = "45 mins";
            
            textKingDuration.setText("Duration: " + duration);
        } else {
            // Default values if no scores exist
            textKingName.setText("No Records Yet");
            textKingSubTitle.setText("Start your first quiz!");
            textKingQuizType.setText("Quiz: --");
            textKingMarks.setText("Marks: --");
            textKingDate.setText("Date: --");
            textKingTime.setText("Time: --");
            textKingDuration.setText("Duration: --");
        }
    }

    private void setupClickListeners() {
        cardJoinQuiz.setOnClickListener(v -> showJoinQuizDialog());
        cardLeaderboard.setOnClickListener(v -> {
            Intent i = new Intent(this, LeaderboardActivity.class);
            i.putExtra("userId", userId);
            i.putExtra("userName", userName);
            startActivity(i);
        });
        cardStudyMCQ.setOnClickListener(v -> startActivity(new Intent(this, StudyMCQActivity.class)));
        
        View.OnClickListener dailyQuizClick = v -> {
            Intent i = new Intent(this, DailyQuizActivity.class);
            i.putExtra("userId", userId);
            i.putExtra("userName", userName);
            startActivity(i);
        };
        cardDailyQuiz.setOnClickListener(dailyQuizClick);
        btnJoinDailyQuiz.setOnClickListener(dailyQuizClick);
    }

    private void showJoinQuizDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_join_quiz, null);
        builder.setView(view);

        RadioGroup rgCategory = view.findViewById(R.id.radioGroupCategory);
        RadioGroup rgMarks = view.findViewById(R.id.radioGroupMarks);
        Button btnStart = view.findViewById(R.id.btnSubmitJoin);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnStart.setOnClickListener(v -> {
            String type = rgCategory.getCheckedRadioButtonId() == R.id.rbETI ? "ETI" : "Management";
            
            int marks = 15;
            int duration = 20;

            int selectedMarksId = rgMarks.getCheckedRadioButtonId();
            if (selectedMarksId == R.id.rb30) {
                marks = 30;
                duration = 45;
            } else if (selectedMarksId == R.id.rb50) {
                marks = 50;
                duration = 60;
            } else if (selectedMarksId == R.id.rb70) {
                marks = 70;
                duration = 90;
            }

            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userName", userName);
            intent.putExtra("quizType", type);
            intent.putExtra("quizDuration", duration);
            intent.putExtra("quizMarks", marks);
            startActivity(intent);
            dialog.dismiss();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) {
                Intent i = new Intent(this, DashboardActivity.class);
                i.putExtra("userId", userId);
                i.putExtra("userName", userName);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_leaderboard) {
                Intent i = new Intent(this, LeaderboardActivity.class);
                i.putExtra("userId", userId);
                i.putExtra("userName", userName);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_join) {
                showJoinQuizDialog();
                return false; // Don't highlight "Join" as a persistent page
            } else if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.putExtra("userId", userId);
                i.putExtra("userName", userName);
                startActivity(i);
                return true;
            }
            return id == R.id.nav_home;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupUserHeader();
        setupWeeklyKing();
    }
}