package com.example.dcl.safetravel.historyRecycleView;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.dcl.safetravel.R;
import com.example.dcl.safetravel.singleHistoryView;

public class historyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    TextView rideId,timeStamp;
    public historyViewHolder(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        rideId = itemView.findViewById(R.id.rideId);
        timeStamp = itemView.findViewById(R.id.timeStamp);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(),singleHistoryView.class);
        Bundle b = new Bundle();
        b.putString("rideId",rideId.getText().toString());
        intent.putExtras(b);
        view.getContext().startActivity(intent);
    }
}
