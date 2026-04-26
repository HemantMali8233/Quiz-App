package com.example.quizapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.example.quizapp.adapters.ScoreAdapter;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.Score;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Scoreboard dashboard with filtering for ETI and Management quizzes.
 */
public class DashboardActivity extends AppCompatActivity {

    private TextView textUserName;
    private ListView listScores;
    private DatabaseHelper dbHelper;
    private int userId;
    private String userName;

    private LinearLayout filterAll, filterETI, filterManagement;
    private TextView textAllLabel, textETILabel, textMgmtLabel;
    private ImageButton buttonBack;

    private List<Score> allScores = new ArrayList<>();
    private ScoreAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);

        textUserName = findViewById(R.id.textUserName);
        listScores = findViewById(R.id.listScores);
        buttonBack = findViewById(R.id.buttonBack);

        filterAll = findViewById(R.id.filterAll);
        filterETI = findViewById(R.id.filterETI);
        filterManagement = findViewById(R.id.filterManagement);

        textAllLabel = findViewById(R.id.textAllLabel);
        textETILabel = findViewById(R.id.textETILabel);
        textMgmtLabel = findViewById(R.id.textMgmtLabel);

        userId = getIntent().getIntExtra("userId", -1);
        userName = getIntent().getStringExtra("userName");

        textUserName.setText("Hi, " + userName);

        buttonBack.setOnClickListener(v -> finish());

        filterAll.setOnClickListener(v -> updateFilter("ALL"));
        filterETI.setOnClickListener(v -> updateFilter("ETI"));
        filterManagement.setOnClickListener(v -> updateFilter("Management"));

        loadScores();
    }

    private void loadScores() {
        allScores = dbHelper.getScoresForUser(userId);
        updateFilter("ALL"); // Default to ALL
    }

    private void updateFilter(String type) {
        // Update UI visuals for filters
        resetFilters();
        if ("ALL".equals(type)) {
            filterAll.setBackgroundResource(R.drawable.filter_card_active);
            textAllLabel.setTextColor(android.graphics.Color.WHITE);
            showFilteredList(allScores);
        } else if ("ETI".equals(type)) {
            filterETI.setBackgroundResource(R.drawable.filter_card_active);
            textETILabel.setTextColor(android.graphics.Color.WHITE);
            filterList(type);
        } else if ("Management".equals(type)) {
            filterManagement.setBackgroundResource(R.drawable.filter_card_active);
            textMgmtLabel.setTextColor(android.graphics.Color.WHITE);
            filterList(type);
        }
    }

    private void resetFilters() {
        filterAll.setBackgroundResource(R.drawable.filter_card_inactive);
        filterETI.setBackgroundResource(R.drawable.filter_card_inactive);
        filterManagement.setBackgroundResource(R.drawable.filter_card_inactive);

        int purpleColor = getResources().getColor(R.color.card_purple);
        textAllLabel.setTextColor(purpleColor);
        textETILabel.setTextColor(purpleColor);
        textMgmtLabel.setTextColor(purpleColor);
    }

    private void filterList(String type) {
        List<Score> filtered = new ArrayList<>();
        for (Score s : allScores) {
            if (type.equals(s.getQuizType())) {
                filtered.add(s);
            }
        }
        showFilteredList(filtered);
    }

    private void showFilteredList(List<Score> list) {
        adapter = new ScoreAdapter(this, list);
        listScores.setAdapter(adapter);
    }
}