package com.example.quizapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp.R;
import com.example.quizapp.adapters.LeaderboardAdapter;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.Score;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerLeaderboard;
    private ImageView btnBack;
    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;

    // Top 3 UI
    private CircleImageView imgFirstPlace, imgSecondPlace, imgThirdPlace;
    private TextView textFirstName, textFirstScore, textSecondName, textSecondScore, textThirdName, textThirdScore;
    private android.view.View layoutFirstPlace, layoutSecondPlace, layoutThirdPlace;
    private android.widget.ProgressBar progressBar;
    private TextView textNoData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        btnBack.setOnClickListener(v -> finish());

        loadLeaderboardData();
    }

    private void initViews() {
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard);
        btnBack = findViewById(R.id.btnBack);

        // Top 3
        imgFirstPlace = findViewById(R.id.imgFirstPlace);
        textFirstName = findViewById(R.id.textFirstName);
        textFirstScore = findViewById(R.id.textFirstScore);
        imgSecondPlace = findViewById(R.id.imgSecondPlace);
        textSecondName = findViewById(R.id.textSecondName);
        textSecondScore = findViewById(R.id.textSecondScore);
        imgThirdPlace = findViewById(R.id.imgThirdPlace);
        textThirdName = findViewById(R.id.textThirdName);
        textThirdScore = findViewById(R.id.textThirdScore);
        
        layoutFirstPlace = findViewById(R.id.layoutFirstPlace);
        layoutSecondPlace = findViewById(R.id.layoutSecondPlace);
        layoutThirdPlace = findViewById(R.id.layoutThirdPlace);
        
        progressBar = findViewById(R.id.progressBar);
        textNoData = findViewById(R.id.textNoData);
    }

    private void loadLeaderboardData() {
        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);
        if (textNoData != null) textNoData.setVisibility(android.view.View.GONE);

        // Fetch all scores from Firestore
        db.collection("scores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                    
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        setupLeaderboard(dbHelper.getLeaderboard());
                        return;
                    }

                    // 1. Aggregate best score for each user locally
                    Map<String, Score> userBestScores = new HashMap<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            Score score = doc.toObject(Score.class);
                            if (score == null) continue;

                            // Determine a grouping key: prefer UID, then Email, then UserName
                            String groupKey = score.getUid();
                            if (groupKey == null || groupKey.isEmpty()) groupKey = score.getEmail();
                            if (groupKey == null || groupKey.isEmpty()) groupKey = score.getUserName();
                            
                            // Skip if no identifying information
                            if (groupKey == null || groupKey.isEmpty()) {
                                Log.w("Leaderboard", "Skipping doc " + doc.getId() + " because no identity found");
                                continue;
                            }

                            // Keep the highest score for this user
                            if (!userBestScores.containsKey(groupKey) || score.getScore() > userBestScores.get(groupKey).getScore()) {
                                userBestScores.put(groupKey, score);
                            }
                        } catch (Exception e) {
                            Log.e("Leaderboard", "Error parsing score doc: " + doc.getId(), e);
                        }
                    }

                    // 2. Sort the unique user scores in descending order
                    List<Score> sortedLeaderboard = new ArrayList<>(userBestScores.values());
                    Collections.sort(sortedLeaderboard, (s1, s2) -> {
                        // First sort by score DESC
                        int scoreCompare = Long.compare(s2.getScore(), s1.getScore());
                        if (scoreCompare != 0) return scoreCompare;
                        
                        // If scores are same, sort by timestamp ASC (earlier achiever gets higher rank)
                        long t1 = 0, t2 = 0;
                        try {
                            t1 = Long.parseLong(s1.getTimestamp());
                            t2 = Long.parseLong(s2.getTimestamp());
                        } catch (Exception e) {}
                        
                        return Long.compare(t1, t2);
                    });

                    setupLeaderboard(sortedLeaderboard);
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                    setupLeaderboard(dbHelper.getLeaderboard());
                });
    }

    private void setupLeaderboard(List<Score> leaderboardData) {
        if (leaderboardData == null || leaderboardData.isEmpty()) {
            if (layoutFirstPlace != null) layoutFirstPlace.setVisibility(android.view.View.INVISIBLE);
            if (layoutSecondPlace != null) layoutSecondPlace.setVisibility(android.view.View.INVISIBLE);
            if (layoutThirdPlace != null) layoutThirdPlace.setVisibility(android.view.View.INVISIBLE);
            if (textNoData != null) textNoData.setVisibility(android.view.View.VISIBLE);
            
            // Clear recycler
            recyclerLeaderboard.setAdapter(new LeaderboardAdapter(new ArrayList<>()));
            return;
        }

        if (textNoData != null) textNoData.setVisibility(android.view.View.GONE);

        // Populate Top 3
        if (leaderboardData.size() > 0) {
            if (layoutFirstPlace != null) layoutFirstPlace.setVisibility(android.view.View.VISIBLE);
            Score first = leaderboardData.get(0);
            if (textFirstName != null) textFirstName.setText(first.getUserName());
            if (textFirstScore != null) textFirstScore.setText(String.valueOf(first.getScore()));
        } else if (layoutFirstPlace != null) {
            layoutFirstPlace.setVisibility(android.view.View.INVISIBLE);
        }

        if (leaderboardData.size() > 1) {
            if (layoutSecondPlace != null) layoutSecondPlace.setVisibility(android.view.View.VISIBLE);
            Score second = leaderboardData.get(1);
            if (textSecondName != null) textSecondName.setText(second.getUserName());
            if (textSecondScore != null) textSecondScore.setText(String.valueOf(second.getScore()));
        } else if (layoutSecondPlace != null) {
            layoutSecondPlace.setVisibility(android.view.View.INVISIBLE);
        }

        if (leaderboardData.size() > 2) {
            if (layoutThirdPlace != null) layoutThirdPlace.setVisibility(android.view.View.VISIBLE);
            Score third = leaderboardData.get(2);
            if (textThirdName != null) textThirdName.setText(third.getUserName());
            if (textThirdScore != null) textThirdScore.setText(String.valueOf(third.getScore()));
        } else if (layoutThirdPlace != null) {
            layoutThirdPlace.setVisibility(android.view.View.INVISIBLE);
        }

        // Populate the rest of the list (4th place onwards)
        List<Score> restOfList = new ArrayList<>();
        if (leaderboardData.size() > 3) {
            restOfList = leaderboardData.subList(3, leaderboardData.size());
        }

        LeaderboardAdapter adapter = new LeaderboardAdapter(restOfList);
        recyclerLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        recyclerLeaderboard.setAdapter(adapter);
    }
}