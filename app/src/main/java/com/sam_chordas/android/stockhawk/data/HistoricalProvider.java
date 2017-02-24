package com.sam_chordas.android.stockhawk.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by hyeryungpark on 11/7/16.
 *
 * Provider for Database used to graph change in stock price over time.
 */
@ContentProvider(authority = HistoricalProvider.AUTHORITY, database = HistoricalDatabase.class)
public class HistoricalProvider {
    public static final String AUTHORITY = "com.sam_chordas.android.stockhawk.data.HistoricalProvider";

    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    interface Path {
        String HISTORICAL = "historical";
    }

    private static Uri buildUri(String... paths){
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path:paths){
            builder.appendPath(path);
        }
        return builder.build();
    }

    @TableEndpoint(table = HistoricalDatabase.HISTORICAL)
    public static class Historical {
        @ContentUri(
                path = Path.HISTORICAL,
                type = "vnd.android.cursor.dir/historical"
        )
        public static final Uri CONTENT_URI = buildUri(Path.HISTORICAL);

        @InexactContentUri(
                name = "HISTORICAL_OF_SYMBOL",
                path = Path.HISTORICAL + "/*",
                type = "vnd.android.cursor.item/historical",
                whereColumn = HistoricalColumns.SYMBOL,
                pathSegment = 1
        )

        public static Uri historicalOfSymbol(String symbol){
            return buildUri(Path.HISTORICAL, symbol);
        }
    }
}
