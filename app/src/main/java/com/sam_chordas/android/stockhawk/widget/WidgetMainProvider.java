package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by hyeryungpark on 11/12/16.
 */
public class WidgetMainProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetOne: appWidgetIds){

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_main_layout);

            // Create an intent that launches each details for now, since I did not code the main yet
            Intent intent = new Intent(context, StockDetailActivity.class);

            PendingIntent clickIntent = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(intent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            rv.setPendingIntentTemplate(R.id.widget_id_stack, clickIntent);
            appWidgetManager.updateAppWidget(appWidgetOne, rv);
        }
    }

    /*
 Sets the remote adapter used to fill in the list item

 @param views RemoteViews to set the RemoteAdapter
*/
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_id_stack,
                new Intent(context, WidgetMainService.class));
    }

    /*
        Sets the remote adapter used to fill in the list item

        @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_id_stack,
                new Intent(context, WidgetMainService.class));
    }
}
