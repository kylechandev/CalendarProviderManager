package com.kyle.calendarprovider.calendar;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

import com.kyle.calendarprovider.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static com.kyle.calendarprovider.Util.checkContextNull;

/**
 * 系统日历工具
 * <p>
 * Created by KYLE on 2019/3/3 - 20:37
 *
 * @see AdvanceTime
 * @see RRuleConstant
 * @see CalendarEvent
 */
public class CalendarProviderManager {

    private static StringBuilder builder = new StringBuilder();


    /*
       TIP: 要向系统日历插入事件,前提系统中必须存在至少1个日历账户
     */


    // ----------------------- 创建日历账户时账户名使用 ---------------------------
    private static String CALENDAR_NAME = "KyleC";
    private static String CALENDAR_ACCOUNT_NAME = "KYLE";
    private static String CALENDAR_DISPLAY_NAME = "KYLE的账户";


    // ------------------------------- 日历账户 -----------------------------------

    /**
     * 获取日历账户ID(若没有则会自动创建一个)
     *
     * @return success: 日历账户ID  failed : -1  permission deny : -2
     */
    @SuppressWarnings("WeakerAccess")
    public static long obtainCalendarAccountID(Context context) {
        long calID = checkCalendarAccount(context);
        if (calID >= 0) {
            return calID;
        } else {
            return createCalendarAccount(context);
        }
    }

