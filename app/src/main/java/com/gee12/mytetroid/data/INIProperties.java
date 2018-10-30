package com.gee12.mytetroid.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class INIProperties {
    private Properties configuration;
//    private String configurationFile = "config.ini";

    public INIProperties() {
        configuration = new Properties();
    }

    public boolean load(String fileName) {
        boolean retval = false;

        try {
            configuration.load(new FileInputStream(fileName));
            retval = true;
        } catch (IOException e) {
            System.out.println("Configuration error: " + e.getMessage());
        }

        return retval;
    }

    public boolean store(String fileName) {
        boolean retval = false;

        try {
            configuration.store(new FileOutputStream(fileName), null);
            retval = true;
        } catch (IOException e) {
            System.out.println("Configuration error: " + e.getMessage());
        }

        return retval;
    }

    public void set(String key, String value) {
        configuration.setProperty(key, value);
    }

    public String get(String key) {
        return configuration.getProperty(key);
    }
}
