package com.example.quizapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.quizapp.R;
import com.example.quizapp.models.Score;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Modern ScoreAdapter to display scores with the new card layout.
 */
public class ScoreAdapter extends ArrayAdapter<Score> {

    public ScoreAdapter(@NonNull Context context, @NonNull List<Score> scores) {
        super(context, 0, scores);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Score score = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_score, parent, false);
        }

        TextView textTime = convertView.findViewById(R.id.textTime);
        TextView textDate = convertView.findViewById(R.id.textDate);
        TextView textQuizType = convertView.findViewById(R.id.textQuizType);
        TextView textScoreDetail = convertView.findViewById(R.id.textScoreDetail);

        if (score != null) {
            textQuizType.setText(score.getQuizType() + " Quiz");
            textScoreDetail.setText("Score: " + score.getScore() + "/" + score.getTotalQuestions());
            
            // Format timestamp (Assuming format "yyyy-MM-dd HH:mm:ss" from DB)
            String ts = score.getTimestamp();
            try {
                SimpleDateFormat sdfSource = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdfSource.parse(ts);
                
                if (date != null) {
                    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM", Locale.getDefault());
                    textTime.setText(sdfTime.format(date));
                    textDate.setText(sdfDate.format(date));
                }
            } catch (ParseException e) {
                textTime.setText("--:--");
                textDate.setText(ts); // Fallback to raw string
            }
        }

        return convertView;
    }
}