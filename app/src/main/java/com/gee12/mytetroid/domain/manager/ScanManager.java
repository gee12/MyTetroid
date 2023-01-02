package com.gee12.mytetroid.domain.manager;

import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Поиск объектов хранилища.
 * Название такое, а не SearchManager, из-за существования одноименного класса в пакете android.app.
 */
@Deprecated
public class ScanManager {

    /**
     * Поиск по названиям веток (рекурсивно с подветками).
     * Пропускает зашифрованные ветки.
     * @param nodes
     * @param query
     * @return
     */
    public static List<TetroidNode> searchInNodesNames(List<TetroidNode> nodes, String query) {
        String regex = buildRegex(query);
        return searchInNodesNamesRecursively(nodes, regex);
    }

    private static List<TetroidNode> searchInNodesNamesRecursively(List<TetroidNode> nodes, String regex) {
        List<TetroidNode> res = new ArrayList<>();
        for (TetroidNode node : nodes) {
            if (!node.isNonCryptedOrDecrypted())
                continue;
            if (node.getName().matches(regex)) {
                res.add(node);
                continue;
            }
            if (node.getSubNodesCount() > 0) {
                res.addAll(searchInNodesNamesRecursively(node.getSubNodes(), regex));
            }
        }
        return res;
    }

    public static List<TetroidRecord> searchInRecordsNames(List<TetroidRecord> srcRecords, String query) {
        List<TetroidRecord> found = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidRecord record : srcRecords) {
            if (record.getName().matches(regex)) {
                found.add(record);
            }
        }
        return found;
    }

    public static List<TetroidFile> searchInFiles(List<TetroidFile> files, String query) {
        List<TetroidFile> found = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidFile file : files) {
            if (file.getName().matches(regex)) {
                found.add(file);
            }
        }
        return found;
    }

    public static List<TetroidTag> searchInTags(List<TetroidTag> tags, String query) {
        List<TetroidTag> found = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidTag tag : tags) {
            if (tag.getName().matches(regex)) {
                found.add(tag);
            }
        }
        return found;
    }

    public static Map<String,TetroidTag> searchInTags(Map<String,TetroidTag> tags, String query) {
        Map<String,TetroidTag> found = new HashMap<>();
        String regex = buildRegex(query);
        for (Map.Entry<String,TetroidTag> tag : tags.entrySet()) {
//            if (tag.getValue().getName().matches(regex)) {
            if (tag.getKey().matches(regex)) {
                found.put(tag.getKey(), tag.getValue());
            }
        }
        return found;
    }

    /**
     * Формирование регулярного выражения для поиска.
     * i - CASE_INSENSITIVE - регистронезависимый режим
     * s - DOTALL - режим одной строки (\n и \r не играют роли)
     * @param query
     * @return
     */
    private static String buildRegex(String query) {
        return "(?is)" + ".*" + Pattern.quote(query) + ".*";
    }

    private static String buildRegex(String query, boolean isOnlyWholeWords) {
        String boundary = (isOnlyWholeWords) ? "\\b" : "";
        // Pattern.quote - помещаем запрос между \Q и \E, чтобы он интерпритировался "как есть"
        return String.format("(?is).*%s%s%s.*", boundary, Pattern.quote(query), boundary);
    }

}
