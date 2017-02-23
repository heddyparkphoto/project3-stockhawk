package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by hyeryungpark on 11/7/16.
 */
@Database(version=HistoricalDatabase.VERSION)
public class HistoricalDatabase {
    private HistoricalDatabase(){}

    public static final int VERSION = 2;

    @Table(HistoricalColumns.class) public static final String HISTORICAL = "historical";
}
