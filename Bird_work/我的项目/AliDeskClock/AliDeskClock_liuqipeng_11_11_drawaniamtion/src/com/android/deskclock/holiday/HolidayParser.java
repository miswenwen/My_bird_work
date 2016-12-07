package com.android.deskclock.holiday;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.JsonReader;

import com.android.deskclock.holiday.HolidayManager.HolidayByYear;
import com.android.deskclock.utils.io.IOUtils;
import com.android.deskclock.Log;

public class HolidayParser {

    public List<HolidayByYear> mYears = new ArrayList<HolidayByYear>();

    public HolidayParser() {
    }

    public void parse(File file) throws IOException {
        Log.i("parse holiday data from file:" + file);
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            parse(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void parse(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            readHolidayByYearArray(reader);
        }
        finally {
            reader.close();
        }
    }

    private void readHolidayByYearArray(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            HolidayByYear yearData = readHolidayByYear(reader);
            mYears.add(yearData);
        }
        reader.endArray();
    }

    private HolidayByYear readHolidayByYear(JsonReader reader) throws IOException {
        int year = -1;
        List<String> holidayData = null;
        List<String> workdayData = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (HolidayContract.PROP_YEAR.equals(name)) {
                year = reader.nextInt();
            } else if (HolidayContract.PROP_HOLIDAY.equals(name)) {
                holidayData = readDayArray(reader);
            } else if (HolidayContract.PROP_WORKDAY.equals(name)) {
                workdayData = readDayArray(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        int[] holidayMonthData = convert2MonthDay(holidayData);
        int[] workdayMonthData = convert2MonthDay(workdayData);
        //for(int i=0;i<holidayMonthData.length;i++)
        //Log.i("holidayMonthData="+holidayMonthData[i]);
        HolidayByYear yearData = new HolidayByYear(year, holidayMonthData, workdayMonthData);
        return yearData;
    }

    private List<String> readDayArray(JsonReader reader) throws IOException {
        List<String> dayArray = new ArrayList<String>();
        reader.beginArray();
        while (reader.hasNext()) {
            dayArray.add(reader.nextString());
        }
        reader.endArray();
        return dayArray;
    }

    private int[] convert2MonthDay(List<String> dayArray) {
        int[] monthData = new int[12];
        Arrays.fill(monthData, 0);
        for (String day : dayArray) {
            if (day != null && day.length() == 4) {
                int month = Integer.parseInt(day.substring(0, 2)) - 1;
                int monthday = Integer.parseInt(day.substring(2, 4));
                if (0 <= month && month < 12 && 0 < monthday && monthday < 32) {
                    monthData[month] |= 1 << (monthday - 1);
                }
            }
        }
        return monthData;
    }

    public void save(Context context, int version) {
        Log.i("save holiday data!");
        HolidayManager.getInstance(context).saveHoliday(version, mYears);
    }
}
