package com.uowee.mqtt.interfaces;

public interface IMqttConnectOptions {

    void setUserName(String username);

    void setPassword(char[] password);

    void setCleanSession(boolean cleanStart);

    void setKeepAliveInterval(short keepAliveSeconds);

    String getUserName();

    char[] getPassword();

    int getKeepAliveInterval();

    boolean getCleanSession();


}
