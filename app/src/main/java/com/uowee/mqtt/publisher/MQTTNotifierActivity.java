package com.uowee.mqtt.publisher;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.uowee.mqtt.service.MqttService;


public class MQTTNotifierActivity extends AppCompatActivity {
    private StatusUpdateReceiver statusUpdateIntentReceiver;
    private MQTTMessageReceiver messageIntentReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.cancel(MqttService.MQTT_NOTIFICATION_UPDATE);
        }
    }

    public class StatusUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();
            String newStatus = notificationData.getString(MqttService.MQTT_STATUS_MSG);

            //...
        }
    }

    public class MQTTMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();
            String newTopic = notificationData.getString(MqttService.MQTT_MSG_RECEIVED_TOPIC);
            String newData = notificationData.getString(MqttService.MQTT_MSG_RECEIVED_MSG);

            // ...
        }
    }

}
