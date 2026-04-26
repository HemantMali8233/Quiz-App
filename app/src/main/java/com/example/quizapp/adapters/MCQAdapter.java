package com.example.quizapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapp.R;
import com.example.quizapp.models.Question;
import java.util.List;

public class MCQAdapter extends RecyclerView.Adapter<MCQAdapter.ViewHolder> {

    private List<Question> questions;

    public MCQAdapter(List<Question> questions) {
        this.questions = questions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mcq_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q = questions.get(position);
        holder.textQuestion.setText((position + 1) + ". " + q.getQuestion());
        holder.textOption1.setText("A) " + q.getOption1());
        holder.textOption2.setText("B) " + q.getOption2());
        holder.textOption3.setText("C) " + q.getOption3());
        holder.textOption4.setText("D) " + q.getOption4());
        
        String correctText = "";
        switch (q.getCorrectIndex()) {
            case 1: correctText = "A"; break;
            case 2: correctText = "B"; break;
            case 3: correctText = "C"; break;
            case 4: correctText = "D"; break;
        }
        holder.textAnswer.setText("Correct Answer: " + correctText);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textQuestion, textOption1, textOption2, textOption3, textOption4, textAnswer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textQuestion = itemView.findViewById(R.id.textQuestion);
            textOption1 = itemView.findViewById(R.id.textOption1);
            textOption2 = itemView.findViewById(R.id.textOption2);
            textOption3 = itemView.findViewById(R.id.textOption3);
            textOption4 = itemView.findViewById(R.id.textOption4);
            textAnswer = itemView.findViewById(R.id.textAnswer);
        }
    }
}