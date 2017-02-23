package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class HistoricalColumns {
    @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
    public static final String _ID = "_id";

    @DataType(DataType.Type.TEXT) @NotNull
    public static final String SYMBOL = "symbol";

    @DataType(DataType.Type.TEXT) @NotNull
    public static final String DATE_TEXT = "date_text";

    @DataType(DataType.Type.REAL)
    public static final String HIGH = "high_price";

    @DataType(DataType.Type.REAL)
    public static final String LOW = "low_price";

    @DataType(DataType.Type.TEXT) @NotNull
    public static final String UPDATED_DATE_TEXT = "updated_date_text";
}
