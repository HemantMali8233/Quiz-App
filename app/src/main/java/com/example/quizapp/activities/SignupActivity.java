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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Signup screen used to create a new user account.
 */
public class SignupActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private Button buttonSignup;
    private android.widget.TextView textGoToLogin;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        buttonSignup = findViewById(R.id.buttonSignup);
        textGoToLogin = findViewById(R.id.textGoToLogin);

        buttonSignup.setOnClickListener(v -> doSignup());
        textGoToLogin.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));
    }

    private void doSignup() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();
        String confirmPass = editConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // ... [validations remain the same] ...
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            Toast.makeText(this, "Name should not contain numbers or special characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@") || !email.endsWith(".com")) {
            Toast.makeText(this, "Email must contain @ and end with .com", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 8 || pass.length() > 20) {
            Toast.makeText(this, "Password must be 8-20 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasUpper = false, hasSpecial = false, hasDigit = false;
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        for (char c : pass.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specialChars.contains(String.valueOf(c))) hasSpecial = true;
        }

        if (!hasUpper || !hasSpecial || !hasDigit) {
            Toast.makeText(this, "Password must contain at least one uppercase letter, one special character, and one number", Toast.LENGTH_LONG).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // 7.1 REGISTER USER
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase registration successful, now add to local DB
                        boolean success = dbHelper.addUser(name, email, pass);
                        if (success) {
                            // 1️⃣1️⃣ SAVE USER DATA IN FIRESTORE AFTER REGISTRATION
                            String uid = mAuth.getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("password", "hidden"); // Production requirement
                            userMap.put("points", 0);

                            if (uid != null) {
                                db.collection("users").document(uid).set(userMap);
                            }

                            User user = dbHelper.loginUser(email, pass);
                            if (user != null) {
                                saveLoginSession(user);
                                Toast.makeText(this, "Registration Success", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(SignupActivity.this, MainActivity.class);
                                i.putExtra("userId", user.getId());
                                i.putExtra("userName", user.getName());
                                startActivity(i);
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "Signup failed in local database", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If registration fails, display a message to the user.
                        Toast.makeText(SignupActivity.this, "Error " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
