package com.uowee.mqtt.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.uowee.mqtt.impl.MqttConnectOptions;
import com.uowee.mqtt.impl.MqttException;
import com.uowee.mqtt.impl.MqttMessage;
import com.uowee.mqtt.impl.MqttPersistenceException;
import com.uowee.mqtt.impl.MqttTopic;
import com.uowee.mqtt.impl.paho.PahoMqttClientFactory;
import com.uowee.mqtt.interfaces.IMqttCallback;
import com.uowee.mqtt.interfaces.IMqttClient;
import com.uowee.mqtt.interfaces.IMqttClientFactory;
import com.uowee.mqtt.interfaces.IMqttConnectOptions;
import com.uowee.mqtt.interfaces.IMqttMessage;
import com.uowee.mqtt.interfaces.IMqttPersistence;
import com.uowee.mqtt.interfaces.IMqttTopic;
import com.uowee.mqtt.logging.ConfigureLog4J;
import com.uowee.mqtt.publisher.MQTTNotifierActivity;
import com.uowee.mqtt.publisher.R;

import org.apache.log4j.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MqttService extends Service implements IMqttCallback {
    @Nullable
    private static final Logger LOG = Logger.getLogger(MqttService.class);

    public static final int MAX_MQTT_CLIENTID_LENGTH = 22;
    public static final String MQTT_STATUS_INTENT = "com.qonect.services.mqtt.STATUS";
    public static final String MQTT_STATUS_CODE = "com.qonect.services.mqtt.STATUS_CODE";
    public static final String MQTT_STATUS_MSG = "com.qonect.services.mqtt.STATUS_MSG";
    public static final String MQTT_PING_ACTION = "com.qonect.services.mqtt.PING";

    public static final String MQTT_PUBLISH_MSG_INTENT = "com.qonect.services.mqtt.SENDMSG";
    public static final String MQTT_PUBLISH_MSG_TOPIC = "com.qonect.services.mqtt.SENDMSG_TOPIC";
    public static final String MQTT_PUBLISH_MSG = "com.qonect.services.mqtt.SENDMSG_MSG";

    public static final String MQTT_MSG_RECEIVED_INTENT = "com.qonect.services.mqtt.MSGRECVD";
    public static final String MQTT_MSG_RECEIVED_TOPIC = "com.qonect.services.mqtt.MSGRECVD_TOPIC";
    public static final String MQTT_MSG_RECEIVED_MSG = "com.qonect.services.mqtt.MSGRECVD_MSG";


    // constants used by status bar notifications
    public static final int MQTT_NOTIFICATION_ONGOING = 1;
    public static final int MQTT_NOTIFICATION_UPDATE = 2;


    private String brokerHostName = "";
    private int brokerPortNumber = 61613;
    private String username = "admin";
    private char[] password = "password".toCharArray();
    private boolean cleanStart = false;
    private short keepAliveSeconds = 20 * 60;
    private IMqttPersistence usePersistence = null;
    private String mqttClientId = null;
    private List<IMqttTopic> topics = new ArrayList<IMqttTopic>();

    private ConnectionStatus connectionStatus = ConnectionStatus.INITIAL;
    private Timestamp connectionStatusChangeTime;


    private IMqttClient mqttClient = null;
    private IMqttClientFactory mqttClientFactory;

    private ExecutorService executor;
    private NetworkConnectionIntentReceiver netConnReceiver;
    private PingSender pingSender;


    private LocalBinder mBinder;


    public enum ConnectionStatus {
        INITIAL,
        CONNECTING,
        CONNECTED,
        NOTCONNECTED_WAITINGFORINTERNET,
        NOTCONNECTED_USERDISCONNECT,
        NOTCONNECTED_DATADISABLED,
        NOTCONNECTED_UNKNOWNREASON
    }


    public class LocalBinder<S> extends Binder {
        private WeakReference<S> mService;

        public LocalBinder(S service) {
            mService = new WeakReference<>(service);
        }

        public S getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initLog();
        changeStatus(ConnectionStatus.INITIAL);
        mBinder = new LocalBinder<MqttService>(this);
        brokerHostName = "192.168.6.210";
        topics.add(new MqttTopic("test-topic"));

        mqttClientFactory = new PahoMqttClientFactory();
        executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doStart(intent, startId);
        return START_STICKY;
    }

    private void doStart(final Intent intent, final int startId) {
        initMqttClient();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        });
    }

    private void initLog() {
        File backupPath = Environment.getExternalStorageDirectory();
        backupPath = new File(backupPath.getPath() + "/log");
        if (!backupPath.exists()) {
            backupPath.mkdirs();
        }
        backupPath = new File(backupPath.getPath(), "MqttService.csv");
        ConfigureLog4J.configure(backupPath.getPath(), ConfigureLog4J.PATTERN_CSV);
        LOG.debug("initLog: Logging to [" + backupPath.getPath() + "]");
    }

    private void changeStatus(ConnectionStatus status) {
        connectionStatus = status;
        connectionStatusChangeTime = new Timestamp(new Date().getTime());
    }

    private void initMqttClient() {
        if (mqttClient != null) {
            return;
        }
        try {
            mqttClient = mqttClientFactory.create(brokerHostName, brokerPortNumber, getClientId(), usePersistence);
            mqttClient.setCallback(this);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private String getClientId() {
        if (mqttClientId == null) {
            String android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            mqttClientId = android_id;

            if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
                mqttClientId = mqttClientId.substring(0, MAX_MQTT_CLIENTID_LENGTH);
            }
        }

        return mqttClientId;
    }


    synchronized void handleStart(Intent intent, int startId) {
        if (mqttClient == null) {
            stopSelf();
            return;
        }

        if (connectionStatus == ConnectionStatus.NOTCONNECTED_USERDISCONNECT) {
            return;
        }

        if (!isBackgroundDataEnabled()) {
            changeStatus(ConnectionStatus.NOTCONNECTED_DATADISABLED);
            broadcastServiceStatus("Not connected - background data disabled # " + getConnectionChangeTimestamp());
            return;
        }

        if (!isConnected()) {
            changeStatus(ConnectionStatus.CONNECTING);
            if (isOnline()) {
                if (connectToBroker()) {
                    onConnect();
                }
            } else {
                changeStatus(ConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
                broadcastServiceStatus("Waiting for network connection # " + getConnectionChangeTimestamp());
            }
        }

        if (netConnReceiver == null) {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            registerReceiver(netConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        if (pingSender == null) {
            pingSender = new PingSender();
            registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
        }

        if (!handleStartAction(intent)) {
            rebroadcastStatus();
        }
    }


    private void broadcastServiceStatus(String statusDescription) {

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_STATUS_INTENT);
        broadcastIntent.putExtra(MQTT_STATUS_CODE, connectionStatus.ordinal());
        broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastReceivedMessage(String topic, byte[] message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
        sendBroadcast(broadcastIntent);
    }

    private boolean isConnected() {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }

    private boolean isBackgroundDataEnabled() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return cm.getBackgroundDataSetting();
        }
        return isOnline();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isAvailable() && netInfo.isConnected();
    }

    private boolean handleStartAction(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }

        if (action.equalsIgnoreCase(MQTT_PUBLISH_MSG_INTENT)) {
            LOG.debug("handleStartAction: action == MQTT_PUBLISH_MSG_INTENT");
            handlePublishMessageIntent(intent);
        }
        return true;
    }

    private void handlePublishMessageIntent(Intent intent) {
        LOG.debug("handlePublishMessageIntent: intent=" + intent);
        boolean isOnline = isOnline();
        boolean isConnected = isConnected();
        if (!isConnected || !isOnline) {
            LOG.error("handlePublishMessageIntent: isOnline()=" + isOnline + ", isConnected()=" + isConnected);
            return;
        }

        byte[] payload = intent.getByteArrayExtra(MQTT_PUBLISH_MSG);
        try {
            mqttClient.publish(new MqttTopic("test-topic"), new MqttMessage(payload));
        } catch (MqttException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

    }


    @Override
    public void onDestroy() {
        disconnectFromBroker();
        broadcastServiceStatus("Disconnected @ " + getConnectionChangeTimestamp());
        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }
        super.onDestroy();
    }

    public void disconnect() {
        disconnectFromBroker();
        changeStatus(ConnectionStatus.NOTCONNECTED_USERDISCONNECT);
        broadcastServiceStatus("Disconnected");
    }


    public void rebroadcastStatus() {
        String status = "";
        switch (connectionStatus) {
            case INITIAL:
                status = "Please wait";
                break;
            case CONNECTING:
                status = "Connecting @ " + getConnectionChangeTimestamp();
                break;
            case CONNECTED:
                status = "Connected @ " + getConnectionChangeTimestamp();
                break;
            case NOTCONNECTED_UNKNOWNREASON:
                status = "Not connected - waiting for network connection @ " + getConnectionChangeTimestamp();
                break;
            case NOTCONNECTED_USERDISCONNECT:
                status = "Disconnected @ " + getConnectionChangeTimestamp();
                break;
            case NOTCONNECTED_DATADISABLED:
                status = "Not connected - background data disabled @ " + getConnectionChangeTimestamp();
                break;
            case NOTCONNECTED_WAITINGFORINTERNET:
                status = "Unable to connect @ " + getConnectionChangeTimestamp();
                break;
        }

        broadcastServiceStatus(status);
    }


    private void disconnectFromBroker() {
        if (netConnReceiver != null) {
            unregisterReceiver(netConnReceiver);
            netConnReceiver = null;
        }
        if (pingSender != null) {
            unregisterReceiver(pingSender);
            pingSender = null;
        }
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
            LOG.error("disconnect failed - mqtt exception", e);
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
            LOG.error("disconnect failed - persistence exception", e);
        } finally {
            mqttClient = null;
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
    }


    protected void onConnect() {
        subscribeToTopics();
    }

    private void subscribeToTopics() {
        LOG.debug("subscribeToTopics");

        boolean subscribed = false;

        if (!isConnected()) {
            LOG.error("Unable to subscribe as we are not connected");
        } else {
            try {
                mqttClient.subscribe(topics.toArray(new IMqttTopic[topics.size()]));

                subscribed = true;
            } catch (IllegalArgumentException e) {
                LOG.error("subscribe failed - illegal argument", e);
            } catch (MqttException e) {
                LOG.error("subscribe failed - MQTT exception", e);
            }
        }

        if (subscribed == false) {
            broadcastServiceStatus("Unable to subscribe @ " + getConnectionChangeTimestamp());
            notifyUser("Unable to subscribe", "MQTT", "Unable to subscribe");
        }
    }

    private boolean connectToBroker() {
        try {
            IMqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(cleanStart);
            options.setKeepAliveInterval(keepAliveSeconds);
            options.setUserName(username);
            options.setPassword(password);
            mqttClient.connect(options);
            changeStatus(ConnectionStatus.CONNECTED);
            broadcastServiceStatus("Connected # " + getConnectionChangeTimestamp());
            scheduleNextPing();
            return true;
        } catch (MqttException e) {
            changeStatus(ConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
            broadcastServiceStatus("Unable to connect # " + getConnectionChangeTimestamp());
            notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");
            scheduleNextPing();
            return false;
        }
    }

    private void scheduleNextPing() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
    }


    private void notifyUser(String alert, String title, String body) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this)
                /**设置通知左边的大图标**/
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                /**设置通知右边的小图标**/
                .setSmallIcon(R.mipmap.ic_launcher)
                /**通知首次出现在通知栏，带上升动画效果的**/
                .setTicker(alert)
                /**设置通知的标题**/
                .setContentTitle(title)
                /**设置通知的内容**/
                .setContentText(body)
                /**通知产生的时间，会在通知信息里显示**/
                .setWhen(System.currentTimeMillis())
                /**设置该通知优先级**/
                .setPriority(Notification.PRIORITY_DEFAULT)
                /**设置这个标志当用户单击面板就可以让通知将自动取消**/
                .setAutoCancel(true)
                /**设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)**/
                .setOngoing(false)
                /**向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：**/
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                // .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MQTTNotifierActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .build();
        /**发起通知**/
        nm.notify(MQTT_NOTIFICATION_UPDATE, notification);
        LOG.debug("notifyUser: alert=" + alert + ", title=" + title + ", body=" + body);


    }


    private String getConnectionChangeTimestamp() {
        return connectionStatusChangeTime.toString();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void messageArrived(IMqttTopic topic, IMqttMessage message) throws Exception {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        try {
            LOG.debug("messageArrived: topic=" + topic.getName() + ", message=" + new String(message.getPayload()));
            broadcastReceivedMessage(topic.getName(), message.getPayload());
        } catch (MqttException e) {
            e.printStackTrace();
        }

        scheduleNextPing();
        wl.release();
    }

    @Override
    public void connectionLost(Throwable throwable) {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        if (isOnline() == false) {
            changeStatus(ConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
            broadcastServiceStatus("Connection lost - no network connection");
            notifyUser("Connection lost - no network connection", "MQTT", "Connection lost - no network connection");
        } else {
            changeStatus(ConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
            broadcastServiceStatus("Connection lost - reconnecting...");
        }

        wl.release();
    }

    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        private final Logger LOG = Logger.getLogger(NetworkConnectionIntentReceiver.class);

        @Override
        public void onReceive(Context ctx, Intent intent) {

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            LOG.warn("onReceive: isOnline()=" + isOnline() + ", isConnected()=" + isConnected());
            if (isOnline() && !isConnected()) {
                doStart(null, -1);
            }
            wl.release();
        }
    }

    public class PingSender extends BroadcastReceiver {
        private final Logger LOG = Logger.getLogger(PingSender.class);

        @Override
        public void onReceive(Context context, Intent intent) {

            if (isOnline() && !isConnected()) {
                LOG.warn("onReceive: isOnline()=" + isOnline() + ", isConnected()=" + isConnected());
                doStart(null, -1);
            } else if (!isOnline()) {
                LOG.debug("Waiting for network to come online again");
            } else {
                try {
                    mqttClient.ping();
                } catch (MqttException e) {
                    LOG.error("ping failed - MQTT exception", e);

                    try {
                        mqttClient.disconnect();
                    } catch (MqttPersistenceException e1) {
                        LOG.error("disconnect failed - persistence exception", e1);
                    } catch (MqttException e2) {
                        LOG.error("disconnect failed - mqtt exception", e2);
                    }

                    LOG.warn("onReceive: MqttException=" + e);
                    doStart(null, -1);
                }
            }

            scheduleNextPing();
        }
    }

}
