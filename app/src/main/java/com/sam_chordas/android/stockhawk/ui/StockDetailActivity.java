package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class StockDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = StockDetailActivity.class.getSimpleName();
    public final static String LABEL = "Chart for stock";
    public static final String OF_STOCK_SYMBOL = "OF_STOCK";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        if (savedInstanceState == null) {

            // Following code during Refactoring of DetailFragment to its own class
            Bundle args = new Bundle();
            // in order to de-couple from either MainActivity or DetailActivity use bundle arguments
            args.putString(StockDetailFragment.DETAIL_ARGUMENT, getIntent().getStringExtra(OF_STOCK_SYMBOL));

            StockDetailFragment df = new StockDetailFragment();
            df.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.line_graph_container, df)
                    .commit();
        }

        Log.d(LOG_TAG, "Intent is null. Cannot get symbol to look up.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         We want to initialize and refresh (invalidate() seems to does that according to authors
         every time here instead of onCreate() which only occurs once.
        */
    }

}
