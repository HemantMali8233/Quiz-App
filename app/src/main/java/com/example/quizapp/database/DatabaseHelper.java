package com.example.quizapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.quizapp.models.Question;
import com.example.quizapp.models.Score;
import com.example.quizapp.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple SQLite helper for the MAD micro project.
 * Handles three tables: users, questions (for syllabus), and scores.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "quiz_app.db";
    private static final int DB_VERSION = 2; // Increment version to force schema update

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USER_NAME = "name";
    private static final String COL_USER_EMAIL = "email";
    private static final String COL_USER_PASSWORD = "password";

    // Questions table (kept for requirement, not used heavily in code)
    private static final String TABLE_QUESTIONS = "questions";
    private static final String COL_Q_ID = "id";
    private static final String COL_Q_TYPE = "quiz_type";
    private static final String COL_Q_CO = "co";
    private static final String COL_Q_TEXT = "question";
    private static final String COL_Q_OP1 = "option1";
    private static final String COL_Q_OP2 = "option2";
    private static final String COL_Q_OP3 = "option3";
    private static final String COL_Q_OP4 = "option4";
    private static final String COL_Q_CORRECT = "correct_index";

    // Scores table
    private static final String TABLE_SCORES = "scores";
    private static final String COL_S_ID = "id";
    private static final String COL_S_USER_ID = "user_id";
    private static final String COL_S_TYPE = "quiz_type";
    private static final String COL_S_SCORE = "score";
    private static final String COL_S_TOTAL = "total_questions";
    private static final String COL_S_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_NAME + " TEXT, " +
                COL_USER_EMAIL + " TEXT UNIQUE, " +
                COL_USER_PASSWORD + " TEXT)";

        String createQuestions = "CREATE TABLE " + TABLE_QUESTIONS + " (" +
                COL_Q_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_Q_TYPE + " TEXT, " +
                COL_Q_CO + " TEXT, " +
                COL_Q_TEXT + " TEXT, " +
                COL_Q_OP1 + " TEXT, " +
                COL_Q_OP2 + " TEXT, " +
                COL_Q_OP3 + " TEXT, " +
                COL_Q_OP4 + " TEXT, " +
                COL_Q_CORRECT + " INTEGER)";

        String createScores = "CREATE TABLE " + TABLE_SCORES + " (" +
                COL_S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_S_USER_ID + " INTEGER, " +
                COL_S_TYPE + " TEXT, " +
                COL_S_SCORE + " INTEGER, " +
                COL_S_TOTAL + " INTEGER, " +
                COL_S_TIMESTAMP + " TEXT)";

        db.execSQL(createUsers);
        db.execSQL(createQuestions);
        db.execSQL(createScores);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCORES);
        onCreate(db);
    }

    // User operations
    public boolean addUser(String name, String email, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_NAME, name);
        cv.put(COL_USER_EMAIL, email);
        cv.put(COL_USER_PASSWORD, password);
        long id = db.insert(TABLE_USERS, null, cv);
        return id != -1;
    }

    public User loginUser(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                COL_USER_EMAIL + "=? AND " + COL_USER_PASSWORD + "=?",
                new String[]{email, password},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME));
            String mail = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL));
            String pass = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD));
            cursor.close();
            return new User(id, name, mail, pass);
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                COL_USER_EMAIL + "=?",
                new String[]{email},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME));
            String mail = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL));
            String pass = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD));
            cursor.close();
            return new User(id, name, mail, pass);
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // Question operations
    public void insertQuestion(Question q) {
        SQLiteDatabase db = getWritableDatabase();
        insertQuestionInternal(db, q);
    }

    private void insertQuestionInternal(SQLiteDatabase db, Question q) {
        if (q == null || q.getQuestion() == null) return;

        // Check if question already exists by text and quiz_type
        Cursor cursor = db.query(TABLE_QUESTIONS, null, 
                COL_Q_TEXT + "=? AND " + COL_Q_TYPE + "=?", 
                new String[]{q.getQuestion(), q.getQuizType()}, 
                null, null, null);
        
        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            return; // Already exists, skip insertion
        }
        if (cursor != null) cursor.close();

        ContentValues cv = new ContentValues();
        cv.put(COL_Q_TYPE, q.getQuizType());
        cv.put(COL_Q_CO, q.getCo());
        cv.put(COL_Q_TEXT, q.getQuestion());
        cv.put(COL_Q_OP1, q.getOption1());
        cv.put(COL_Q_OP2, q.getOption2());
        cv.put(COL_Q_OP3, q.getOption3());
        cv.put(COL_Q_OP4, q.getOption4());
        cv.put(COL_Q_CORRECT, q.getCorrectIndex());
        db.insert(TABLE_QUESTIONS, null, cv);
    }

    public void insertQuestionsBatch(List<Question> questions) {
        if (questions == null || questions.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Question q : questions) {
                insertQuestionInternal(db, q);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Question> getQuestions(String quizType, boolean random) {
        List<Question> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_QUESTIONS,
                null,
                COL_Q_TYPE + "=?",
                new String[]{quizType},
                null,
                null,
                random ? "RANDOM()" : null // Return random or sequential questions
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_TYPE));
                String co = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_CO));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_TEXT));
                String op1 = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_OP1));
                String op2 = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_OP2));
                String op3 = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_OP3));
                String op4 = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_OP4));
                int correct = cursor.getInt(cursor.getColumnIndexOrThrow(COL_Q_CORRECT));
                list.add(new Question(type, co, text, op1, op2, op3, op4, correct));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public int getQuestionCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTIONS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // Score operations
    public void insertScore(int userId, String quizType, int score, int total, String timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_S_USER_ID, userId);
        cv.put(COL_S_TYPE, quizType);
        cv.put(COL_S_SCORE, score);
        cv.put(COL_S_TOTAL, total);
        cv.put(COL_S_TIMESTAMP, timestamp);
        db.insert(TABLE_SCORES, null, cv);
    }

    public List<Score> getScoresForUser(int userId) {
        List<Score> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Join scores with users to get the name and email
        String query = "SELECT s." + COL_S_ID + ", u." + COL_USER_NAME + ", u." + COL_USER_EMAIL + ", s." + COL_S_TYPE + ", s." + COL_S_SCORE + ", s." + COL_S_TOTAL + ", s." + COL_S_TIMESTAMP +
                " FROM " + TABLE_SCORES + " s" +
                " JOIN " + TABLE_USERS + " u ON s." + COL_S_USER_ID + " = u." + COL_USER_ID +
                " WHERE s." + COL_S_USER_ID + "=?" +
                " ORDER BY s." + COL_S_ID + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String email = cursor.getString(2);
                String type = cursor.getString(3);
                int scoreValue = cursor.getInt(4);
                int total = cursor.getInt(5);
                String ts = cursor.getString(6);
                
                Score s = new Score(id, name, type, scoreValue, total, ts);
                s.setEmail(email);
                list.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }

    public int getOverallScore(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        int total = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_S_SCORE + ") FROM " + TABLE_SCORES + " WHERE " + COL_S_USER_ID + "=?", new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getInt(0);
            cursor.close();
        }
        return total;
    }

    public int getQuizCount(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SCORES + " WHERE " + COL_S_USER_ID + "=?", new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getBestScore(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        int best = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_S_SCORE + ") FROM " + TABLE_SCORES + " WHERE " + COL_S_USER_ID + "=?", new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            best = cursor.getInt(0);
            cursor.close();
        }
        return best;
    }

    public int getBestScoreBySubject(int userId, String subject) {
        SQLiteDatabase db = getReadableDatabase();
        int best = 0;
        // Search for both the exact subject name and variations like "Daily: Subject"
        String query = "SELECT MAX(" + COL_S_SCORE + ") FROM " + TABLE_SCORES + 
                      " WHERE " + COL_S_USER_ID + "=? AND (" + COL_S_TYPE + "=? OR " + COL_S_TYPE + " LIKE ?)";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), subject, "Daily: " + subject + "%"});
        if (cursor != null && cursor.moveToFirst()) {
            best = cursor.getInt(0);
            cursor.close();
        }
        return best;
    }

    public List<Score> getLeaderboard() {
        List<Score> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Join scores with users to get the name and email, and group by email to get max score
        String query = "SELECT s." + COL_S_ID + ", u." + COL_USER_NAME + ", u." + COL_USER_EMAIL + ", s." + COL_S_TYPE + ", MAX(s." + COL_S_SCORE + ") as best_score, s." + COL_S_TOTAL + ", s." + COL_S_TIMESTAMP +
                " FROM " + TABLE_SCORES + " s" +
                " JOIN " + TABLE_USERS + " u ON s." + COL_S_USER_ID + " = u." + COL_USER_ID +
                " GROUP BY u." + COL_USER_EMAIL +
                " ORDER BY best_score DESC LIMIT 10";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String email = cursor.getString(2);
                String type = cursor.getString(3);
                int scoreValue = cursor.getInt(4);
                int total = cursor.getInt(5);
                String ts = cursor.getString(6);
                
                Score s = new Score(id, name, type, scoreValue, total, ts);
                s.setEmail(email);
                list.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }
}
