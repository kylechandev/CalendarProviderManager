package com.kyle.calendarprovider.calendar;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * 系统日历工具
 * <p>
 * Created by KYLE on 2019/3/3 - 20:37
 */
public class CalendarProviderManager {

    /*
       TIP: 要向系统日历插入事件,前提系统中必须存在至少1个日历账户
     */


    // ----------------------- 创建日历账户时账户名使用 ---------------------------
    private static String CALENDAR_NAME = "KyleC";
    private static String CALENDAR_ACCOUNT_NAME = "KYLE";
    private static String CALENDAR_DISPLAY_NAME = "KYLE的账户";


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
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission("android.permission.WRITE_CALENDAR")) {
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
     * 删除创建的日历
     *
     * @return -2: permission deny  0: No designated account  1: delete success
     */
    public static int deleteCalendarAccountByName(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        int deleteCount;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[]{CALENDAR_ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission("android.permission.WRITE_CALENDAR")) {
                deleteCount = context.getContentResolver().delete(uri, selection, selectionArgs);
            } else {
                return -2;
            }
        } else {
            deleteCount = context.getContentResolver().delete(uri, selection, selectionArgs);
        }

        return deleteCount;
    }

    /**
     * 添加日历事件
     *
     * @param eventTitle    事件标题
     * @param eventDes      事件描述
     * @param eventLocation 事件地点
     * @param startTime     事件开始时间
     * @param endTime       事件结束时间
     * @param advanceTime   事件提醒提前时间(If you don't need to remind the incoming parameters -2)
     * @return 0: success  -1: failed  -2: permission deny
     * @see AdvanceTime 提醒时间
     */
    public static int addCalendarEvent(Context context, String eventTitle, String eventDes,
                                       String eventLocation, long startTime, long endTime, int advanceTime) {
         /*
            TIP: 插入一个新事件的规则：
             1.  必须包含CALENDAR_ID和DTSTART字段
             2.  必须包含EVENT_TIMEZONE字段,使用TimeZone.getDefault().getID()方法获取默认时区
             3.  对于非重复发生的事件,必须包含DTEND字段
             4.  对重复发生的事件,必须包含一个附加了RRULE或RDATE字段的DURATION字段
         */

        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

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
        setupEvent(startTime, endTime, eventTitle, eventDes, eventLocation, event);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 判断权限
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission("android.permission.WRITE_CALENDAR")) {
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


        if (-2 != advanceTime) {
            // 获取事件ID
            long eventID = ContentUris.parseId(eventUri);

            // 开始组装事件提醒数据
            ContentValues reminders = new ContentValues();
            // 此提醒所对应的事件ID
            reminders.put(CalendarContract.Reminders.EVENT_ID, eventID);
            // 设置提醒提前的时间(0：准时  -1：使用系统默认)
            reminders.put(CalendarContract.Reminders.MINUTES, advanceTime);
            // 设置事件提醒方式为通知警报
            reminders.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            reminderUri = context.getContentResolver().insert(uri2, reminders);

            if (null == reminderUri) {
                return -1;
            }
        }

        return 0;
    }

    /**
     * 更新指定ID的日历事件
     *
     * @param eventID       日历事件ID
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param eventTitle    事件标题
     * @param eventDes      事件描述
     * @param eventLocation 事件地点
     * @param advanceTime   事件提醒时间
     * @return -2: permission deny  else success
     * @see AdvanceTime 提醒时间
     */
    public static int updateCalendarEvent(Context context, long eventID, long startTime, long endTime,
                                          String eventTitle, String eventDes, String eventLocation,
                                          long advanceTime) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        int updatedCount1;

        Uri uri1 = CalendarContract.Events.CONTENT_URI;
        Uri uri2 = CalendarContract.Reminders.CONTENT_URI;

        ContentValues event = new ContentValues();
        setupEvent(startTime, endTime, eventTitle, eventDes, eventLocation, event);

        // 更新匹配条件
        String selection1 = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs1 = new String[]{String.valueOf(eventID)};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission("android.permission.WRITE_CALENDAR")) {
                updatedCount1 = context.getContentResolver().update(uri1, event, selection1, selectionArgs1);
            } else {
                return -2;
            }
        } else {
            updatedCount1 = context.getContentResolver().update(uri1, event, selection1, selectionArgs1);
        }


        ContentValues reminders = new ContentValues();
        reminders.put(CalendarContract.Reminders.MINUTES, advanceTime);
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
    public static int updateCalendarEventStartTime(Context context, long eventID, long startTime) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DTSTART, startTime);

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
    public static int updateCalendarEventEndTime(Context context, long eventID, long endTime) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DTEND, endTime);


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
    public static int updateCalendarEventTime(Context context, long eventID, long startTime, long endTime) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DTSTART, startTime);
        event.put(CalendarContract.Events.DTEND, endTime);


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
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

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
    public static int updateCalendarEventDes(Context context, long eventID, String eventDes) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.DESCRIPTION, eventDes);


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
    public static int updateCalendarEventLocation(Context context, long eventID, String eventLocation) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.EVENT_LOCATION, eventLocation);


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
    public static int updateCalendarEventTitAndDes(Context context, long eventID, String eventTitle, String eventDes) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, eventTitle);
        event.put(CalendarContract.Events.DESCRIPTION, eventDes);


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
    public static int updateCalendarEventCommonInfo(Context context, long eventID, String eventTitle,
                                                    String eventDes, String eventLocation) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Events.CONTENT_URI;

        // 新的数据
        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, eventTitle);
        event.put(CalendarContract.Events.DESCRIPTION, eventDes);
        event.put(CalendarContract.Events.EVENT_LOCATION, eventLocation);


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
    private static int updateCalendarEventReminder(Context context, long eventID, long advanceTime) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        Uri uri = CalendarContract.Reminders.CONTENT_URI;

        ContentValues reminders = new ContentValues();
        reminders.put(CalendarContract.Reminders.MINUTES, advanceTime);
        reminders.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

        // 更新匹配条件
        String selection2 = "(" + CalendarContract.Reminders.EVENT_ID + " = ?)";
        String[] selectionArgs2 = new String[]{String.valueOf(eventID)};

        return context.getContentResolver().update(uri, reminders, selection2, selectionArgs2);
    }

    /**
     * 删除日历事件
     *
     * @param eventID 事件ID
     * @return -2: permission deny  else success
     */
    public static int deleteCalendarEvent(Context context, long eventID) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

        int deletedCount1;
        Uri uri1 = CalendarContract.Events.CONTENT_URI;
        Uri uri2 = CalendarContract.Reminders.CONTENT_URI;

        // 删除匹配条件
        String selection = "(" + CalendarContract.Events._ID + " = ?)";
        String[] selectionArgs = new String[]{String.valueOf(eventID)};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission("android.permission.WRITE_CALENDAR")) {
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

    /**
     * 查询指定日历账户下的所有事件
     *
     * @return If failed return null else return List<CalendarEvent>
     */
    public static List<CalendarEvent> queryAccountEvent(Context context, long calID) {
        if (null == context) {
            throw new IllegalArgumentException("context can not be null");
        }

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
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission("android.permission.READ_CALENDAR")) {
                cursor = context.getContentResolver().query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
            } else {
                return null;
            }
        } else {
            cursor = context.getContentResolver().query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
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
                calendarEvent.setId(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID)));
                calendarEvent.setCalID(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID)));
                calendarEvent.setTitle(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE)));
                calendarEvent.setDescription(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)));
                calendarEvent.setEventLocation(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)));
                calendarEvent.setDisplayColor(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.DISPLAY_COLOR)));
                calendarEvent.setStatus(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.STATUS)));
                calendarEvent.setStart(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART)));
                calendarEvent.setEnd(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTEND)));
                calendarEvent.setDuration(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DURATION)));
                calendarEvent.setEventTimeZone(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE)));
                calendarEvent.setEventEndTimeZone(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_END_TIMEZONE)));
                calendarEvent.setAllDay(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)));
                calendarEvent.setAccessLevel(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.ACCESS_LEVEL)));
                calendarEvent.setAvailability(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.AVAILABILITY)));
                calendarEvent.setHasAlarm(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.HAS_ALARM)));
                calendarEvent.setRRule(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.RRULE)));
                calendarEvent.setRDate(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.RDATE)));
                calendarEvent.setHasAttendeeData(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.HAS_ATTENDEE_DATA)));
                calendarEvent.setLastDate(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.LAST_DATE)));
                calendarEvent.setOrganizer(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ORGANIZER)));
                calendarEvent.setIsOrganizer(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.IS_ORGANIZER)));


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
     * 组装日历事件
     *
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param eventTitle    事件标题
     * @param eventDes      事件描述
     * @param eventLocation 事件地点
     * @param event         组装的事件
     */
    private static void setupEvent(long startTime, long endTime, String eventTitle, String eventDes,
                                   String eventLocation, ContentValues event) {
        // 事件开始时间
        event.put(CalendarContract.Events.DTSTART, startTime);
        // 事件结束时间
        event.put(CalendarContract.Events.DTEND, endTime);
        // 事件标题
        event.put(CalendarContract.Events.TITLE, eventTitle);
        // 事件描述(对应手机系统日历备注栏)
        event.put(CalendarContract.Events.DESCRIPTION, eventDes);
        // 事件地点
        event.put(CalendarContract.Events.EVENT_LOCATION, eventLocation);
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
        // 设置事件重复规则
        // event.put(CalendarContract.Events.RRULE, );
    }

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
