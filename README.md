# QuizHub - Comprehensive Study & Testing Platform

QuizHub is a modern, high-performance Android application designed for Semester 6 students to master subjects like **ETI (Emerging Trends in IT)** and **Management**. The app features a hybrid data architecture using **Firebase Firestore** for real-time global features and **SQLite** for reliable offline persistence.

## 🚀 Key Features

### 1. **Authentication & User Profiles**
- Secure login and signup via **Firebase Authentication**.
- Dynamic user profiles that sync between cloud and local storage.
- Detailed career stats, including total points and subject-specific best scores.

### 2. **Join Quiz (Core Testing)**
- **Custom Configuration Window**: Select subject and marking scheme (15, 30, or 50 marks).
- **Balanced Questions**: Automatically fetches and balances questions across all Course Outcomes (CO1-CO5).
- **Real-time Sync**: Scores are instantly synced to the global leaderboard.

### 3. **Study MCQ (Learning Hub)**
- **Tabbed Interface**: Seamlessly switch between ETI and Management subjects.
- **Smart Filtering**: Filter questions by Course Outcome (CO1-CO5) for targeted preparation.
- **Zero Lag**: Uses a "Fetch-All-Filter-Local" strategy for instantaneous subject and filter switching.

### 4. **Daily Quiz (Progression Mode)**
- **Professional Navigation**: Features a bi-directional navigation bar (Previous & Next).
- **Answer Persistence**: Remembers user selections if they navigate back to previous questions.
- **Dynamic Submission**: The navigation automatically adapts to a "Submit" button on the final question.

### 5. **Global Leaderboard**
- **Visual Podium**: High-visibility Top 3 ranking with Gold, Silver, and Bronze badges.
- **Fair Ranking**: Implements tie-breaker logic based on achievement timestamps (earlier achievers rank higher).
- **Unique User Aggregation**: Only the best career score of each unique user is displayed.

## 🛠️ Technical Stack

- **Language**: Java
- **Database**: 
  - **Firebase Firestore**: Primary source for global rankings and question banks.
  - **SQLite**: Local persistence for offline access and instant UI loading.
- **Backend**: Firebase Auth (User Management).
- **UI/UX**: Material Design 3, custom gradients, vector icons, and `welcome_bg` recurring theme.

## 📂 Project Structure

- `activities/`: Contains all screen logic (Home, Quiz, Study, Leaderboard, etc.).
- `adapters/`: Manages list rendering for MCQs, Scores, and Leaderboard rankings.
- `models/`: Java objects representing core data entities (Question, Score, User).
- `database/`: `DatabaseHelper.java` manages all local SQLite operations.
- `res/`: Modern XML layouts, vibrant gradients, and specialized header drawables.

## 🔒 Security & Optimization
- Confidential files like `google-services.json` and `local.properties` are protected via `.gitignore`.
- Advanced null safety and error handling prevent crashes during cloud fetching.
- Formatted timestamps (`DD/MM/YYYY hh:mm:ss AM/PM`) for professional data presentation.

---
*Developed for Semester 6 Mobile Application Development Micro-Project.*