package com.example.quizapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp.R;
import com.example.quizapp.adapters.MCQAdapter;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.Question;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class StudyMCQActivity extends AppCompatActivity {

    private static final String TAG = "StudyMCQActivity";
    private TabLayout tabLayout;
    private RecyclerView recyclerMCQ;
    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private ImageView btnBack, btnRefresh;
    private Spinner spinnerFilterCO;
    private TextView textResultCount;
    private ProgressBar progressBar;

    private List<Question> allCloudQuestions = new ArrayList<>();
    private String currentSubject = "ETI";
    private String currentCOFilter = "All COs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_mcq);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        
        initViews();
        setupTabs();
        setupFilter();

        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> fetchAllQuestionsFromCloud());
        
        // Load everything once from cloud
        fetchAllQuestionsFromCloud();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayoutMCQ);
        recyclerMCQ = findViewById(R.id.recyclerMCQ);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        spinnerFilterCO = findViewById(R.id.spinnerFilterCO);
        textResultCount = findViewById(R.id.textResultCount);
        progressBar = findViewById(R.id.progressBar);

        if (recyclerMCQ != null) {
            recyclerMCQ.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupTabs() {
        if (tabLayout == null) return;
        
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("ETI"));
        tabLayout.addTab(tabLayout.newTab().setText("Management"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentSubject = tab.getPosition() == 0 ? "ETI" : "Management";
                Log.d(TAG, "Tab switched to: " + currentSubject);
                applyFilterAndRefresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFilter() {
        if (spinnerFilterCO == null) return;
        
        String[] coFilters = {"All COs", "CO1", "CO2", "CO3", "CO4", "CO5"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, coFilters);
        spinnerFilterCO.setAdapter(adapter);

        spinnerFilterCO.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCOFilter = coFilters[position];
                Log.d(TAG, "Filter changed to: " + currentCOFilter);
                applyFilterAndRefresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchAllQuestionsFromCloud() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Fetching ALL questions from cloud...");
        
        db.collection("questions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    List<Question> tempList = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                Question q = doc.toObject(Question.class);
                                if (q != null && q.getQuestion() != null) {
                                    tempList.add(q);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing doc: " + doc.getId(), e);
                            }
                        }
                    }

                    if (!tempList.isEmpty()) {
                        Log.d(TAG, "Successfully loaded " + tempList.size() + " total questions from Cloud");
                        allCloudQuestions = tempList;
                        applyFilterAndRefresh();
                    } else {
                        Log.d(TAG, "Cloud collection is empty, using local DB");
                        loadFromLocalDB();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Cloud fetch failed, using local DB", e);
                    loadFromLocalDB();
                });
    }

    private void loadFromLocalDB() {
        // As a last resort, fetch from local SQLite
        List<Question> etiLocal = dbHelper.getQuestions("ETI", false);
        List<Question> mgmtLocal = dbHelper.getQuestions("Management", false);
        
        allCloudQuestions.clear();
        allCloudQuestions.addAll(etiLocal);
        allCloudQuestions.addAll(mgmtLocal);
        
        Log.d(TAG, "Loaded " + allCloudQuestions.size() + " total questions from Local DB");
        applyFilterAndRefresh();
    }

    private void applyFilterAndRefresh() {
        List<Question> filteredList = new ArrayList<>();
        
        for (Question q : allCloudQuestions) {
            if (q == null) continue;
            
            // 1. Filter by Subject (Case-Insensitive for safety)
            boolean subjectMatch = q.getQuizType() != null && q.getQuizType().equalsIgnoreCase(currentSubject);
            
            if (subjectMatch) {
                // 2. Filter by CO
                if ("All COs".equals(currentCOFilter)) {
                    filteredList.add(q);
                } else if (currentCOFilter.equalsIgnoreCase(q.getCo())) {
                    filteredList.add(q);
                }
            }
        }
        
        Log.d(TAG, "Filtered list size: " + filteredList.size() + " for " + currentSubject + " (" + currentCOFilter + ")");
        updateUI(filteredList);
    }

    private void updateUI(List<Question> filteredList) {
        if (recyclerMCQ == null) return;
        
        MCQAdapter adapter = new MCQAdapter(filteredList);
        recyclerMCQ.setAdapter(adapter);

        if (textResultCount != null) {
            String countText = filteredList.size() + " " + currentSubject + " Questions";
            if (!"All COs".equals(currentCOFilter)) {
                countText += " for " + currentCOFilter;
            }
            textResultCount.setText(countText);
        }
        
        if (filteredList.isEmpty() && allCloudQuestions.isEmpty()) {
            Toast.makeText(this, "No data available. Please check internet or refresh.", Toast.LENGTH_LONG).show();
        }
    }
}