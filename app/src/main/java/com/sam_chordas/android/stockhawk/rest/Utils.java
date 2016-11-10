package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.HistoricalColumns;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
      throw e;
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    if (null==bidPrice || "null".equalsIgnoreCase(bidPrice)){
      bidPrice = "0";
    }
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
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
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
   // return shp.getInt(context.getString(R.string.pref_stock_status), StockTaskService.STOCK_STATUS_UNKNOWN);

    if (isConnected(context)){
      return StockTaskService.STOCK_STATUS_OK;
    } else {
      return StockTaskService.STOCK_STATUS_NOT_CONNECTED;
    }
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
      builder.withValue(HistoricalColumns.HIGH, roundPrice(jsonObject.getString("High")));
      builder.withValue(HistoricalColumns.LOW, roundPrice(jsonObject.getString("Low")));

    } catch (JSONException e){
      e.printStackTrace();
      throw e;
    }
    return builder.build();
  }

  private static float roundPrice(String price) {

    float returnVal;
    if (null==price || price.length()==0){
      return 0f;
    } else {
      String temp = truncateBidPrice(price);
      returnVal = Float.parseFloat(temp);
      return returnVal;
    }
  }
}
