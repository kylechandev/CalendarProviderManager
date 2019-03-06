package com.kyle.calendarprovider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by KYLE on 2019/3/6 - 13:53
 */
public class TimeUtil {

    private static String getEndDate(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return format.format(date);
    }

    public static String getFinalRRuleMode(long time) {
        return getEndDate(time) + "T235959Z";
    }

}
