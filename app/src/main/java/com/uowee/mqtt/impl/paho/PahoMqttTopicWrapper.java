package com.uowee.mqtt.impl.paho;

import com.uowee.mqtt.interfaces.IMqttTopic;


public class PahoMqttTopicWrapper implements IMqttTopic {


    private String topic;

    public PahoMqttTopicWrapper(String topic) {
        this.topic = topic;
    }


    @Override
    public String getName() {
        return topic;
    }

    @Override
    public int getQoS() {
        return 0;
    }
}
