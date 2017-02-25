package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.service.TaskTagKind;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import java.util.Locale;

/**
 * Refactored majority of functions from MyStocksActivity class created by sam_chordas on 11/9/15.
 */

/**
 * Updated by hyeryungpark on 12/11/16 for Udacity project.
 *  * Add EmptyView with Status message.
 *  * Implement RecyclerViewItemClickListener.OnItemClickListener to launch Graph in DetailFragment.
 *  * Add Error handling if Stock symbol search returns with only nulls and inform the user.
 *
 */
public class MyStocksFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    static final String LOG_TAG = MyStocksFragment.class.getSimpleName();

    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    boolean isConnected;
    private static String mNewStockName;
    private View mRootView;
    private TaskHelper mResultReceiverHelper;
    public static final int ADD_FAILED = -99;
    public static final String RECEIVER = "receiver";

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContext = getActivity();
        isConnected = Utils.isConnected(mContext);

        mRootView = inflater.inflate(R.layout.fragment_my_stocks, container, false);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(mContext, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", TaskTagKind.INIT);
            if (isConnected) {
                mContext.startService(mServiceIntent);
            } else {
                networkToast();
            }
        }

        RecyclerView recyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(mContext, null, (TextView) mRootView.findViewById(R.id.recycler_view_stocks_empty));

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(mContext,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                         String itemSymbol = ((TextView) v.findViewById(R.id.stock_symbol)).getText().toString();
                        ((MyStocksClickListener) getActivity()).OnStockItemClick(itemSymbol);
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) mRootView.findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isConnected = Utils.isConnected(mContext);
                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    String inputToUpper = null;
                                    if (input != null) {
                                        inputToUpper = input.toString().toUpperCase();
                                    }

                                    mNewStockName = inputToUpper;   // Stock symbol in DB is upper case

                                    Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{inputToUpper}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(mContext, "This stock is already saved!",
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", TaskTagKind.ADD);
                                        mServiceIntent.putExtra("symbol", input.toString());

                                        mResultReceiverHelper = new TaskHelper(new Handler());
                                        mResultReceiverHelper.setReceiver(
                                                new TaskHelper.Receiver() {
                                                    @Override
                                                    public void onReceiveResult(int resultCode, Bundle resultData) {
                                                        String msg = resultData.getString(Intent.EXTRA_TEXT);
                                                        if (resultCode==StockTaskService.STOCK_INVALID_NAME){
                                                            msg = String.format(Locale.US, getString(R.string.stock_not_found), mNewStockName);
                                                        }
                                                        Toast toast = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
                                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                                        toast.show();
                                                    }
                                                });

                                        mServiceIntent.putExtra(RECEIVER, mResultReceiverHelper);
                                        mContext.startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        if (isConnected) {
            long period = 3600L;
            long flex = 10L;

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(TaskTagKind.PERIODIC)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(mContext).schedule(periodicTask);
        }

        return mRootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(mContext, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;

        updateEmptyView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }


    private void updateEmptyView() {
        String message = getString(R.string.empty_view_textview);

        int status = Utils.getStockStatus(mContext);
        switch (status) {
            case StockTaskService.STOCK_STATUS_OK:
                break;
            case StockTaskService.STOCK_STATUS_NOT_CONNECTED:
                message = getString(R.string.empty_network_not_connected);
                break;
            case StockTaskService.STOCK_INVALID_NAME:
                // Include the stock name in the error message; mNewStockName set during "add" on the FloatingActionButton
                message = String.format(Locale.US, getString(R.string.stock_not_found), mNewStockName);
                break;
            case StockTaskService.STOCK_STATUS_UNKNOWN:
            default:
                message = getString(R.string.empty_status_unknown);
                break;
        }

        if (mCursorAdapter.getItemCount() == 0) {
            TextView emptyview = (TextView) mRootView.findViewById(R.id.recycler_view_stocks_empty);
            if (emptyview != null) {
                emptyview.setText(message);
            }
        }
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        // clean up
        mNewStockName = null;
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.compareTo(getString(R.string.pref_stock_status)) == 0) {
            updateEmptyView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.empty_network_not_connected), Toast.LENGTH_SHORT).show();
    }

    public interface MyStocksClickListener {
        public void OnStockItemClick(String stockSymbol);
    }
}