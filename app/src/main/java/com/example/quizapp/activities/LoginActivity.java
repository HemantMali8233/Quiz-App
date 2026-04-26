package com.example.quizapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.example.quizapp.database.DatabaseHelper;
import com.example.quizapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Simple login screen using email and password.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private android.widget.TextView textGoToSignup;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textGoToSignup = findViewById(R.id.textGoToSignup);

        buttonLogin.setOnClickListener(v -> doLogin());
        textGoToSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        // 7.3 CHECK SESSION
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            handleSuccessfulLogin(user.getUid(), user.getEmail());
        }
    }

    private void doLogin() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // 7.2 LOGIN USER via Firebase Auth
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            handleSuccessfulLogin(user.getUid(), email);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSuccessfulLogin(String uid, String email) {
        // First check if user exists in local DB
        User localUser = dbHelper.getUserByEmail(email);
        if (localUser != null) {
            proceedToMain(localUser);
        } else {
            // User exists in Firebase but not in local SQLite (e.g., cleared data or new device)
            // Fetch user profile from Firestore
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            // Add to local DB to maintain consistency
                            dbHelper.addUser(name, email, "firebase_auth"); 
                            User newUser = dbHelper.getUserByEmail(email);
                            if (newUser != null) {
                                proceedToMain(newUser);
                            }
                        } else {
                            Toast.makeText(this, "User profile not found in cloud", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void proceedToMain(User user) {
        saveLoginSession(user);
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        i.putExtra("userId", user.getId());
        i.putExtra("userName", user.getName());
        startActivity(i);
        finish();
    }

    private void saveLoginSession(User user) {
        SharedPreferences prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("userId", user.getId());
        editor.putString("userName", user.getName());
        editor.putLong("lastLoginTime", System.currentTimeMillis());
        editor.apply();
    }
}
