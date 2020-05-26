package com.gee12.mytetroid.data;

import com.gee12.mytetroid.LogManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class INIProperties {

    private Properties config;
    private String fileName;

    /**
     *
     * @param fileName
     */
    public INIProperties(String fileName) {
        this.config = new Properties();
        this.fileName = fileName;
    }

    /**
     * Загрузка параметров из файла.
     * @return
     */
    public boolean load() {
        try {
            config.load(new FileInputStream(fileName));
        } catch (IOException e) {
            LogManager.addLog("Configuration error: ", e);
            return false;
        }
        return true;
    }

    /**
     * Сохранение параметров в файл.
     * @return
     */
    public boolean save() {
        try {
            config.store(new FileOutputStream(fileName), null);
        } catch (IOException e) {
            LogManager.addLog("Configuration error: ", e);
            return false;
        }
        return true;
    }

    /**
     * Установка значения по ключу.
     * @param key
     * @param value
     */
    public void set(String key, String value) {
        config.setProperty(key, value);
    }

    /**
     * Получение значения по ключу
     * @param key
     * @return
     */
    public String get(String key) {
        return config.getProperty(key);
    }

}
