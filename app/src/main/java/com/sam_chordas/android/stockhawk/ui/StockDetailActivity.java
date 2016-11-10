package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalColumns;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.util.ArrayList;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = StockDetailActivity.class.getSimpleName();
    public final static String LABEL = "Chart for stock";
    public static final String OF_STOCK_SYMBOL = "OF_STOCK";
    private Intent mServiceIntent;
    private final static int HISTORICAL_CURSOR_LOADER = 4;
    private String mOfSymbol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);


        Intent intent = getIntent();
        if (intent!=null){
            if (intent.hasExtra(OF_STOCK_SYMBOL)) {
                mOfSymbol = intent.getStringExtra(OF_STOCK_SYMBOL);

                // Make sure the history of the stock doesn't already exist in the DB
                // NOTE: Later, in Phase 2, I should implement to refresh data if data is old.  For now, I am content.
                Cursor cursor = getContentResolver().query(HistoricalProvider.Historical.historicalOfSymbol(mOfSymbol),
                        null, HistoricalColumns.SYMBOL + "= ?",
                        new String[] {mOfSymbol}, null);
                if (cursor.getCount() != 0) {
                    drawLineChart(cursor);
                }
                /*
                    We can ask our IntentService to kick off another yql that will populate our historical table
                    which we can ask the Loader to get the data on a background thread-way
                    well, at least that's what the intention is!  Intention not the android Intent!
                 */
                else if (Utils.isConnected(this)){
                    getLoaderManager().initLoader(HISTORICAL_CURSOR_LOADER, null, this);

                    mServiceIntent = new Intent(this, StockIntentService.class);
                    /*  The tag and the key/value that I refactor-ed the existing methods
                        to process the new yql queries to get what we want: historical data
                    */
                    mServiceIntent.putExtra("tag", "historicalData");
                    mServiceIntent.putExtra("symbol_h", mOfSymbol);
                    startService(mServiceIntent);
               }
            }
        } else {
            Log.d(LOG_TAG, "Intent is null. Cannot get symbol to look up.");
        }
    }

    private void drawLineChart(Cursor cursor) {
        LineChart mLineChart;
        mLineChart = (LineChart) findViewById(R.id.linechart);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        int plotNum = cursor.getCount();

        /*
            We can reach here without data if we needed a fresh data,
            and the StockTaskService is still running on a background thread;
            set off this loader again.
         */
        if (null==cursor || plotNum == 0){
            Log.d(LOG_TAG, "drawLineChart - no data");
            return;
        }

        String date;
        float high;
        float x = 0f;

        while (cursor.moveToNext()) {
            date = cursor.getString(cursor.getColumnIndex(HistoricalColumns.DATE_TEXT));
            high = cursor.getFloat(cursor.getColumnIndex(HistoricalColumns.HIGH));
            entries.add(new Entry(x, high));
            x = x + 0.1f;
            dates.add(date);
        }

        // Ready to set up UI for multiple datasets, ILineDataSet.  It works also for single LineDataSet that we have.
        ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

        //embellish for UI
        LineDataSet lineDataSet1 = new LineDataSet(entries, mOfSymbol);
        lineDataSet1.setDrawCircles(false);
        lineDataSet1.setColor(Color.MAGENTA);

        //add to the sets
        lineDataSets.add(lineDataSet1);

        //setData to the LineChart by constructing the LineData (xStringArray, LineDataSets)
        mLineChart.setData(new LineData(lineDataSets));
        mLineChart.setVisibleXRangeMaximum(35f);    // experiment on the phone to get good curve
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        Log.d(LOG_TAG, "onCreateLoader");

        return new CursorLoader(this, HistoricalProvider.Historical.historicalOfSymbol(mOfSymbol),
                null, HistoricalColumns.SYMBOL + "= ?",
                new String[] {mOfSymbol}, HistoricalColumns.DATE_TEXT+" ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(LOG_TAG, "onLoadFinished");
        drawLineChart(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        Log.d(LOG_TAG, "onLoaderReset");
        // Placeholder if old graph must be cleared for some reason.  In such case make the mLineChart available.
        //Clear the data in the mLineChart
//        if (mLineChart!=null){

//            Log.d(LOG_TAG, "onLoaderReset - mLineChart is not null.");
//            mLineChart.clearValues();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Placeholder if old graph must be cleared for some reason.  In such case make the mLineChart available.
//        if (mLineChart!=null){

            Log.d(LOG_TAG, "onResume"); // - mLineChart is not null.");
//
//        } else {
//            Log.d(LOG_TAG, "onResume - mLineChart is NULL!!!");
//        }
    }


}
