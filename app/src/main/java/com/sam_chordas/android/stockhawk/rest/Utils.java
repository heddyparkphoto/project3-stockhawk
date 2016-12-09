package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalColumns;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static ArrayList quoteJsonToContentVals(String JSON) throws JSONException {
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          if (!validateStock(jsonObject)) {
            return null;
          }
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              if (!validateStock(jsonObject)) {
                Log.e(LOG_TAG, "Validation failed, Not added this stock: "+jsonObject.getString("symbol"));
                continue;     // Not sure if not adding to the operations is a good way, so at least write the Error Log
              }
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
      throw e;
    } catch (Exception e){
      Log.e(LOG_TAG, "Something went wrong, could not apply batch ops. "+e);
    }
    return batchOperations;
  }

  private static boolean validateStock(JSONObject jsonObject) {

    try {
      Float.parseFloat(jsonObject.getString("Bid"));
      return true;
    } catch (JSONException | NumberFormatException ex){
      Log.e(LOG_TAG, "Validation failed! "+ex);
      return false;
    }
  }

  public static String truncateBidPrice(String bidPrice){
//    if (null==bidPrice || "null".equalsIgnoreCase(bidPrice)){
//      bidPrice = "0";
//    }
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) throws JSONException {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
        /* We will always save toUpperCase - to identify existing stock symbol to give user input non-case sensitive way */
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol").toUpperCase());
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
      throw e;
    }
    return builder.build();
  }

  @SuppressWarnings("ResourceType")  //Without this line, we cannot compile to return the IntDef we'd like to use.
  public static @StockTaskService.StockStatusDefinitions int getStockStatus(Context context){

    SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);

    if (!isConnected(context)){
      SharedPreferences.Editor editor = shp.edit();
      editor.putInt(context.getString(R.string.pref_stock_status), StockTaskService.STOCK_STATUS_NOT_CONNECTED).commit();
    }

   return shp.getInt(context.getString(R.string.pref_stock_status), StockTaskService.STOCK_STATUS_UNKNOWN);
  }

  public static boolean isConnected(Context context){
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
  }

  public static ArrayList<ContentProviderOperation> historicalJsonContentVals(String JSON)
                      throws JSONException
  {
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");

        resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

        if (resultsArray != null && resultsArray.length() != 0){
          for (int i = 0; i < resultsArray.length(); i++){
            jsonObject = resultsArray.getJSONObject(i);
            batchOperations.add(historicalBatchOperation(jsonObject));
          }

        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
      throw e;
    }
    return batchOperations;
  }

  private static ContentProviderOperation historicalBatchOperation(JSONObject jsonObject) throws JSONException {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            HistoricalProvider.Historical.CONTENT_URI);
    try {
      builder.withValue(HistoricalColumns.SYMBOL, jsonObject.getString("Symbol"));
      builder.withValue(HistoricalColumns.DATE_TEXT, jsonObject.getString("Date"));
      // HIGH LOW are REALs, using a convenience method to extract the json string to a float of two decimal point floats
      builder.withValue(HistoricalColumns.HIGH, Float.parseFloat(jsonObject.getString("High")));
      builder.withValue(HistoricalColumns.LOW, Float.parseFloat(jsonObject.getString("Low")));

    } catch (JSONException e){
      e.printStackTrace();
      throw e;
    }
    return builder.build();
  }

    /*
        This method to return start and end dates in a format specifically used for the yql.

        params:
        int daysToSubtract:  number of days to be subtracted from yesterday's date

        returns:
        String [0] is always a startDate, [1] is always an endDate.
     */
    public static String[] formatTimeSpanForApi(int daysToSubtract){
        String[] returnVal = new String[2];
        final String API_DATE_FORMAT = "YYYY-MM-dd";  // "YYYY-MM-DD" gave how many days into 366 days, 11/20/2016 was 325.
        int actualDaysToSubtract = daysToSubtract-1; // less one than passed in to include the end/start dates.

        /* Set dateFormat we need to use the yql api */
        java.text.SimpleDateFormat apiDateFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.US);

        /* Calculate historyDate */
        Calendar historyDate = Calendar.getInstance();  // Get todate
        historyDate.add(Calendar.DATE, -1); // set historyDate to be yesterday's date to get the Stock price at market close yesterday

        /* Transform to a String that the api expects */
        String endDateFormatted = apiDateFormat.format(historyDate.getTime());
        Log.d(LOG_TAG, "StockHawk endDate: "+endDateFormatted);

        historyDate.add(Calendar.DATE, (-actualDaysToSubtract));   // use one less as said above.

        String startDateFormatted = apiDateFormat.format(historyDate.getTime());
        Log.d(LOG_TAG, "StockHawk startDate: "+startDateFormatted);

        // populate the returnVal array in the right order
        returnVal[0] = startDateFormatted;
        returnVal[1] = endDateFormatted;

        return returnVal;
    }
}
