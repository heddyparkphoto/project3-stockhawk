package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.util.ArrayList;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class StockDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = StockDetailActivity.class.getSimpleName();
    private LineChart mLineChart;
    public final static String LABEL = "Chart for stock";
    public static final String OF_STOCK_SYMBOL = "OF_STOCK";
    private Intent mServiceIntent;

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
            if (intent.hasExtra(OF_STOCK_SYMBOL)) {
                String symbol = intent.getStringExtra(OF_STOCK_SYMBOL);
                Toast.makeText(this, "passed intent "+ symbol, Toast.LENGTH_SHORT).show();

                /*
                    We can ask our IntentService to kick off another yql that will populate our historical table
                    which we can ask the Loader to get the data on a background thread-way
                    well, at least that's what the intention is!  Intention not the android Intent!
                 */
                if (Utils.isConnected(this)){
                    mServiceIntent = new Intent(this, StockIntentService.class);
                    // a new tag that I will refactor the existing methods to process the new yql queries to get what we want: historical data
                    mServiceIntent.putExtra("tag", "historicalData");
                    mServiceIntent.putExtra("symbol_h", symbol);
                    startService(mServiceIntent);
                }

            }

        } else {
            Log.d(LOG_TAG, "Intent is null");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //curious - check if we have the historical database
        Cursor queryCursor;
        Uri uri = HistoricalProvider.Historical.historicalOfSymbol("GOOG");
        queryCursor = this.getContentResolver().query(uri,
                null, null,
                null, null);
        if (queryCursor != null) {
            // Init task. Populates DB with quotes for the symbols seen below
            Toast.makeText(this, "GOOG has " + queryCursor.getCount()+" rows!", Toast.LENGTH_LONG).show();

            queryCursor.close();
        }
    }
}
