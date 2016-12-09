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
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalColumns;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.TaskTagKind;

import java.util.ArrayList;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = StockDetailActivity.class.getSimpleName();
    public final static String LABEL = "Chart for stock";
    public static final String OF_STOCK_SYMBOL = "OF_STOCK";
    public static final int SPAN_DAYS = 14;

    private Intent mServiceIntent;
    private final static int HISTORICAL_CURSOR_LOADER = 4;
    private String mOfSymbol;
    private LineChart mLineChart;
    private TextView mStockTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mStockTitle = (TextView) findViewById(R.id.graphTitle);

        Intent intent = getIntent();
        if (intent!=null){
            if (intent.hasExtra(OF_STOCK_SYMBOL)) {
                mOfSymbol = intent.getStringExtra(OF_STOCK_SYMBOL);

                // Set title for this instance
                if (mStockTitle!=null){
                    String caps= mOfSymbol.toUpperCase();
                    mStockTitle.setText(caps);
                }

                // Make sure the history of the stock doesn't already exist in the DB
                // NOTE: Later, in Phase 2, I should implement to refresh data if data is old.  For now, I am content.
                Cursor cursor = getContentResolver().query(HistoricalProvider.Historical.historicalOfSymbol(mOfSymbol),
                        null, HistoricalColumns.SYMBOL + "= ?",
                        new String[] {mOfSymbol}, HistoricalColumns.DATE_TEXT);
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
                    mServiceIntent.putExtra("tag", TaskTagKind.HISTORIC);
                    mServiceIntent.putExtra("symbol_h", mOfSymbol);
                    startService(mServiceIntent);
               } else {
                    Toast.makeText(this, getString(R.string.empty_network_not_connected), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Log.d(LOG_TAG, "Intent is null. Cannot get symbol to look up.");
        }
    }

    private void drawLineChart(Cursor cursor) {

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        int plotNum = cursor.getCount();

        /*
            We can reach here without data if we needed a fresh data,
            and the StockTaskService is still running on a background thread;
            set User-Friendly message and invoke invalidate() to have the LineChart wait
            onLoadFinished() invokes this method again.
         */
        if (null==cursor || plotNum == 0){

            if (mLineChart!=null) {
                if (Utils.isConnected(this)) {
                    mLineChart.setNoDataText(getString(R.string.linechart_no_data));
                } else {
                    mLineChart.setNoDataText(getString(R.string.linechart_default));
                }
                mLineChart.invalidate();
            }
            return;
        }

        String date;
        float high;
        float x = 0f;

        while (cursor.moveToNext()) {
            date = cursor.getString(cursor.getColumnIndex(HistoricalColumns.DATE_TEXT));
            high = cursor.getFloat(cursor.getColumnIndex(HistoricalColumns.HIGH));
            entries.add(new Entry(x, high));
            x = x + 1.0f;   // My own set up: each plot point seems to set by each day, not smaller.
            String unitD = shortenDate(date);  // Display mm/dd as X-axis unit
            dates.add(unitD);
        }

        // Used the new feature in the MPAndroidChart:v3.0.1 to label the X-axis with shortened dates
        IndexAxisValueFormatter displayDateOnX = new IndexAxisValueFormatter();

        if (!dates.isEmpty()) {
            String[] datesArray = new String[dates.size()];
            displayDateOnX.setValues(dates.toArray(datesArray));
        }

        // Ready to set up UI for multiple datasets, ILineDataSet.  It works also for single LineDataSet that we have.
        ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

        //embellish for UI
        LineDataSet lineDataSet1 = new LineDataSet(entries, mOfSymbol); //mOfSymbol and the graph color are paired and shown in the UI
        lineDataSet1.setDrawCircles(false);
        lineDataSet1.setColor(Color.MAGENTA);

        //add to the sets
        lineDataSets.add(lineDataSet1);

        // This may be a hack but have seen the mLineChart being null.  Re-get it to initiate.
        if (null==mLineChart){
            mLineChart = (LineChart) findViewById(R.id.linechart);
        }

        Description desc = new Description();
        desc.setText(this.getString(R.string.a11y_graph_name));
        mLineChart.setDescription(desc);

        XAxis xaxis = mLineChart.getXAxis();
        xaxis.setValueFormatter(displayDateOnX);

        //setData to the LineChart by constructing the LineData (xStringArray, LineDataSets)
        mLineChart.setData(new LineData(lineDataSets));
        //mLineChart.setVisibleXRangeMaximum(35f);    //Commenting out during phase 2: did not work well with shorter ranges - experiment on the phone to get good curve
    }

    /*
        Convenience method to shorten date as unit on X-Axis
        From: "2525-11-10" to "11/10".... We'll explain it is up to yesterday's market close rolling days.
     */
    private String shortenDate(String date) {
        String localD = date;

        if (date==null || date.length() < 6){
            return localD;
        }

        // Remove the year, and use '/' between the month and date
        localD = (localD.substring(5)).replace("-", "/");
        return localD;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(this, HistoricalProvider.Historical.historicalOfSymbol(mOfSymbol),
                null, HistoricalColumns.SYMBOL + "= ?",
                new String[] {mOfSymbol}, HistoricalColumns.DATE_TEXT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (mLineChart!=null){
            mLineChart.invalidate();
        }

        drawLineChart(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

//        if (mLineChart!=null){
//            Log.d(LOG_TAG, "onLoaderReset - mLineChart is not null.");
//            mLineChart.clearValues();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         We want to initialize and refresh (invalidate() seems to does that according to authors
         every time here instead of onCreate() which only occurs once.
        */

        mLineChart = (LineChart) findViewById(R.id.linechart);
        mLineChart.invalidate();

        // Without init-ing here, there were no data - not sure it is the only way
        // to ensure, but it is better than crashing.
        getLoaderManager().restartLoader(HISTORICAL_CURSOR_LOADER, null, this);
    }
}
