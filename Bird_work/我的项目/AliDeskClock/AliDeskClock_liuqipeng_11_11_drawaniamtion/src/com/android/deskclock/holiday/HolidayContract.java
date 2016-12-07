package com.android.deskclock.holiday;

public final class HolidayContract {

    /* Holiday file format, JSON array:
     * [{"year":2014, "holiday":["0101", "0131", ...], "workday":["0430", ...]},
     *    {"year":2015, "holiday":["0101", "0102", ...], "workday":["0104", ...]}]
     */

    public static final String PROP_YEAR = "year";
    public static final String PROP_HOLIDAY = "holiday";
    public static final String PROP_WORKDAY = "workday";

}
