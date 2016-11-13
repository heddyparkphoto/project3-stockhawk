package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by hyeryungpark on 11/12/16.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class WidgetMainProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(StockTaskService.STOCK_WIDGET_DATA_UPDATED)) {

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_id_stack);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetOne: appWidgetIds){

            Intent intent = new Intent(context, WidgetMainService.class);

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetOne);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_main_layout);

            // setRemoteAdapter() method obtains the RemoteViewsFactory declared in the WidgetMainService.java
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                setRemoteAdapter(context, rv);
            } else {
                setRemoteAdapterV11(context, rv);
            }

            // Create an intent that launches each details for now, since I did not code the main yet
            // this clicking is NOT WORKING YET!!!
            Intent clickIntent = new Intent(context, StockDetailActivity.class);

            PendingIntent pendingIntent = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            rv.setPendingIntentTemplate(R.id.widget_id_stack, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetOne, rv);
        }
    }

    /*
 Sets the remote adapter used to fill in the one item

 @param views RemoteViews to set the RemoteAdapter
*/
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_id_stack,
                new Intent(context, WidgetMainService.class));
    }

    /*
        Sets the remote adapter used to fill in the one item

        @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_id_stack,
                new Intent(context, WidgetMainService.class));
    }
}