    /**
     * 检查是否存在日历账户
     *
     * @return 存在：日历账户ID  不存在：-1
     */
    private static long checkCalendarAccount(Context context) {
        try (Cursor cursor = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                null, null, null, null)) {
            // 不存在日历账户
            if (null == cursor) {
                return -1;
            }
            int count = cursor.getCount();
            // 存在日历账户，获取第一个账户的ID
            if (count > 0) {
                cursor.moveToFirst();
                return cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars._ID));
            } else {
                return -1;
            }
        }
    }

    /**
     * 创建一个新的日历账户
     *
     * @return success：ACCOUNT ID , create failed：-1 , permission deny：-2
     */
    private static long createCalendarAccount(Context context) {
        // 系统日历表
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        // 要创建的账户
        Uri accountUri;

        // 开始组装账户数据
        ContentValues account = new ContentValues();
        // 账户类型：本地
        // 在添加账户时，如果账户类型不存在系统中，则可能该新增记录会被标记为脏数据而被删除
        // 设置为ACCOUNT_TYPE_LOCAL可以保证在不存在账户类型时，该新增数据不会被删除
        account.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        // 日历在表中的名称
        account.put(CalendarContract.Calendars.NAME, CALENDAR_NAME);
        // 日历账户的名称
        account.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME);
        // 账户显示的名称
        account.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME);
        // 日历的颜色
        account.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.parseColor("#515bd4"));
        // 用户对此日历的获取使用权限等级
        account.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        // 设置此日历可见
        account.put(CalendarContract.Calendars.VISIBLE, 1);
        // 日历时区
        account.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());
        // 可以修改日历时区
        account.put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1);
        // 同步此日历到设备上
        account.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        // 拥有者的账户
        account.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDAR_ACCOUNT_NAME);
        // 可以响应事件
        account.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1);
        // 单个事件设置的最大的提醒数
        account.put(CalendarContract.Calendars.MAX_REMINDERS, 8);
        // 设置允许提醒的方式
        account.put(CalendarContract.Calendars.ALLOWED_REMINDERS, "0,1,2,3,4");
        // 设置日历支持的可用性类型
        account.put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, "0,1,2");
        // 设置日历允许的出席者类型
        account.put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, "0,1,2");

        /*
            TIP: 修改或添加ACCOUNT_NAME只能由SYNC_ADAPTER调用
            对uri设置CalendarContract.CALLER_IS_SYNCADAPTER为true,即标记当前操作为SYNC_ADAPTER操作
            在设置CalendarContract.CALLER_IS_SYNCADAPTER为true时,必须带上参数ACCOUNT_NAME和ACCOUNT_TYPE(任意)
         */
        uri = uri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.Calendars.CALENDAR_LOCATION)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查日历权限
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(
                    "android.permission.WRITE_CALENDAR")) {
                accountUri = context.getContentResolver().insert(uri, account);
            } else {
                return -2;
            }
        } else {
            accountUri = context.getContentResolver().insert(uri, account);
        }

        return accountUri == null ? -1 : ContentUris.parseId(accountUri);
    }

    /**
     * 删除创建的日历账户
     *
     * @return -2: permission deny  0: No designated account  1: delete success
     */
    public static int deleteCalendarAccountByName(Context context) {
        checkContextNull(context);

        int deleteCount;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[]{CALENDAR_ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(
                    "android.permission.WRITE_CALENDAR")) {
                deleteCount = context.getContentResolver().delete(uri, selection, selectionArgs);
            } else {
                return -2;
            }
        } else {
            deleteCount = context.getContentResolver().delete(uri, selection, selectionArgs);
        }

        return deleteCount;
    }


    // ------------------------------- 添加日历事件 -----------------------------------

    /**
     * 添加日历事件
     *
     * @param calendarEvent 日历事件(详细参数说明请参看{@link CalendarEvent}构造方法)
     * @return 0: success  -1: failed  -2: permission deny
     */
    public static int addCalendarEvent(Context context, CalendarEvent calendarEvent) {
         /*
            TIP: 插入一个新事件的规则：
             1.  必须包含CALENDAR_ID和DTSTART字段
             2.  必须包含EVENT_TIMEZONE字段,使用TimeZone.getDefault().getID()方法获取默认时区
             3.  对于非重复发生的事件,必须包含DTEND字段
             4.  对重复发生的事件,必须包含一个附加了RRULE或RDATE字段的DURATION字段
         */

        checkContextNull(context);

        // 获取日历账户ID，也就是要将事件插入到的账户
        long calID = obtainCalendarAccountID(context);

        // 系统日历事件表
        Uri uri1 = CalendarContract.Events.CONTENT_URI;
        // 创建的日历事件
        Uri eventUri;

        // 系统日历事件提醒表
        Uri uri2 = CalendarContract.Reminders.CONTENT_URI;
        // 创建的日历事件提醒
        Uri reminderUri;

        // 开始组装事件数据
        ContentValues event = new ContentValues();
        // 事件要插入到的日历账户
        event.put(CalendarContract.Events.CALENDAR_ID, calID);
        setupEvent(calendarEvent, event);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 判断权限
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(
                    "android.permission.WRITE_CALENDAR")) {
                eventUri = context.getContentResolver().insert(uri1, event);
            } else {
                return -2;
            }
        } else {
            eventUri = context.getContentResolver().insert(uri1, event);
        }

        if (null == eventUri) {
            return -1;
        }


        if (-2 != calendarEvent.getAdvanceTime()) {
            // 获取事件ID
            long eventID = ContentUris.parseId(eventUri);

            // 开始组装事件提醒数据
            ContentValues reminders = new ContentValues();
            // 此提醒所对应的事件ID
            reminders.put(CalendarContract.Reminders.EVENT_ID, eventID);
            // 设置提醒提前的时间(0：准时  -1：使用系统默认)
            reminders.put(CalendarContract.Reminders.MINUTES, calendarEvent.getAdvanceTime());
            // 设置事件提醒方式为通知警报
            reminders.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            reminderUri = context.getContentResolver().insert(uri2, reminders);

            if (null == reminderUri) {
                return -1;
            }
        }

        return 0;
    }


    // ------------------------------- 更新日历事件 -----------------------------------

    /**
     * 更新指定ID的日历事件
     *
     * @param newCalendarEvent 更新的日历事件
     * @return -2: permission deny  else success
     */
    public static int updateCalendarEvent(Context context, long eventID, CalendarEvent newCalendarEvent) {
        checkContextNull(context);

        int updatedCount1;

        Uri uri1 = CalendarContract.Events.CONTENT_URI;
        Uri uri2 = CalendarContract.Reminders.CONTENT_URI;

        ContentValues event = new ContentValues();
        setupEvent(newCalendarEvent, event);

        // 更新匹配条件
        String selection1 = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs1 = new String[]{String.valueOf(eventID)};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(
                    "android.permission.WRITE_CALENDAR")) {
                updatedCount1 = context.getContentResolver().update(uri1, event, selection1, selectionArgs1);
            } else {
                return -2;
            }
        } else {
            updatedCount1 = context.getContentResolver().update(uri1, event, selection1, selectionArgs1);
        }


        ContentValues reminders = new ContentValues();
        reminders.put(CalendarContract.Reminders.MINUTES, newCalendarEvent.getAdvanceTime());
        reminders.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

        // 更新匹配条件
        String selection2 = "(" + CalendarContract.Reminders.EVENT_ID + " = ?)";
        String[] selectionArgs2 = new String[]{String.valueOf(eventID)};

        int updatedCount2 = context.getContentResolver().update(uri2, reminders, selection2, selectionArgs2);

        return (updatedCount1 + updatedCount2) / 2;
    }

    /**
     * 更新指定ID事件的开始时间
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventbeginTime(Context context, long eventID, long newBeginTime) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DTSTART, newBeginTime);

        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的结束时间
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventEndTime(Context context, long eventID, long newEndTime) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DTEND, newEndTime);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的起始时间
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventTime(Context context, long eventID, long newBeginTime,
                                              long newEndTime) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DTSTART, newBeginTime);
        event.put(CalendarContract.Events.DTEND, newEndTime);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的标题
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventTitle(Context context, long eventID, String newTitle) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, newTitle);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的描述
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventDes(Context context, long eventID, String newEventDes) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DESCRIPTION, newEventDes);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的地点
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventLocation(Context context, long eventID, String newEventLocation) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.EVENT_LOCATION, newEventLocation);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的标题和描述
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventTitAndDes(Context context, long eventID, String newEventTitle,
                                                   String newEventDes) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, newEventTitle);
        event.put(CalendarContract.Events.DESCRIPTION, newEventDes);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的常用信息(标题、描述、地点)
     *
     * @return If successfully returns 1
     */
    public static int updateCalendarEventCommonInfo(Context context, long eventID, String newEventTitle,
                                                    String newEventDes, String newEventLocation) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, newEventTitle);
        event.put(CalendarContract.Events.DESCRIPTION, newEventDes);
        event.put(CalendarContract.Events.EVENT_LOCATION, newEventLocation);


        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }

    /**
     * 更新指定ID事件的提醒方式
     *
     * @return If successfully returns 1
     */
    private static int updateCalendarEventReminder(Context context, long eventID, long newAdvanceTime) {
        checkContextNull(context);

        Uri uri = CalendarContract.Reminders.CONTENT_URI;

        ContentValues reminders = new ContentValues();
        reminders.put(CalendarContract.Reminders.MINUTES, newAdvanceTime);

        // 更新匹配条件
        String selection2 = "(" + CalendarContract.Reminders.EVENT_ID + " = ?)";
        String[] selectionArgs2 = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, reminders, selection2, selectionArgs2);
    }

    /**
     * 更新指定ID事件的提醒重复规则
     *
     * @return If successfully returns 1
     */
    private static int updateCalendarEventRRule(Context context, long eventID, String newRRule) {
        checkContextNull(context);

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.RRULE, newRRule);

        // 匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, event, selection, selectionArgs);
    }


    // ------------------------------- 删除日历事件 -----------------------------------

    /**
     * 删除日历事件
     *
     * @param eventID 事件ID
     * @return -2: permission deny  else success
     */
    public static int deleteCalendarEvent(Context context, long eventID) {
        checkContextNull(context);

        int deletedCount1;
        Uri uri1 = CalendarContract.Events.CONTENT_URI;
        Uri uri2 = CalendarContract.Reminders.CONTENT_URI;

        // 删除匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(
                    "android.permission.WRITE_CALENDAR")) {
                deletedCount1 = context.getContentResolver().delete(uri1, selection, selectionArgs);
            } else {
                return -2;
            }
        } else {
            deletedCount1 = context.getContentResolver().delete(uri1, selection, selectionArgs);
        }

        // 删除匹配条件
        String selection2 = "(" + CalendarContract.Reminders.EVENT_ID + " = ?)";
        String[] selectionArgs2 = new String[]{String.valueOf(eventID)};

        int deletedCount2 = context.getContentResolver().delete(uri2, selection2, selectionArgs2);

        return (deletedCount1 + deletedCount2) / 2;
    }


    // ------------------------------- 查询日历事件 -----------------------------------

    /**
     * 查询指定日历账户下的所有事件
     *
     * @return If failed return null else return List<CalendarEvent>
     */
    public static List<CalendarEvent> queryAccountEvent(Context context, long calID) {
        checkContextNull(context);

        final String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Events.CALENDAR_ID,             // 在表中的列索引0
                CalendarContract.Events.TITLE,                   // 在表中的列索引1
                CalendarContract.Events.DESCRIPTION,             // 在表中的列索引2
                CalendarContract.Events.EVENT_LOCATION,          // 在表中的列索引3
                CalendarContract.Events.DISPLAY_COLOR,           // 在表中的列索引4
                CalendarContract.Events.STATUS,                  // 在表中的列索引5
                CalendarContract.Events.DTSTART,                 // 在表中的列索引6
                CalendarContract.Events.DTEND,                   // 在表中的列索引7
                CalendarContract.Events.DURATION,                // 在表中的列索引8
                CalendarContract.Events.EVENT_TIMEZONE,          // 在表中的列索引9
                CalendarContract.Events.EVENT_END_TIMEZONE,      // 在表中的列索引10
                CalendarContract.Events.ALL_DAY,                 // 在表中的列索引11
                CalendarContract.Events.ACCESS_LEVEL,            // 在表中的列索引12
                CalendarContract.Events.AVAILABILITY,            // 在表中的列索引13
                CalendarContract.Events.HAS_ALARM,               // 在表中的列索引14
                CalendarContract.Events.RRULE,                   // 在表中的列索引15
                CalendarContract.Events.RDATE,                   // 在表中的列索引16
                CalendarContract.Events.HAS_ATTENDEE_DATA,       // 在表中的列索引17
                CalendarContract.Events.LAST_DATE,               // 在表中的列索引18
                CalendarContract.Events.ORGANIZER,               // 在表中的列索引19
                CalendarContract.Events.IS_ORGANIZER,            // 在表中的列索引20
                CalendarContract.Events._ID                      // 在表中的列索引21
        };

        // 事件匹配
        Uri uri = CalendarContract.Events.CONTENT_URI;
        Uri uri2 = CalendarContract.Reminders.CONTENT_URI;

        String selection = "(" + CalendarContract.Events.CALENDAR_ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(calID)};

        Cursor cursor;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(
                    "android.permission.READ_CALENDAR")) {
                cursor = context.getContentResolver().query(uri, EVENT_PROJECTION, selection,
                        selectionArgs, null);
            } else {
                return null;
            }
        } else {
            cursor = context.getContentResolver().query(uri, EVENT_PROJECTION, selection,
                    selectionArgs, null);
        }

        if (null == cursor) {
            return null;
        }

        // 查询结果
        List<CalendarEvent> result = new ArrayList<>();

        // 开始查询数据
        if (cursor.moveToFirst()) {
            do {
                CalendarEvent calendarEvent = new CalendarEvent();
                result.add(calendarEvent);
                calendarEvent.setId(cursor.getLong(cursor.getColumnIndex(
                        CalendarContract.Events._ID)));
                calendarEvent.setCalID(cursor.getLong(cursor.getColumnIndex(
                        CalendarContract.Events.CALENDAR_ID)));
                calendarEvent.setTitle(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.TITLE)));
                calendarEvent.setDescription(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.DESCRIPTION)));
                calendarEvent.setEventLocation(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.EVENT_LOCATION)));
                calendarEvent.setDisplayColor(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.DISPLAY_COLOR)));
                calendarEvent.setStatus(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.STATUS)));
                calendarEvent.setStart(cursor.getLong(cursor.getColumnIndex(
                        CalendarContract.Events.DTSTART)));
                calendarEvent.setEnd(cursor.getLong(cursor.getColumnIndex(
                        CalendarContract.Events.DTEND)));
                calendarEvent.setDuration(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.DURATION)));
                calendarEvent.setEventTimeZone(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.EVENT_TIMEZONE)));
                calendarEvent.setEventEndTimeZone(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.EVENT_END_TIMEZONE)));
                calendarEvent.setAllDay(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.ALL_DAY)));
                calendarEvent.setAccessLevel(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.ACCESS_LEVEL)));
                calendarEvent.setAvailability(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.AVAILABILITY)));
                calendarEvent.setHasAlarm(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.HAS_ALARM)));
                calendarEvent.setRRule(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.RRULE)));
                calendarEvent.setRDate(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.RDATE)));
                calendarEvent.setHasAttendeeData(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.HAS_ATTENDEE_DATA)));
                calendarEvent.setLastDate(cursor.getInt(cursor.getColumnIndex(
                        CalendarContract.Events.LAST_DATE)));
                calendarEvent.setOrganizer(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.ORGANIZER)));
                calendarEvent.setIsOrganizer(cursor.getString(cursor.getColumnIndex(
                        CalendarContract.Events.IS_ORGANIZER)));


                // ----------------------- 开始查询事件提醒 ------------------------------
                String[] REMINDER_PROJECTION = new String[]{
                        CalendarContract.Reminders._ID,                     // 在表中的列索引0
                        CalendarContract.Reminders.EVENT_ID,                // 在表中的列索引1
                        CalendarContract.Reminders.MINUTES,                 // 在表中的列索引2
                        CalendarContract.Reminders.METHOD,                  // 在表中的列索引3
                };
                String selection2 = "(" + CalendarContract.Reminders.EVENT_ID + " = ?)";
                String[] selectionArgs2 = new String[]{String.valueOf(calendarEvent.getId())};

                try (Cursor reminderCursor = context.getContentResolver().query(uri2, REMINDER_PROJECTION,
                        selection2, selectionArgs2, null)) {
                    if (null != reminderCursor) {
                        if (reminderCursor.moveToFirst()) {
                            List<CalendarEvent.EventReminders> reminders = new ArrayList<>();
                            do {
                                CalendarEvent.EventReminders reminders1 = new CalendarEvent.EventReminders();
                                reminders.add(reminders1);
                                reminders1.setReminderId(reminderCursor.getLong(
                                        reminderCursor.getColumnIndex(CalendarContract.Reminders._ID)));
                                reminders1.setReminderEventID(reminderCursor.getLong(
                                        reminderCursor.getColumnIndex(CalendarContract.Reminders.EVENT_ID)));
                                reminders1.setReminderMinute(reminderCursor.getInt(
                                        reminderCursor.getColumnIndex(CalendarContract.Reminders.MINUTES)));
                                reminders1.setReminderMethod(reminderCursor.getInt(
                                        reminderCursor.getColumnIndex(CalendarContract.Reminders.METHOD)));
                            } while (reminderCursor.moveToNext());
                            calendarEvent.setReminders(reminders);
                        }
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return result;
    }

    /**
     * 判断日历账户中是否已经存在此事件
     *
     * @param begin 事件开始时间
     * @param end   事件结束时间
     * @param title 事件标题
     */
    public static boolean isEventAlreadyExist(Context context, long begin, long end, String title) {
        String[] projection = new String[]{
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.TITLE
        };

        Cursor cursor = CalendarContract.Instances.query(
                context.getContentResolver(), projection, begin, end, title);

        return null != cursor && cursor.moveToFirst()
                && cursor.getString(
                cursor.getColumnIndex(CalendarContract.Instances.TITLE)).equals(title);
    }


    // ------------------------------- 日历事件相关 -----------------------------------

    /**
     * 组装日历事件
     */
    private static void setupEvent(CalendarEvent calendarEvent, ContentValues event) {
        // 事件开始时间
        event.put(CalendarContract.Events.DTSTART, calendarEvent.getStart());
        // 事件结束时间
        event.put(CalendarContract.Events.DTEND, calendarEvent.getEnd());
        // 事件标题
        event.put(CalendarContract.Events.TITLE, calendarEvent.getTitle());
        // 事件描述(对应手机系统日历备注栏)
        event.put(CalendarContract.Events.DESCRIPTION, calendarEvent.getDescription());
        // 事件地点
        event.put(CalendarContract.Events.EVENT_LOCATION, calendarEvent.getEventLocation());
        // 事件时区
        event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        // 定义事件的显示，默认即可
        event.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT);
        // 事件的状态
        event.put(CalendarContract.Events.STATUS, 0);
        // 设置事件提醒警报可用
        event.put(CalendarContract.Events.HAS_ALARM, 1);
        // 设置事件忙
        event.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        if (null != calendarEvent.getRRule()) {
            // 设置事件重复规则
            event.put(CalendarContract.Events.RRULE,
                    getFullRRuleForRRule(calendarEvent.getRRule(),
                            calendarEvent.getStart(), calendarEvent.getEnd()));
        }
    }

    /**
     * 获取完整的重复规则(包含终止时间)
     *
     * @param rRule     重复规则
     * @param beginTime 开始时间
     * @param endTime   结束时间
     */
    private static String getFullRRuleForRRule(String rRule, long beginTime, long endTime) {
        builder.delete(0, builder.length());

        switch (rRule) {
            case RRuleConstant.REPEAT_WEEKLY_BY_MO:
            case RRuleConstant.REPEAT_WEEKLY_BY_TU:
            case RRuleConstant.REPEAT_WEEKLY_BY_WE:
            case RRuleConstant.REPEAT_WEEKLY_BY_TH:
            case RRuleConstant.REPEAT_WEEKLY_BY_FR:
            case RRuleConstant.REPEAT_WEEKLY_BY_SA:
            case RRuleConstant.REPEAT_WEEKLY_BY_SU:
                return builder.append(rRule).append(Util.getFinalRRuleMode(endTime)).toString();
            case RRuleConstant.REPEAT_CYCLE_WEEKLY:
                return builder.append(rRule).append(Util.getWeekForDate(beginTime)).append("; UNTIL = ")
                        .append(Util.getFinalRRuleMode(endTime)).toString();
            case RRuleConstant.REPEAT_CYCLE_MONTHLY:
                return builder.append(rRule).append(Util.getDayOfMonth(beginTime))
                        .append("; UNTIL = ").append(Util.getFinalRRuleMode(endTime)).toString();
            default:
                return rRule;
        }
    }


    // ------------------------------- 通过Intent启动系统日历 -----------------------------------

    /*
        日历的Intent对象：

           动作                  描述                         附加功能
        ACTION_VIEW        打开指定时间的日历                    无
        ACTION_VIEW        查看由EVENT_ID指定的事件        开始时间，结束时间
        ACTION_EDIT        编辑由EVENT_ID指定的事件        开始时间，结束时间
        ACTION_INSERT      创建一个事件                         所有


        Intent对象的附加功能：

        Events.TITLE                                        事件标题
        CalendarContract.EXTRA_EVENT_BEGIN_TIME             开始时间
        CalendarContract.EXTRA_EVENT_END_TIME               结束时间
        CalendarContract.EXTRA_EVENT_ALL_DAY                是否全天
        Events.EVENT_LOCATION                               事件地点
        Events.DESCRIPTION                                  事件描述
        Intent.EXTRA_EMALL                                  受邀者电子邮件,用逗号分隔
        Events.RRULE                                        事件重复规则
        Events.ACCESS_LEVEL                                 事件私有还是公有
        Events.AVAILABILITY                                 预定事件是在忙时计数还是闲时计数
     */

    /**
     * 通过Intent启动系统日历新建事件界面插入新的事件
     * <p>
     * TIP: 这将不再需要声明读写日历数据的权限
     *
     * @param beginTime 事件开始时间
     * @param endTime   事件结束时间
     * @param title     事件标题
     * @param des       事件描述
     * @param location  事件地点
     * @param isAllDay  事件是否全天
     */
    public static void startCalendarForIntentToInsert(Context context, long beginTime, long endTime,
                                                      String title, String des, String location,
                                                      boolean isAllDay) {
        checkCalendarAccount(context);


        // FIXME: 2019/3/6 VIVO手机无法打开界面，找不到对应的Activity  com.bbk.calendar
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                .putExtra(CalendarContract.Events.ALL_DAY, isAllDay)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, des)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location);

        if (null != intent.resolveActivity(context.getPackageManager())) {
            context.startActivity(intent);
        }
    }

    /**
     * 通过Intent启动系统日历来编辑指定ID的事件
     * <p>
     *
     * @param eventID 要编辑的事件ID
     */
    public static void startCalendarForIntentToEdit(Context context, long eventID) {
        checkCalendarAccount(context);

        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
        Intent intent = new Intent(Intent.ACTION_EDIT).setData(uri);

        if (null != intent.resolveActivity(context.getPackageManager())) {
            context.startActivity(intent);
        }
    }

    /**
     * 通过Intent启动系统日历来查看指定ID的事件
     *
     * @param eventID 要查看的事件ID
     */
    public static void startCalendarForIntentToView(Context context, long eventID) {
        checkCalendarAccount(context);

        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);

        if (null != intent.resolveActivity(context.getPackageManager())) {
            context.startActivity(intent);
        }
    }


    // ----------------------------- 日历账户名相关设置 -----------------------------------

    public static String getCalendarName() {
        return CALENDAR_NAME;
    }

    public static void setCalendarName(String calendarName) {
        CALENDAR_NAME = calendarName;
    }

    public static String getCalendarAccountName() {
        return CALENDAR_ACCOUNT_NAME;
    }

    public static void setCalendarAccountName(String calendarAccountName) {
        CALENDAR_ACCOUNT_NAME = calendarAccountName;
    }

    public static String getCalendarDisplayName() {
        return CALENDAR_DISPLAY_NAME;
    }

    public static void setCalendarDisplayName(String calendarDisplayName) {
        CALENDAR_DISPLAY_NAME = calendarDisplayName;
    }

}
