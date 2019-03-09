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

    /**
     * Загрузка параметров из файла
     * @param fileName
     * @return
     */
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

    /**
     * Сохранение параметров в файл
     * @param fileName
     * @return
     */
    public boolean save(String fileName) {
        boolean retval = false;

        try {
            configuration.store(new FileOutputStream(fileName), null);
            retval = true;
        } catch (IOException e) {
            System.out.println("Configuration error: " + e.getMessage());
        }

        return retval;
    }

    /**
     * Установка значения по ключу
     * @param key
     * @param value
     */
    public void set(String key, String value) {
        configuration.setProperty(key, value);
    }

    /**
     * Получение значения по ключу
     * @param key
     * @return
     */
    public String get(String key) {
        return configuration.getProperty(key);
    }

    /**
     * Получение значения по ключу без двойных кавычек вначале и вконце
     * @param key
     * @return
     */
    public String getWithoutQuotes(String key) {
        String res = configuration.getProperty(key);
        if (res != null && !res.isEmpty())
            res = res.replaceAll("^\"|\"$", "");
        return res;
    }
}
