package com.rfidresearchgroup.activities.statistics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rfidresearchgroup.database.entity.MatchEntity;
import com.rfidresearchgroup.rfidtools.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchListAdapter extends RecyclerView.Adapter<MatchListAdapter.MatchViewHolder> {

    private Context context;
    private List<MatchEntity> matches = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN);

    public MatchListAdapter(Context context) {
        this.context = context;
    }

    public void setMatches(List<MatchEntity> matches) {
        this.matches = matches;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        MatchEntity match = matches.get(position);
        
        holder.tvMatchDate.setText(dateFormat.format(new Date(match.timestamp)));
        holder.tvTeamA.setText(match.teamAName);
        holder.tvTeamB.setText(match.teamBName);
        holder.tvScore.setText(match.setsTeamA + ":" + match.setsTeamB);
        
        long minutes = match.durationMs / 60000;
        holder.tvDuration.setText("⏱ " + minutes + " Min");
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvMatchDate, tvTeamA, tvTeamB, tvScore, tvDuration;

        MatchViewHolder(View itemView) {
            super(itemView);
            tvMatchDate = itemView.findViewById(R.id.tvMatchDate);
            tvTeamA = itemView.findViewById(R.id.tvTeamA);
            tvTeamB = itemView.findViewById(R.id.tvTeamB);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
