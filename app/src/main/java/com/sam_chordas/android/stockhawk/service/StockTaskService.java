package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksFragment;
import com.sam_chordas.android.stockhawk.ui.StockDetailFragment;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
/*
    Updated by: hyeryungpark for Udacity project: around 11/7/16
    * Add Historical data fetch request.
    * Add Error conditon hanlding with Status codes.  Declare IntDef annotations for the status.
    * Add Notification to Widget codes.

 */
public class StockTaskService extends GcmTaskService {
    public static final String STOCK_WIDGET_DATA_UPDATED = "com.sam_chordas.android.stockhawk.STOCK_WIDGET_DATA_UPDATED";

    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        // Base URL for the Yahoo query
        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");

        try {
            if (params.getTag().equals(TaskTagKind.HISTORIC)) {
                String of_symbol = params.getExtras().getString("symbol_h");
                String[] spanDays = Utils.formatTimeSpanForApi(StockDetailFragment.mPreferenceDays);
                urlStringBuilder.append(URLEncoder.encode("select Symbol, Date, High, Low from yahoo.finance.historicaldata where "
                        + "symbol = \"" + of_symbol + "\" and startDate = \"" + spanDays[0] + "\" and endDate = \"" + spanDays[1] + "\"", "UTF-8"));

                /*
                   Handy test to verify the historical query
                   -------------------------------------------
                    StringBuilder test = new StringBuilder();
                    test.append("https://query.yahooapis.com/v1/public/yql?q=");
                    test.append(URLEncoder.encode("select Symbol, Date, High, Low from yahoo.finance.historicaldata where "
                          + "symbol = \"" + of_symbol + "\" and startDate = \"2015-09-01\" and endDate = \"2016-09-01\"", "UTF-8"));
                    Log.d(LOG_TAG, "TEST query : " + test);
                   -------------------------------------------
                */
            } else {
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                        + "in (", "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            setStockStatus(mContext, STOCK_INVALID_NAME);
            e.printStackTrace();
        }

        if (params.getTag().equals(TaskTagKind.INIT) || params.getTag().equals(TaskTagKind.PERIODIC)) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    setStockStatus(mContext, STOCK_INVALID_NAME);
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    setStockStatus(mContext, STOCK_INVALID_NAME);
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals(TaskTagKind.ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                setStockStatus(mContext, STOCK_INVALID_NAME);
                e.printStackTrace();
            }
        } else if (params.getTag().equals(TaskTagKind.HISTORIC)) {
            // Nothing to append for the historical query
        }

        // Finalize the query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                setStockStatus(mContext, STOCK_STATUS_OK);
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }

                    if (params.getTag().equals(TaskTagKind.HISTORIC)) {
                        // Historical DB insert
                        mContext.getContentResolver().applyBatch(HistoricalProvider.AUTHORITY,
                                Utils.historicalJsonContentVals(getResponse));
                    } else {
                        // This insert already existed for the Main screen and Quote DB
                        // Add checkDbOperation check here to capture and inform the user for any error conditions.
                        ArrayList checkDbOperation = Utils.quoteJsonToContentVals(getResponse);
                        if (params.getTag().equals(TaskTagKind.ADD) && (checkDbOperation == null || checkDbOperation.isEmpty())) {
                            //ADD failed, Log and return Failure code to the StockIntentService
                            Log.i(LOG_TAG, "INVALID STOCK NAME. Response was: " + getResponse);
                            return MyStocksFragment.ADD_FAILED;
                        }

                        // If reached here, there are no errors.  Continue normally.
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                                checkDbOperation);
                    }
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error Response returned threw JSONException", e);
                    return -1;
                }
            } catch (IOException e) {
                setStockStatus(mContext, STOCK_INVALID_SERVER);
                e.printStackTrace();
            }
        }

        /*
           Database has been changed.
           Notify the WidgetMainProvider through WidgetMainService action.
        */
        Intent intent = new Intent(STOCK_WIDGET_DATA_UPDATED).setPackage(mContext.getPackageName());
        mContext.sendBroadcast(intent);

        return result;
    }

    /*
      For UI EmptyView: Provide Detailed info to help the user what went wrong when showing an empty screen.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STOCK_STATUS_OK, STOCK_INVALID_NAME, STOCK_STATUS_NOT_CONNECTED, STOCK_INVALID_SERVER, STOCK_STATUS_UNKNOWN})

    public @interface StockStatusDefinitions {
    }

    public static final int STOCK_STATUS_OK = 0;
    public static final int STOCK_INVALID_NAME = 1;
    public static final int STOCK_STATUS_NOT_CONNECTED = 2;
    public static final int STOCK_INVALID_SERVER = 3;
    public static final int STOCK_STATUS_UNKNOWN = 4;

    public static void setStockStatus(Context context, @StockStatusDefinitions int status) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = shp.edit();
        editor.putInt(context.getString(R.string.pref_stock_status), status).commit();
    }

    /*
    Handy sample url-encoded string to test historical data

    http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20
    where%20symbol%20%3D%20%22AAPL%22%20and%20startDate%20%3D%20%222012-09-11%22%20and%20endDate%20%3D%20%222014-02-11
    %22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=
     */
}
