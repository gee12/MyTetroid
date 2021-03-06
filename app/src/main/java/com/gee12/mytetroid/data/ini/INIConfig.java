package com.gee12.mytetroid.data.ini;

import com.gee12.mytetroid.logs.ILogger;

import org.ini4j.Ini;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class INIConfig {

    protected ILogger mLogger;
    protected Ini config;
    private String fileName;

    /**
     *
     * @param fileName
     */
    public INIConfig(ILogger logger, String fileName) {
        this.mLogger = logger;
        config = new Ini();
        this.fileName = fileName;
    }

    /**
     * Загрузка параметров из файла.
     * @return
     */
    public boolean load() {
        try {
            config.load(new FileReader(fileName));
        } catch (IOException e) {
            if (mLogger != null) {
                mLogger.log("Configuration error: ", e);
            }
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
            config.store(new FileWriter(fileName));
        } catch (IOException e) {
            if (mLogger != null) {
                mLogger.log("Configuration error: ", e);
            }
            return false;
        }
        return true;
    }

    public Map<String, String> getSection(String key) {
        return config.get(key);
    }

    public String get(String sectionKey, String key) {
        Map<String, String> section = getSection(sectionKey);
        return (section != null) ? section.get(key) : null;
    }

    public void set(String sectionKey, String key, String value) {
        Map<String, String> section = getSection(sectionKey);
        if (section == null) {
            section = config.add(sectionKey);
        }
        if (section != null) {
            section.put(key, value);
        }
    }
}
