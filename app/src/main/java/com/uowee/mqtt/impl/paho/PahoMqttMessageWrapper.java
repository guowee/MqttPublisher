package com.uowee.mqtt.impl.paho;

import com.uowee.mqtt.impl.MqttException;
import com.uowee.mqtt.interfaces.IMqttMessage;

import org.eclipse.paho.client.mqttv3.MqttMessage;


public class PahoMqttMessageWrapper implements IMqttMessage {


    private MqttMessage message;

    public PahoMqttMessageWrapper(MqttMessage message) {
        this.message = message;
    }

    @Override
    public int getQoS() {
        return message.getQos();
    }

    @Override
    public byte[] getPayload() throws MqttException {
        return message.getPayload();
    }

    @Override
    public boolean isRetained() {
        return message.isRetained();
    }

    @Override
    public boolean isDuplicate() {
        return message.isDuplicate();
    }
}
