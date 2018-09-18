package com.uowee.mqtt.impl;

public class MqttPersistenceException extends Throwable {
    private static final long serialVersionUID = -1575347886261275716L;

    public MqttPersistenceException(String detailMessage) {
        super(detailMessage);
    }

    public MqttPersistenceException(Throwable throwable) {
        super(throwable);
    }

    public MqttPersistenceException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}

