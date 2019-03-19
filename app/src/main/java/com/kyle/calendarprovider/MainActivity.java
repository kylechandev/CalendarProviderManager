package com.kyle.calendarprovider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kyle.calendarprovider.calendar.CalendarEvent;
import com.kyle.calendarprovider.calendar.CalendarProviderManager;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_main_add)
    Button btnMainAdd;
    @BindView(R.id.btn_main_delete)
    Button btnMainDelete;
    @BindView(R.id.btn_main_update)
    Button btnMainUpdate;
    @BindView(R.id.btn_main_query)
    Button btnMainQuery;
    @BindView(R.id.tv_event)
    TextView tvEvent;
    @BindView(R.id.btn_edit)
    Button btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR,
                            Manifest.permission.READ_CALENDAR}, 1);
        }
    }

    @OnClick({R.id.btn_main_add, R.id.btn_main_delete, R.id.btn_edit,
            R.id.btn_main_update, R.id.btn_main_query})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_main_add:
                CalendarEvent calendarEvent = new CalendarEvent(
                        "马上吃饭",
                        "吃好吃的",
                        "南信院二食堂",
                        System.currentTimeMillis(),
                        System.currentTimeMillis() + 60000,
                        0, null
                );

                // 添加事件
                int result = CalendarProviderManager.addCalendarEvent(this, calendarEvent);
                if (result == 0) {
                    Toast.makeText(this, "插入成功", Toast.LENGTH_SHORT).show();
                } else if (result == -1) {
                    Toast.makeText(this, "插入失败", Toast.LENGTH_SHORT).show();
                } else if (result == -2) {
                    Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_main_delete:
                // 删除事件
                long calID2 = CalendarProviderManager.obtainCalendarAccountID(this);
                List<CalendarEvent> events2 = CalendarProviderManager.queryAccountEvent(this, calID2);
                if (null != events2) {
                    if (events2.size() == 0) {
                        Toast.makeText(this, "没有事件可以删除", Toast.LENGTH_SHORT).show();
                    } else {
                        long eventID = events2.get(0).getId();
                        int result2 = CalendarProviderManager.deleteCalendarEvent(this, eventID);
                        if (result2 == -2) {
                            Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "查询失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_main_update:
                // 更新事件
                long calID = CalendarProviderManager.obtainCalendarAccountID(this);
                List<CalendarEvent> events = CalendarProviderManager.queryAccountEvent(this, calID);
                if (null != events) {
                    if (events.size() == 0) {
                        Toast.makeText(this, "没有事件可以更新", Toast.LENGTH_SHORT).show();
                    } else {
                        long eventID = events.get(0).getId();
                        int result3 = CalendarProviderManager.updateCalendarEventTitle(
                                this, eventID, "改吃晚饭的房间第三方监督司法");
                        if (result3 == 1) {
                            Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "查询失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_main_query:
                // 查询事件
                long calID4 = CalendarProviderManager.obtainCalendarAccountID(this);
                List<CalendarEvent> events4 = CalendarProviderManager.queryAccountEvent(this, calID4);
                StringBuilder stringBuilder4 = new StringBuilder();
                if (null != events4) {
                    for (CalendarEvent event : events4) {
                        stringBuilder4.append(events4.toString()).append("\n");
                    }
                    tvEvent.setText(stringBuilder4.toString());
                    Toast.makeText(this, "查询成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "查询失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_edit:
                // 启动系统日历进行编辑事件
                CalendarProviderManager.startCalendarForIntentToInsert(this, System.currentTimeMillis(),
                        System.currentTimeMillis() + 60000, "哈", "哈哈哈哈", "蒂埃纳",
                        false);
                break;
            default:
                break;
        }
    }

}
