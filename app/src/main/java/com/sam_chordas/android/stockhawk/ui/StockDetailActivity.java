package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;

import java.util.ArrayList;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class StockDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = StockDetailActivity.class.getSimpleName();
    private LineChart mLineChart;
    public final static String LABEL = "Chart for stock";

    // Plots saved as Entry(float x, float y)
    private ArrayList<Entry> highPricePoints = new ArrayList<Entry>();
    // Lines that can hold 1 or more graphs representing the points ArrayList - for now we'll only plot high prices
    private ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mLineChart = (LineChart) findViewById(R.id.linechart);

        Intent intent = getIntent();
        if (intent!=null){
            if (intent.hasExtra("OF_STOCK")) {
                String symbol = intent.getStringExtra("OF_STOCK");
                Toast.makeText(this, "passed intent "+ symbol, Toast.LENGTH_SHORT).show();
            }

        } else {
            Log.d(LOG_TAG, "Intent is null");
        }
    }


}
