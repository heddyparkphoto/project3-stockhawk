package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalProvider;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by hyeryungpark on 11/12/16.
 */
public class WidgetMainService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
       /*
            construct the factory that is "Adapter-like" methods implementation
         */
        RemoteViewsFactory factory = new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        null,
                        QuoteColumns.ISCURRENT  + "= ?",
                        new String[] {String.valueOf(1)},
                        null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data!=null){
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data==null?0:data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_stack_layout);

                String symbolTxt = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                String priceTxt = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
                String changeTxt = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, symbolTxt);
                }

                views.setTextViewText(R.id.widget_stock_symbol, symbolTxt);
                views.setTextViewText(R.id.widget_bid_price, priceTxt);
                views.setTextViewText(R.id.widget_change, changeTxt);

                /*
                    !!!!!! This click intent still not working.... it doesn't fire, plus one time it crashed.
                    MORE WORK NEEDED, for now, I am satisfied!  Big day - yesterday and today having actually
                    somewhat implemented the Widget collection
                 */
                final Intent fillInIntent = new Intent();

                Uri detailUri = HistoricalProvider.Historical.historicalOfSymbol(symbolTxt);

                fillInIntent.setData(detailUri);
                views.setOnClickFillInIntent(R.id.widget_stack_parent, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {

                return new RemoteViews(getPackageName(), R.layout.widget_stack_layout);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                // Something's not working ...with using the row, come back later!!!
//                if (data.moveToPosition(position)){
//                    return Long.valueOf(data.getString(QuoteColumns._ID));
//                } else {
                    return position;
//                }
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };

        return factory;
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_stock_symbol, description);
    }
}
