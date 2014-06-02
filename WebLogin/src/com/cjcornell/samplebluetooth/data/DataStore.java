package com.cjcornell.samplebluetooth.data;

import com.cjcornell.samplebluetooth.Friend;

/**
 * Stores data used throughout the application. Use this to save data that
 * everyone needs, instead of passing it from intent to intent. It will stay
 * valid even if you rotate the screen.
 */
public class DataStore {
    private static DataStore instance = new DataStore();

    private String token = null;
    private Friend me = null;
    private String accessToken = null;
    private String macAddress = null;

    protected DataStore() {}

    public static DataStore getInstance() {
        return instance;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String newToken) {
        token = newToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getBaseParameterString() {
        return this.accessToken + "/" + this.me.getId();
    }

    public Friend getMe() {
        return me;
    }

    public void setMe(Friend me) {
        this.me = me;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
