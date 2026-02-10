package com.rfidresearchgroup.activities.statistics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rfidresearchgroup.database.entity.PlayerEntity;
import com.rfidresearchgroup.rfidtools.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.PlayerViewHolder> {

    private Context context;
    private List<PlayerEntity> players = new ArrayList<>();

    public PlayerListAdapter(Context context) {
        this.context = context;
    }

    public void setPlayers(List<PlayerEntity> players) {
        this.players = players;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        PlayerEntity player = players.get(position);
        
        holder.tvPlayerName.setText(player.name);
        holder.tvMatchesPlayed.setText(String.valueOf(player.matchesPlayed));
        holder.tvMatchesWon.setText(String.valueOf(player.matchesWon));
        
        double winRate = player.matchesPlayed > 0 
            ? (player.matchesWon * 100.0 / player.matchesPlayed) 
            : 0.0;
        holder.tvWinRate.setText(String.format(Locale.GERMAN, "%.1f%%", winRate));
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlayerName, tvMatchesPlayed, tvMatchesWon, tvWinRate;

        PlayerViewHolder(View itemView) {
            super(itemView);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvMatchesPlayed = itemView.findViewById(R.id.tvMatchesPlayed);
            tvMatchesWon = itemView.findViewById(R.id.tvMatchesWon);
            tvWinRate = itemView.findViewById(R.id.tvWinRate);
        }
    }
}
