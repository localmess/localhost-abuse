package com.localhost_abuse.xprofilepoc;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ListView listViewHistory;
    private TextView emptyView;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listViewHistory = findViewById(R.id.listViewHistory);
        emptyView = findViewById(R.id.textViewEmpty);

        List<String> logs = LogDatabaseHelper.getInstance(this).getAllLogs();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);

        if (logs.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listViewHistory.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            listViewHistory.setVisibility(View.VISIBLE);
            listViewHistory.setAdapter(adapter);
        }

        RequestLogger.getInstance().getLogLiveData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String logEntry) {
                List<String> updatedLogs = LogDatabaseHelper.getInstance(HistoryActivity.this).getAllLogs();
                if (updatedLogs.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    listViewHistory.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    listViewHistory.setVisibility(View.VISIBLE);
                    adapter.clear();
                    adapter.addAll(updatedLogs);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }
}
