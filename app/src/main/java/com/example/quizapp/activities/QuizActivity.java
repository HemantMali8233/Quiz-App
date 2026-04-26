package com.example.quizapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.Question;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class QuizActivity extends AppCompatActivity {

    private TextView textQuestionCounter, textQuestion, textTimer, textQuestionLabel, textQuizType, textPoints;
    private TextView buttonOption1, buttonOption2, buttonOption3, buttonOption4;
    private View layoutOption1, layoutOption2, layoutOption3, layoutOption4;
    private Button buttonPrevious, buttonNext, buttonSubmit;
    private ProgressBar quizProgressBar;
    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private final List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int[] userAnswers;
    private int userId;
    private String userName;
    private String quizType;

    private int totalQuestions = 30;
    private CountDownTimer timer;
    private long timeLeftMs;
    private long initialTimeMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();

        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");
        
        // Read data passed from MainActivity dialog
        quizType = getIntent().getStringExtra("quizType");
        totalQuestions = getIntent().getIntExtra("quizMarks", 30);
        timeLeftMs = getIntent().getIntExtra("quizDuration", 45) * 60L * 1000L;
        initialTimeMs = timeLeftMs;

        textQuestionCounter = findViewById(R.id.textQuestionCounter);
        textQuestion = findViewById(R.id.textQuestion);
        textTimer = findViewById(R.id.textRemainingTime);
        textQuestionLabel = findViewById(R.id.textQuestionLabel);
        textQuizType = findViewById(R.id.textQuizType);
        textPoints = findViewById(R.id.textPoints);
        
        buttonOption1 = findViewById(R.id.buttonOption1);
        buttonOption2 = findViewById(R.id.buttonOption2);
        buttonOption3 = findViewById(R.id.buttonOption3);
        buttonOption4 = findViewById(R.id.buttonOption4);
        
        layoutOption1 = findViewById(R.id.layoutOption1);
        layoutOption2 = findViewById(R.id.layoutOption2);
        layoutOption3 = findViewById(R.id.layoutOption3);
        layoutOption4 = findViewById(R.id.layoutOption4);

        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonNext = findViewById(R.id.buttonNext);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        
        quizProgressBar = findViewById(R.id.quizProgressBar);

        // Start quiz directly with passed data
        startQuizFlow();

        layoutOption1.setOnClickListener(v -> selectAnswer(1));
        layoutOption2.setOnClickListener(v -> selectAnswer(2));
        layoutOption3.setOnClickListener(v -> selectAnswer(3));
        layoutOption4.setOnClickListener(v -> selectAnswer(4));

        buttonPrevious.setOnClickListener(v -> goPrevious());
        buttonNext.setOnClickListener(v -> goNext());
        buttonSubmit.setOnClickListener(v -> finishQuiz());
    }

    private void balanceQuestionsByCO(List<Question> allQuestions, int limit) {
        if (allQuestions == null || allQuestions.isEmpty()) {
            questions.clear();
            return;
        }
        
        Map<String, List<Question>> coMap = new HashMap<>();
        for (Question q : allQuestions) {
            if (q == null) continue;
            String co = q.getCo() != null ? q.getCo() : "CO1";
            if (!coMap.containsKey(co)) coMap.put(co, new ArrayList<>());
            coMap.get(co).add(q);
        }
        
        List<String> cos = new ArrayList<>(coMap.keySet());
        Collections.sort(cos); 
        
        int cosCount = cos.size();
        if (cosCount == 0) {
            questions.clear();
            questions.addAll(allQuestions);
            Collections.shuffle(questions);
            if (questions.size() > limit) {
                questions.subList(limit, questions.size()).clear();
            }
            return;
        }
        
        int perCo = limit / cosCount;
        if (perCo == 0 && limit > 0) perCo = 1; // Ensure at least some questions if limit > 0
        int extra = limit % cosCount;
        
        List<Question> balancedList = new ArrayList<>();
        for (int i = 0; i < cosCount; i++) {
            List<Question> coQuestions = coMap.get(cos.get(i));
            if (coQuestions == null || coQuestions.isEmpty()) continue;
            
            Collections.shuffle(coQuestions);
            int countToTake = perCo + (i < extra ? 1 : 0);
            if (balancedList.size() < limit) {
                int take = Math.min(countToTake, coQuestions.size());
                balancedList.addAll(coQuestions.subList(0, take));
            }
        }
        
        if (balancedList.size() < limit) {
            List<Question> remaining = new ArrayList<>(allQuestions);
            remaining.removeAll(balancedList);
            Collections.shuffle(remaining);
            int needed = limit - balancedList.size();
            if (needed > 0 && !remaining.isEmpty()) {
                balancedList.addAll(remaining.subList(0, Math.min(needed, remaining.size())));
            }
        }
        
        Collections.shuffle(balancedList);
        questions.clear();
        questions.addAll(balancedList);
    }

    private void startQuizFlow() {
        if (quizType == null) {
            Toast.makeText(this, "Error: Quiz type missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Toast.makeText(this, "Loading " + quizType + " quiz...", Toast.LENGTH_SHORT).show();
        loadQuestionsFromFirestore(quizType);
    }

    private void setupQuizData() {
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "No questions available for this quiz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        userAnswers = new int[questions.size()];
        textQuizType.setText(quizType + " Quiz");
        quizProgressBar.setMax(questions.size());
        currentQuestionIndex = 0; 
        showQuestion();
        startTimer();
    }

    private void loadQuestionsFromFirestore(String type) {
        android.util.Log.d("QuizActivity", "Fetching questions for: " + type);
        db.collection("questions")
                .whereEqualTo("quizType", type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        android.util.Log.d("QuizActivity", "Found " + queryDocumentSnapshots.size() + " documents");
                        List<Question> allFromFirestore = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                Question q = doc.toObject(Question.class);
                                if (q != null && q.getQuestion() != null) {
                                    allFromFirestore.add(q);
                                } else {
                                    android.util.Log.w("QuizActivity", "Question or body is null for doc: " + doc.getId());
                                }
                            } catch (Exception e) {
                                android.util.Log.e("QuizActivity", "Error parsing doc: " + doc.getId(), e);
                            }
                        }
                        
                        if (!allFromFirestore.isEmpty()) {
                            android.util.Log.d("QuizActivity", "Successfully parsed " + allFromFirestore.size() + " questions");
                            balanceQuestionsByCO(allFromFirestore, totalQuestions);
                            setupQuizData();
                        } else {
                            android.util.Log.w("QuizActivity", "No questions parsed, falling back to local");
                            fallbackToLocal(type);
                        }
                    } else {
                        android.util.Log.w("QuizActivity", "Query snapshots empty or null, falling back to local");
                        fallbackToLocal(type);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("QuizActivity", "Firestore fetch failed", e);
                    fallbackToLocal(type);
                });
    }

    private void fallbackToLocal(String type) {
        loadQuestionsForQuiz(type);
        setupQuizData();
    }

    private void goPrevious() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            showQuestion();
        }
    }

    private void goNext() {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            showQuestion();
        }
    }

    private void selectAnswer(int option) {
        userAnswers[currentQuestionIndex] = option;
        highlightSelectedOption(option);
    }

    private void highlightSelectedOption(int option) {
        resetOptionStyles();
        switch (option) {
            case 1: 
                layoutOption1.setSelected(true); 
                break;
            case 2: 
                layoutOption2.setSelected(true); 
                break;
            case 3: 
                layoutOption3.setSelected(true); 
                break;
            case 4: 
                layoutOption4.setSelected(true); 
                break;
        }
    }

    private void showQuestion() {
        if (currentQuestionIndex >= questions.size()) return;

        Question q = questions.get(currentQuestionIndex);
        textQuestionLabel.setText("Question " + String.format(Locale.getDefault(), "%02d", currentQuestionIndex + 1));
        textQuestionCounter.setText((currentQuestionIndex + 1) + " of " + questions.size());
        textPoints.setText((currentQuestionIndex + 1) + " of " + questions.size());
        quizProgressBar.setProgress(currentQuestionIndex + 1);
        
        textQuestion.setText(q.getQuestion());
        buttonOption1.setText(q.getOption1());
        buttonOption2.setText(q.getOption2());
        buttonOption3.setText(q.getOption3());
        buttonOption4.setText(q.getOption4());

        resetOptionStyles();

        // Restore previous answer if any
        if (userAnswers[currentQuestionIndex] != 0) {
            highlightSelectedOption(userAnswers[currentQuestionIndex]);
        }

        // Navigation button logic
        buttonPrevious.setEnabled(currentQuestionIndex > 0);
        buttonPrevious.setAlpha(currentQuestionIndex > 0 ? 1.0f : 0.5f);

        if (currentQuestionIndex == questions.size() - 1) {
            buttonNext.setVisibility(View.GONE);
            buttonSubmit.setText("Submit");
        } else {
            buttonNext.setVisibility(View.VISIBLE);
            buttonSubmit.setText("Finish");
        }
    }

    private void resetOptionStyles() {
        layoutOption1.setSelected(false);
        layoutOption2.setSelected(false);
        layoutOption3.setSelected(false);
        layoutOption4.setSelected(false);
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        
        timer = new CountDownTimer(timeLeftMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMs = millisUntilFinished;
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                timeLeftMs = 0;
                updateTimerUI();
                Toast.makeText(QuizActivity.this, "Time is over! Submitting.", Toast.LENGTH_SHORT).show();
                finishQuiz();
            }
        }.start();
    }

    private void updateTimerUI() {
        long seconds = timeLeftMs / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        textTimer.setText(String.format(Locale.getDefault(), "Remaining time : %02d:%02d", minutes, secs));
    }

    private void finishQuiz() {
        if (timer != null) {
            timer.cancel();
        }

        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (userAnswers[i] == q.getCorrectIndex()) {
                correct++;
            }
        }

        Intent i = new Intent(QuizActivity.this, ResultActivity.class);
        i.putExtra("userId", userId);
        i.putExtra("userName", userName);
        i.putExtra("quizType", quizType);
        i.putExtra("score", correct);
        i.putExtra("totalQuestions", questions.size());
        
        // Passing the questions and user answers for summary
        i.putExtra("questions", (ArrayList<Question>) questions);
        i.putExtra("userAnswers", userAnswers);
        
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }

    private void loadQuestionsForQuiz(String type) {
        questions.clear();

        List<Question> dbQuestions = dbHelper.getQuestions(type, false); // Get from local SQLite

        if (dbQuestions != null && !dbQuestions.isEmpty()) {
            balanceQuestionsByCO(dbQuestions, totalQuestions);
        } else {
            // Fallback in case no questions were loaded
            questions.add(new Question(type, "CO1", "No questions found for " + type + ". Please check your internet connection.", "N/A", "N/A", "N/A", "N/A", 1));
        }
    }
}
