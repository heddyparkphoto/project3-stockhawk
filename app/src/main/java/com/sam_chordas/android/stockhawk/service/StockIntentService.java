package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.ui.MyStocksFragment;

/**
 * Created by sam_chordas on 10/1/15.
 */
/*
    Updated by: hyeryungpark for Udacity project: around 11/7/16
    * Add Historical request handling.
    * Add result handling to give User feedback in case of unexpected failure.
 */
public class StockIntentService extends IntentService {

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();

        if (intent.getStringExtra("tag").equals(TaskTagKind.ADD)) {
            args.putString("symbol", intent.getStringExtra("symbol"));
        } else if (intent.getStringExtra("tag").equals(TaskTagKind.HISTORIC)) {
            args.putString("symbol_h", intent.getStringExtra("symbol_h"));
        }

        // Activity would like to receive the result if "add" operation fails
        final ResultReceiver receiver = intent.getParcelableExtra(MyStocksFragment.RECEIVER);
        Bundle bundle = new Bundle();

        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        int result =stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));

        // Activity would like to handle the result if "add" operation fails
        if (receiver!=null) {
            if (result == MyStocksFragment.ADD_FAILED) {
                bundle.putString(Intent.EXTRA_TEXT, "Add a stock failed!");
                receiver.send(StockTaskService.STOCK_INVALID_NAME, bundle);
            }
        }
    }
}
