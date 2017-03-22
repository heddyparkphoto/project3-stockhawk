package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.TaskTagKind;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by hyeryungpark on 12/11/16.
 */
public class StockDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = StockDetailFragment.class.getSimpleName();
    public final static String DETAIL_ARGUMENT = "DETAIL_ARGUMENT";
    private View mRootView;

    public static int mPreferenceDays;
    private static int oldPreferenceDays;

    private Intent mServiceIntent;
    private final static int HISTORICAL_CURSOR_LOADER = 4;
    private String mOfSymbol;
    private LineChart mLineChart;
    private TextView mStockTitle;
    private boolean mRefresh;
    private final static String STOCK_SYMBOL_PLACTHOLDER = "";  // Default title when no data

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_line_graph, container, false);
        mLineChart = (LineChart) mRootView.findViewById(R.id.linechart);
        mLineChart.invalidate();

        mStockTitle = (TextView) mRootView.findViewById(R.id.graphTitle);

        oldPreferenceDays = mPreferenceDays;

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String pref_span_days = shared.getString(getString(R.string.pref_historic_key), getString(R.string.default_change_over14days));

        mPreferenceDays = Integer.parseInt(pref_span_days);

        return mRootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(), HistoricalProvider.Historical.historicalOfSymbol(mOfSymbol),
                null, HistoricalColumns.SYMBOL + "= ?",
                new String[]{mOfSymbol}, HistoricalColumns.DATE_TEXT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (mLineChart != null) {
            mLineChart.invalidate();
        }

        drawLineChart(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Nothing to do
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        Context context = getActivity();

        Bundle bundle = getArguments();

        /* The condition where default stock is used.  They both can occur in Two-pane mode
         * if Refresh is needed because the user just swiped/deleted a stock in the master panel
         * or
         * it is a first screen and the user had not picked any stocks in the master panel yet.
         */
        if (mRefresh || null==bundle){
            Cursor c = context.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns.SYMBOL}, null,
                    null, null);
            if (c.moveToFirst()) {
                mOfSymbol = c.getString(0);
            } else {
                mOfSymbol = STOCK_SYMBOL_PLACTHOLDER;
            }
        } else {
            if (bundle != null) {
                mOfSymbol = bundle.getString(StockDetailFragment.DETAIL_ARGUMENT, getString(R.string.default_symbol_text));
            }
        }

        // Set title for this instance
        if (mStockTitle != null){
            if (mOfSymbol.compareTo(STOCK_SYMBOL_PLACTHOLDER)==0) {
                mStockTitle.setText(R.string.a11y_stock_symbol);
            } else {
                String caps = mOfSymbol.toUpperCase();
                mStockTitle.setText(caps);
            }
        }

        // Make sure the history of the stock doesn't already exist in the DB
        boolean shouldRefetch = true;
        Cursor cursor = context.getContentResolver().query(HistoricalProvider.Historical.historicalOfSymbol(mOfSymbol),
                null, HistoricalColumns.SYMBOL + "= ?",
                new String[]{mOfSymbol}, HistoricalColumns.DATE_TEXT);

        if (cursor.getCount() > 0) {
            int updatedCol = cursor.getColumnIndex(HistoricalColumns.UPDATED_DATE_TEXT);
            if (cursor.moveToFirst()) {
                String updatedDateStr = cursor.getString(updatedCol);
                String todateStr = Utils.getUpdatedDateText(Calendar.getInstance().getTime());

                if (updatedDateStr.equals(todateStr) && cursor.getCount() >= mPreferenceDays) {
                    shouldRefetch = false;
                }
            }
        }

        if (mOfSymbol.compareTo(STOCK_SYMBOL_PLACTHOLDER)==0 || !shouldRefetch) { //!shouldRefetch) {
            drawLineChart(cursor);
        } else if (Utils.isConnected(getActivity())) {
            /*
                We can ask our IntentService to kick off another yql that will populate our historical table
                which we can ask the Loader to get the data on a background thread-way
                well, at least that's what the intention is!  Intention not the android Intent!
             */

            getLoaderManager().initLoader(HISTORICAL_CURSOR_LOADER, null, this);

            mServiceIntent = new Intent(context, StockIntentService.class);
            mServiceIntent.putExtra("tag", TaskTagKind.HISTORIC);
            mServiceIntent.putExtra("symbol_h", mOfSymbol);
            context.startService(mServiceIntent);
        } else {
            Toast.makeText(context, getString(R.string.empty_network_not_connected), Toast.LENGTH_LONG).show();
        }

        super.onActivityCreated(savedInstanceState);
    }

    private void drawLineChart(Cursor cursor) {

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        int plotNum = cursor.getCount();
        int plotCounter = 0;

        int newFirstPosition = 0;
        if (plotNum > (mPreferenceDays - 1)) {
            newFirstPosition = plotNum - (mPreferenceDays-1);
        }

        /*
            We can reach here without data if we needed a fresh data,
            and the StockTaskService is still running on a background thread;
            set User-Friendly message and invoke invalidate() to have the LineChart wait
            onLoadFinished() invokes this method again.
         */
        if (null == cursor || plotNum == 0) {

            if (mLineChart != null) {
                if (Utils.isConnected(getActivity())) {
                    if (STOCK_SYMBOL_PLACTHOLDER.equalsIgnoreCase(mOfSymbol)){
                        mLineChart.setNoDataText(getString(R.string.linechart_ui_message));
                    } else {
                        mLineChart.setNoDataText(getString(R.string.linechart_no_data));
                    }
                } else {
                    mLineChart.setNoDataText(getString(R.string.linechart_default));
                }
                mLineChart.setNoDataTextColor(getResources().getColor(R.color.material_red_700));
                mLineChart.setNoDataTextTypeface(Typeface.SANS_SERIF);
                mLineChart.invalidate();
            }
            return;
        }

        String date;
        float high;
        float x = 0f;

        if (cursor.moveToPosition(newFirstPosition)) {
            while (cursor.moveToNext()) {
                if (plotCounter <= mPreferenceDays) {
                    date = cursor.getString(cursor.getColumnIndex(HistoricalColumns.DATE_TEXT));
                    high = cursor.getFloat(cursor.getColumnIndex(HistoricalColumns.HIGH));
                    entries.add(new Entry(x, high));
                    x = x + 1.0f;   // 1.0f increment seems to work best: one plot point per day, not smaller.
                    String unitD = shortenDate(date);  // Display mm/dd as X-axis unit
                    dates.add(unitD);
                    plotCounter++;
                }
            }
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

        // This may be a hack but have seen the mLineChart being null.  Re-get it if null.
        if (null == mLineChart) {
            mLineChart = (LineChart) mRootView.findViewById(R.id.linechart);
        }

        Description desc = new Description();
        String graphName = String.format(this.getString(R.string.a11y_graph_name), String.valueOf(mPreferenceDays));
        desc.setText(graphName);
        mLineChart.setDescription(desc);

        XAxis xaxis = mLineChart.getXAxis();
        xaxis.setValueFormatter(displayDateOnX);

        //setData to the LineChart by constructing the LineData (xStringArray, LineDataSets)
        mLineChart.setData(new LineData(lineDataSets));
    }

    /*
        Convenience method to shorten date as unit on X-Axis
        From: "2525-11-10" to "11/10".... We'll explain it is up to yesterday's market close rolling days.
     */
    private String shortenDate(String date) {
        String localD = date;
        String year = "";

        if (date == null || date.length() < 6) {
            return localD;
        }

        // if over a year, append '/yy'
        if (mPreferenceDays > 300){
            year = "/".concat(localD.substring(2,4));
        }

        // Remove the year, and use '/' between the month and date
        localD = (localD.substring(5)).replace("-", "/");

        return localD.concat(year);
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         We want to initialize and refresh (invalidate() seems to does that according to authors
         every time here instead of onCreate() which only occurs once.
        */
        mLineChart = (LineChart) mRootView.findViewById(R.id.linechart);
        mLineChart.invalidate();

        // Without init-ing here, there were no data - not sure it is the only way
         //to ensure, but it is better than crashing.
        getLoaderManager().restartLoader(HISTORICAL_CURSOR_LOADER, null, this);
    }

    /*
        When UI is in the Two-pane mode, and a stock is removed from the 'master' panel,
        refresh the 'detail' panel accordingly.
        To do so, QuoteCursorAdapter notifies the MyStocksActivity with FLAG STRING during onItemDismiss(),
        and MyStocksActivity invokes this method as it replaces the StockDetailFragment
        StockDetailFragment onActivityCreated refreshes
     */
    public void dataChanged(){
        this.mRefresh = true;
    }
}
