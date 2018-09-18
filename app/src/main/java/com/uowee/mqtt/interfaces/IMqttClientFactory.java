package com.uowee.mqtt.interfaces;


import com.uowee.mqtt.impl.MqttException;

public interface IMqttClientFactory {

    IMqttClient create(String host, int port, String clientId, IMqttPersistence persistence) throws MqttException;
}
