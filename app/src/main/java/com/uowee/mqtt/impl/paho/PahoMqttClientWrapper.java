package com.uowee.mqtt.impl.paho;

import com.uowee.mqtt.impl.MqttException;
import com.uowee.mqtt.impl.MqttPersistenceException;
import com.uowee.mqtt.interfaces.IMqttCallback;
import com.uowee.mqtt.interfaces.IMqttClient;
import com.uowee.mqtt.interfaces.IMqttConnectOptions;
import com.uowee.mqtt.interfaces.IMqttMessage;
import com.uowee.mqtt.interfaces.IMqttTopic;

import org.apache.log4j.Logger;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;


public class PahoMqttClientWrapper implements IMqttClient {

    private static final Logger LOG = Logger.getLogger(PahoMqttClientWrapper.class);

    private static final String TOPIC_PING = "PING";

    private MqttClient client;


    public PahoMqttClientWrapper(String serverURI, String clientId,
                                 MqttClientPersistence persistence) throws MqttException {
        LOG.debug("init(serverURI=" + serverURI + ", clientId=" + clientId + ", persistence=" + persistence + ")");

        try {
            this.client = new MqttClient(serverURI, clientId, persistence);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            throw new MqttException(e);
        }
    }

    @Override
    public void setCallback(final IMqttCallback callback) throws MqttException {
        this.client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                callback.connectionLost(cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                try {
                    callback.messageArrived(new PahoMqttTopicWrapper(topic), new PahoMqttMessageWrapper(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    @Override
    public void publish(IMqttTopic topic, IMqttMessage message) throws MqttException {
        MqttTopic t = this.client.getTopic(topic.getName());

        try {
            MqttMessage m = new MqttMessage();
            m.setRetained(message.isRetained());
            m.setQos(message.getQoS());
            m.setPayload(message.getPayload());

            t.publish(m);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            throw new MqttException(e);
        }

    }

    @Override
    public void subscribe(IMqttTopic topic) throws IllegalArgumentException, MqttException {
        subscribe(new IMqttTopic[]{topic});
    }

    @Override
    public void subscribe(IMqttTopic[] topics) throws IllegalArgumentException, MqttException {
        int amount = topics.length;

        String[] topicArray = new String[amount];
        int[] prioArray = new int[amount];

        for (int i = 0; i < amount; i++) {
            topicArray[i] = topics[i].getName();
            prioArray[i] = topics[i].getQoS();
        }

        try {
            this.client.subscribe(topicArray, prioArray);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            throw new MqttException(e);
        }
    }

    @Override
    public boolean isConnected() {
        return this.client.isConnected();
    }

    @Override
    public void connect(IMqttConnectOptions options) throws MqttException {

        if (this.client.isConnected()) {
            try {
                disconnect();
            } catch (MqttPersistenceException e) {
                e.printStackTrace();
            }
        }

        LOG.debug("connect(options=" + options.getUserName() + ",  " + options.getPassword() + ")");

        MqttConnectOptions o = new MqttConnectOptions();
        o.setCleanSession(options.getCleanSession());
        o.setKeepAliveInterval(options.getKeepAliveInterval());
        o.setUserName(options.getUserName());
        o.setPassword(options.getPassword());


        try {
            this.client.connect(o);
        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            throw new MqttException(e);
        }
    }

    @Override
    public void disconnect() throws MqttException, MqttPersistenceException {
        if (!this.client.isConnected()) {
            return;
        }

        try {
            this.client.disconnect();
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            throw new MqttException(e);
        }
    }

    @Override
    public void ping() throws MqttException {
        MqttTopic topic = this.client.getTopic(TOPIC_PING);

        MqttMessage message = new MqttMessage();
        message.setRetained(false);
        message.setQos(1);
        message.setPayload(new byte[]{0});

        try {
            topic.publish(message);
        } catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException e) {
            e.printStackTrace();
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            throw new MqttException(e);
        }
    }
}
