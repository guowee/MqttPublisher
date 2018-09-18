package com.uowee.mqtt.interfaces;


import com.uowee.mqtt.impl.MqttException;
import com.uowee.mqtt.impl.MqttPersistenceException;

public interface IMqttClient {

    void setCallback(IMqttCallback callback) throws MqttException;

    void publish(IMqttTopic topic, IMqttMessage message) throws MqttException;

    void subscribe(IMqttTopic topic) throws IllegalArgumentException, MqttException;

    void subscribe(IMqttTopic[] topics) throws IllegalArgumentException, MqttException;

    boolean isConnected();

    void connect(IMqttConnectOptions options) throws MqttException;

    void disconnect() throws MqttException, MqttPersistenceException;

    void ping() throws MqttException;

}