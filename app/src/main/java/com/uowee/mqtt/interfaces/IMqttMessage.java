package com.uowee.mqtt.interfaces;


import com.uowee.mqtt.impl.MqttException;

public interface IMqttMessage {

    int getQoS();

    byte[] getPayload() throws MqttException;

    boolean isRetained();

    boolean isDuplicate();
}
