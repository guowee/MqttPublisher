package com.uowee.mqtt.impl;

import com.uowee.mqtt.interfaces.IMqttConnectOptions;


public class MqttConnectOptions implements IMqttConnectOptions {

    private String username;
    private char[] password;
    private short keepAliveSeconds;
    private boolean isClean;

    @Override
    public void setUserName(String username) {
        this.username = username;
    }

    @Override
    public void setPassword(char[] password) {
        this.password = password;
    }

    @Override
    public void setCleanSession(boolean cleanStart) {
        this.isClean = cleanStart;
    }

    @Override
    public void setKeepAliveInterval(short keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public char[] getPassword() {
        return password;
    }

    @Override
    public int getKeepAliveInterval() {
        return keepAliveSeconds;
    }

    @Override
    public boolean getCleanSession() {
        return isClean;
    }
}
