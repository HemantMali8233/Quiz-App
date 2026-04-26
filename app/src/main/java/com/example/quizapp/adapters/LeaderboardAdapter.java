package com.example.quizapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapp.R;
import com.example.quizapp.models.Score;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<Score> scores;

    public LeaderboardAdapter(List<Score> scores) {
        this.scores = scores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Score score = scores.get(position);
        // Position starts from 0, but in the list it should start from 4
        holder.textRank.setText(String.valueOf(position + 4));
        holder.textName.setText(score.getUserName());
        holder.textScore.setText(score.getScore() + " pts");
        // Default image for now
        holder.imagePlayer.setImageResource(R.drawable.ic_person);
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textRank, textName, textScore;
        CircleImageView imagePlayer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textRank = itemView.findViewById(R.id.textRank);
            textName = itemView.findViewById(R.id.textName);
            textScore = itemView.findViewById(R.id.textScore);
            imagePlayer = itemView.findViewById(R.id.imagePlayer);
        }
    }
}