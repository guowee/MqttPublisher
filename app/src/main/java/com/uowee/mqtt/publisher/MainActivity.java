package com.uowee.mqtt.publisher;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.uowee.mqtt.service.MqttService;

import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements MqttServiceDelegate.MessageHandler, MqttServiceDelegate.StatusHandler {
    private static final Logger LOG = Logger.getLogger(MainActivity.class);

    private MqttServiceDelegate.MessageReceiver msgReceiver;
    private MqttServiceDelegate.StatusReceiver statusReceiver;

    private TextView timestampView, topicView, messageView, statusView;

    private EditText publishEditView;
    private Button publishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timestampView = findViewById(R.id.timestampView);
        topicView = findViewById(R.id.topicView);
        messageView = findViewById(R.id.messageView);
        statusView = findViewById(R.id.statusView);

        publishEditView = findViewById(R.id.publishEditView);
        publishButton = findViewById(R.id.publishButton);

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MqttServiceDelegate.publish(
                        MainActivity.this,
                        "test-topic",
                        publishEditView.getText().toString().getBytes());
            }
        });

        bindStatusReceiver();
        bindMessageReceiver();
        //Start service if not started
        MqttServiceDelegate.startService(this);
    }


    private void bindMessageReceiver() {
        msgReceiver = new MqttServiceDelegate.MessageReceiver();
        msgReceiver.registerHandler(this);
        registerReceiver(msgReceiver,
                new IntentFilter(MqttService.MQTT_MSG_RECEIVED_INTENT));
    }

    private void unbindMessageReceiver() {
        if (msgReceiver != null) {
            msgReceiver.unregisterHandler(this);
            unregisterReceiver(msgReceiver);
            msgReceiver = null;
        }
    }

    private void bindStatusReceiver() {
        statusReceiver = new MqttServiceDelegate.StatusReceiver();
        statusReceiver.registerHandler(this);
        registerReceiver(statusReceiver,
                new IntentFilter(MqttService.MQTT_STATUS_INTENT));
    }

    private void unbindStatusReceiver() {
        if (statusReceiver != null) {
            statusReceiver.unregisterHandler(this);
            unregisterReceiver(statusReceiver);
            statusReceiver = null;
        }
    }

    private String getCurrentTimestamp() {
        return new Timestamp(new Date().getTime()).toString();
    }

    @Override
    public void handleMessage(String topic, byte[] payload) {
        String message = new String(payload);

        LOG.debug("handleMessage: topic=" + topic + ", message=" + message);

        if (timestampView != null) timestampView.setText("When: " + getCurrentTimestamp());
        if (topicView != null) topicView.setText("Topic: " + topic);
        if (messageView != null) messageView.setText("Message: " + message);
    }

    @Override
    public void handleStatus(MqttService.ConnectionStatus status, String reason) {
        LOG.debug("handleStatus: status=" + status + ", reason=" + reason);
        if (statusView != null)
            statusView.setText("Status: " + status.toString() + " (" + reason + ")");
    }


}


