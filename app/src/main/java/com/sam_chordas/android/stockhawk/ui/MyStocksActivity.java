package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Created by sam_chordas on 11/9/15.
 */
/*
    Updated by: hyeryungpark for Udacity project: around 11/7/16
    * Add Two pane mode codes.  Refactored majority of functions to a Fragment.
    * Add Preference Settings action codes.

 */
public class MyStocksActivity extends AppCompatActivity
        implements MyStocksFragment.MyStocksClickListener {

    static final String LOG_TAG = MyStocksActivity.class.getSimpleName();
    private static final String MASTERFRAGMENT_TAG = "MFRAG";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Context mContext;
    boolean isConnected;
    public static String REMOVED_FLAG = "REMOVED";
    public static String ADDED_FLAG = "ADDED";

    // Tablet variables
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    public boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        isConnected = Utils.isConnected(this);
        setContentView(R.layout.activity_my_stocks);

        String stockSymbolOnIntent = getIntent()!=null?getIntent().getStringExtra(StockDetailActivity.OF_STOCK_SYMBOL):null;

        if (findViewById(R.id.line_graph_container) != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                StockDetailFragment df = new StockDetailFragment();
                if (stockSymbolOnIntent!=null) {
                    Bundle args = new Bundle();
                    args.putString(StockDetailFragment.DETAIL_ARGUMENT, stockSymbolOnIntent);
                    df.setArguments(args);
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.line_graph_container, df, DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;

            // Coded during Styling the Action bar - remove casted-shadow for onePane device
//            getSupportActionBar().setElevation(0f);
        }

        // Manage preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_main, false);

        mTitle = getTitle();
    }

    /*
        Click callback method when an item is clicked in the 'master' pane.
        If in Two-pane mode update the graph shown on the 'details' pane.
     */
    @Override
    public void OnStockItemClick(String stockSymbol) {
        if (mTwoPane) {
            // We are in Two-pane, update the graph shown on the 'details' pane.
            StockDetailFragment df = new StockDetailFragment();

            if (stockSymbol.compareTo(REMOVED_FLAG)==0){
                df.dataChanged();
            }
            Bundle args = new Bundle();
            args.putString(StockDetailFragment.DETAIL_ARGUMENT, stockSymbol);
            df.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.line_graph_container, df, DETAILFRAGMENT_TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            if (stockSymbol.compareTo(REMOVED_FLAG)==0) {
                // Nothing to do because the item is already cleared by RecyclerView
                // and StockDetail is not displayed in One-pane.
            } else {
                // Start Graph with explicit Intent
                Intent intent = new Intent(this, StockDetailActivity.class);
                intent.putExtra(StockDetailActivity.OF_STOCK_SYMBOL, stockSymbol);
                startActivity(intent);
            }
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // Display both launcher icon and the title
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        actionBar.setTitle(mTitle);
        actionBar.setIcon(R.mipmap.ic_launcher);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, MySettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }
}
