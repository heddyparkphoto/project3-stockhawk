package com.sam_chordas.android.stockhawk;

import android.test.AndroidTestCase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by hyeryungpark on 11/21/16.
 */
public class DateCalculationTest extends AndroidTestCase {

    static final private String LOG_TAG = "DateCalculationTest";
    static final private String API_DATE_FORMAT = "YYYY-MM-dd";  // "YYYY-MM-DD" gave how many days into 366 days, 11/20/2016 was 325.

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testStartAndEndDateYesterdayFormatted(){

        /* Set dateFormat we need to use the yql api */
        java.text.SimpleDateFormat apiDateFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.US);

        /* Calculate historyDate */
        Calendar historyDate = Calendar.getInstance();  // Get todate
        historyDate.add(Calendar.DATE, -1); // set historyDate to be yesterday's date to get the Stock price at market close yesterday

        /* Transform to a String that the api expects */
        String queryDateFormat = apiDateFormat.format(historyDate.getTime());
        Log.d(LOG_TAG, "StockHawk endDate: "+queryDateFormat);

        historyDate.add(Calendar.DATE, -6);

        queryDateFormat = apiDateFormat.format(historyDate.getTime());
        Log.d(LOG_TAG, "StockHawk startDate: "+queryDateFormat);
    }

//    public void testStartAndEndDateYesterdayFormat (){

        /*
            endDate should be the yesterday
            startDate should be 7 days ago 'including today,therefore subtract 6 days'.

            final format should be what the yahoo api has: "2015-09-01"
         */

//        Calendar endDate = Calendar.getInstance();  // Get todate
//        endDate.add(Calendar.DATE, -1); // set endDate to be yesterday's date to get the Stock price at market close yesterday
//
//        String debugString = endDate.get(Calendar.YEAR)+"-"+(endDate.get(Calendar.MONTH)+1)+"-"+endDate.get(Calendar.DATE);
//        Log.d(LOG_TAG, "Current date: "+debugString);
//
//        endDate.add(Calendar.DATE, -6);
//        debugString = endDate.get(Calendar.YEAR)+"-"+(endDate.get(Calendar.MONTH)+1)+"-"+endDate.get(Calendar.DATE);
//        Log.d(LOG_TAG, "Current date: "+debugString);
//    }

//    public void testAnyStartAndEndDateFormat(){
        //Calendar endDate = Calendar.getInstance();  // Test todate
        //Calendar endDate = new GregorianCalendar(2016, Calendar.MARCH, 2); // Test leap year
        //Calendar endDate = new GregorianCalendar(2015, Calendar.MARCH, 2); // Test Non-leap year
        //Calendar endDate = new GregorianCalendar(2016, Calendar.JANUARY, 2); // Test change of the years & 31 day month
//        Calendar endDate = new GregorianCalendar(2016, Calendar.JULY, 2); // Test 30 day month
//        String debugString = endDate.get(Calendar.YEAR)+"-"+(endDate.get(Calendar.MONTH)+1)+"-"+endDate.get(Calendar.DATE);
//        Log.d(LOG_TAG, "Current date: "+debugString);
//
//        endDate.add(Calendar.DATE, -6);
//        debugString = endDate.get(Calendar.YEAR)+"-"+(endDate.get(Calendar.MONTH)+1)+"-"+endDate.get(Calendar.DATE);
//        Log.d(LOG_TAG, "Current date: "+debugString);
//
//    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
