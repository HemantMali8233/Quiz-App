package com.example.quizapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.example.quizapp.models.Question;
import com.example.quizapp.models.UserQuizProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class DailyQuizActivity extends AppCompatActivity {

    private RadioGroup rgSubject;
    private Spinner spinnerCO;
    private Button btnStart;
    private ProgressBar progressBar;
    private ImageView btnBack;
    
    private FirebaseFirestore db;
    private int userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_quiz);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");

        rgSubject = findViewById(R.id.rgSubject);
        spinnerCO = findViewById(R.id.spinnerCO);
        btnStart = findViewById(R.id.btnStartDailyQuiz);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        setupCOSpinner();

        btnBack.setOnClickListener(v -> finish());
        btnStart.setOnClickListener(v -> checkProgressAndStart());
    }

    private void setupCOSpinner() {
        String[] cos = {"CO1", "CO2", "CO3", "CO4", "CO5"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cos);
        spinnerCO.setAdapter(adapter);
    }

    private void checkProgressAndStart() {
        String subject = rgSubject.getCheckedRadioButtonId() == R.id.rbManagement ? "Management" : "ETI";
        String co = spinnerCO.getSelectedItem().toString();
        String uid = FirebaseAuth.getInstance().getUid();

        progressBar.setVisibility(View.VISIBLE);
        btnStart.setEnabled(false);

        if (uid == null) {
            progressBar.setVisibility(View.GONE);
            btnStart.setEnabled(true);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check user progress in Firestore
        db.collection("userQuizProgress")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Long lastIndex = doc.getLong(subject + "_lastIndex");
                            // Since we don't have a date here based on the new schema, 
                            // we'll fetch questions directly or add a date field if needed.
                            // For now, following the specific schema provided.
                            fetchNextQuestions(subject, co, lastIndex != null ? lastIndex.intValue() : 0);
                        } else {
                            // First time ever
                            fetchNextQuestions(subject, co, 0);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnStart.setEnabled(true);
                        Toast.makeText(this, "Error checking progress", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isAlreadyAttemptedToday(long lastTimestamp) {
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastTimestamp);
        
        Calendar now = Calendar.getInstance();
        
        return last.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
               last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }

    private void fetchNextQuestions(String subject, String co, int lastIndex) {
        db.collection("questions")
                .whereEqualTo("quizType", subject)
                .whereEqualTo("co", co)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnStart.setEnabled(true);
                    
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<Question> allQuestions = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            allQuestions.add(doc.toObject(Question.class));
                        }
                        
                        // Randomize the entire set
                        Collections.shuffle(allQuestions);
                        
                        // Take the first 10 (or all if fewer than 10)
                        List<Question> selectedQuestions = allQuestions.subList(0, Math.min(10, allQuestions.size()));
                        
                        startQuiz(selectedQuestions, subject, co, lastIndex + selectedQuestions.size());
                    } else {
                        Toast.makeText(this, "No questions available for " + co + " in " + subject, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startQuiz(List<Question> questions, String subject, String co, int newIndex) {
        Intent intent = new Intent(this, DailyQuizAttemptActivity.class);
        // Use a simple ArrayList to ensure serializability
        ArrayList<Question> questionList = new ArrayList<>(questions);
        intent.putExtra("questions", questionList);
        intent.putExtra("subject", subject);
        intent.putExtra("co", co);
        intent.putExtra("newIndex", newIndex);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        startActivity(intent);
        finish();
    }
}