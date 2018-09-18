package com.uowee.mqtt.impl.paho;

import com.uowee.mqtt.impl.MqttException;
import com.uowee.mqtt.interfaces.IMqttClient;
import com.uowee.mqtt.interfaces.IMqttClientFactory;
import com.uowee.mqtt.interfaces.IMqttPersistence;


public class PahoMqttClientFactory implements IMqttClientFactory {
    @Override
    public IMqttClient create(String host, int port, String clientId, IMqttPersistence persistence) throws MqttException {
        PahoMqttClientPersistence persistenceImpl = null;
        if (persistence != null) {
            persistenceImpl = new PahoMqttClientPersistence(persistence);
        }
        return new PahoMqttClientWrapper("tcp://" + host + ":" + port, clientId, persistenceImpl);
    }
}
