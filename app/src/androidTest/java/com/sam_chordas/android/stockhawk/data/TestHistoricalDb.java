package com.sam_chordas.android.stockhawk.data;

import android.test.AndroidTestCase;

import java.util.HashSet;

/**
 * Created by hyeryungpark on 11/7/16.
 */
public class TestHistoricalDb extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        deleteTheDatabase();
    }

    private void deleteTheDatabase() {
        mContext.deleteDatabase(HistoricalDatabase.class.getSimpleName());
    }

    public void testCreateDb() throws Throwable {
        // Hold table names that's supposed to be
        final HashSet<String> tableNameHashSet = new HashSet<>();
        tableNameHashSet.add(HistoricalDatabase.HISTORICAL);



    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
