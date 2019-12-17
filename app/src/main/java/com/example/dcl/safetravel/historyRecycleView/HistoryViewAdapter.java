package com.example.dcl.safetravel.historyRecycleView;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.dcl.safetravel.R;

import java.util.List;

public class HistoryViewAdapter extends RecyclerView.Adapter<historyViewHolder>{

    List<historyView> item;
    Context context;

    public HistoryViewAdapter(List<historyView> item, Context context) {
        this.item = item;
        this.context = context;
    }

    @NonNull
    @Override
    public historyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_history,viewGroup,false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(lp);
        historyViewHolder hvh = new historyViewHolder(layout);

        return hvh;
    }

    @Override
    public void onBindViewHolder(@NonNull historyViewHolder historyViewHolder, int i) {
        historyViewHolder.rideId.setText(item.get(i).getRideId());
        historyViewHolder.timeStamp.setText(item.get(i).getTimeStamp());
    }

    @Override
    public int getItemCount() {
        return item.size();
    }
}