package com.gee12.mytetroid.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
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
public class ScanManager implements Parcelable {

    public static final String QUERY_SEPAR = " ";

    /**
     * Словарь найденных объектов.
     */
    HashMap<ITetroidObject, FoundType> foundObjects;

    /**
     * Запрос.
     */
    private String query;
    /**
     * Источники поиска.
     */
    boolean inText;
    boolean inRecordsNames;
    boolean inAuthor;
    boolean inUrl;

    /**
     * Поиск по меткам.
     * Тип поиска 1 - добавление в результат самих меток.
     * Тип поиска 2 - добавление в результат записей меток.
     */
    boolean inTags;
    boolean inNodes;
    boolean inFiles;
    boolean inIds;
    /**
     * Разбивать ли запрос на слова.
     */
    private boolean isSplitToWords;
    /**
     * Искать только целые слова.
     */
    private boolean isOnlyWholeWords;
    /**
     * Искать только в текущей ветке.
     */
    private boolean isSearchInNode;

    /**
     * Целевая ветка для поиска.
     */
    private TetroidNode node;
    /**
     * Найдены зашифрованные ветки.
     */
    private boolean existCryptedNodes;

    /**
     *
     * @param query
     */
    public ScanManager(String query) {
        this.query = query;
    }

    /**
     *
     * @param in
     */
    protected ScanManager(Parcel in) {
        this.query = in.readString();
        this.inText = in.readInt() == 1;
        this.inRecordsNames = in.readInt() == 1;
        this.inAuthor = in.readInt() == 1;
        this.inUrl = in.readInt() == 1;
        this.inTags = in.readInt() == 1;
        this.inNodes = in.readInt() == 1;
        this.inFiles = in.readInt() == 1;
        this.inIds = in.readInt() == 1;
        this.isSplitToWords = in.readInt() == 1;
        this.isOnlyWholeWords = in.readInt() == 1;
        this.isSearchInNode = in.readInt() == 1;
    }

    /**
     * Глобальный поиск.
     * @param node
     * @return
     */
    public HashMap<ITetroidObject,FoundType> globalSearch(Context context, TetroidNode node) {
        this.foundObjects = new HashMap<>();
        this.node = node;

        if (isSplitToWords) {
            for (String word : query.split(QUERY_SEPAR)) {
                foundObjects.putAll(globalSearch(context, node, word));
            }
        } else {
            foundObjects.putAll(globalSearch(context, node, query));
        }
        return foundObjects;
    }

    /**
     * Глобальный поиск
     * @param node
     * @param query
     * @return
     */
    private HashMap<ITetroidObject,FoundType> globalSearch(Context context, TetroidNode node, String query) {
        List<TetroidNode> srcNodes;
        if (isSearchInNode) {
            if (node != null) {
                srcNodes = new ArrayList<>();
                srcNodes.add(node);
            } else {
                return foundObjects;
            }
        } else {
            srcNodes = DataManager.getInstance().getRootNodes();
        }
        String regex = buildRegex(query, isOnlyWholeWords);
        // поиск по веткам, записям, реквизитам записей, файлам
        boolean inRecords = inRecordsNames || inText || inAuthor || inUrl || inFiles || inIds
                // 2 - если при поиске по меткам добавляем в результат сами записи, а не метки
                || inTags;
        if (inNodes || inRecords) {
            globalSearchInNodes(context, srcNodes, regex, inRecords);
        }
        // поиск по всем меткам в базе, если не указана ветка для поиска
        // 2 - если используем тип (2), то комментируем
//        if (inTags && !isSearchInNode) {
//            globalSearchInTags(DataManager.getTags(), regex, isOnlyWholeWords);
//        }
        return foundObjects;
    }

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

    /**
     * Глобальный поиск по названиям веток.
     * Пропускает зашифрованные ветки.
     * @param nodes
     * @param regex
     * @param inRecords
     */
    private void globalSearchInNodes(Context context, List<TetroidNode> nodes, String regex, boolean inRecords) {
        for (TetroidNode node : nodes) {
            if (!node.isNonCryptedOrDecrypted()) {
                this.existCryptedNodes = true;
                continue;
            }
            // поиск по именам веток
            if (inNodes && node.getName().matches(regex)) {
                addFoundObject(node, FoundType.TYPE_NODE);
            }
            // поиск по id веток
            if (inIds && node.getId().matches(regex)) {
                addFoundObject(node, FoundType.TYPE_NODE_ID);
            }
            if (inRecords && node.getRecordsCount() > 0) {
                globalSearchInRecords(context, node.getRecords(), regex);
            }
            if (node.getSubNodesCount() > 0) {
                globalSearchInNodes(context, node.getSubNodes(), regex, inRecords);
            }
        }
    }

    /**
     * Поиск по названиям записей.
     * @param srcRecords
     * @param regex
     * @return
     */
    private void globalSearchInRecords(Context context, List<TetroidRecord> srcRecords, String regex) {
        for (TetroidRecord record : srcRecords) {
            // поиск по именам записей
            if (inRecordsNames && record.getName().matches(regex)) {
                addFoundObject(record, FoundType.TYPE_RECORD);
            }
            // поиск по авторам
            if (inAuthor && record.getAuthor().matches(regex)) {
                addFoundObject(record, FoundType.TYPE_AUTHOR);
            }
            // поиск по ссылкам
            if (inUrl && record.getAuthor().matches(regex)) {
                addFoundObject(record, FoundType.TYPE_URL);
            }
            // поиск по файлам записи
            if (inFiles && record.getAttachedFilesCount() > 0) {
                globalSearchInFiles(record.getAttachedFiles(), regex);
            }
            // поиск по id записей
            if (inIds && record.getId().matches(regex)) {
                addFoundObject(record, FoundType.TYPE_RECORD_ID);
            }
            // поиск по тексту записи (читаем текст html файла)
            if (inText) {
                String text = RecordsManager.getRecordTextDecrypted(context, record);
                if (text != null && text.matches(regex)) {
                    addFoundObject(record, FoundType.TYPE_RECORD_TEXT);
                }
            }
            // поиск по меткам (только если указана ветка для поиска)
            // 2 - комментируем isSearchInNode, если используем тип (2)
            if (inTags /*&& isSearchInNode*/) {
                // 1 - добавляем саму метку в результат
//                globalSearchInTags(record.getTags(), query, isOnlyWholeWords);
                // 2 - добавляем запись, содержащую метку
                if (record.getTagsString().matches(regex)) {
                    addFoundObject(record, FoundType.TYPE_TAG);
                }
            }
        }
    }

    /**
     * Поиск по названиям записей.
     * @param srcRecords
     * @param query
     * @return
     */
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

    /**
     * Поиск по файлам записи.
     * @param files
     * @param query
     * @return
     */
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

    /**
     * Глобальный поиск по файлам записи.
     * @param files
     * @param regex
     * @return
     */
    private void globalSearchInFiles(List<TetroidFile> files, String regex) {
        for (TetroidFile file : files) {
            // поиск по именам файлов
            if (file.getName().matches(regex)) {
                addFoundObject(file, FoundType.TYPE_FILE);
            }
            // поиск по id файлов
            if (file.getId().matches(regex)) {
                addFoundObject(file, FoundType.TYPE_FILE_ID);
            }
        }
    }

    /**
     * Поиск по меткам.
     * @param tags
     * @param query
     * @return
     */
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
     * Глобальный поиск по меткам.
     * Не используется в связи с переходом на 2й тип поиска по меткам.
     * @param tags
     * @param regex
     */
    public void globalSearchInTags(List<TetroidTag> tags, String regex) {
        for (TetroidTag tagEntry : tags) {
            if (tagEntry.getName().matches(regex)) {
                // 1 - добавляем саму метку в результат
//                addFoundObject(tagEntry, FoundType.TYPE_TAG);
                // 2 - добавляем записи метки в результат
                for (TetroidRecord record : tagEntry.getRecords()) {
                    addFoundObject(record, FoundType.TYPE_TAG);
                }
            }
        }
    }

//    /**
//     * Поиск по именам меток, указанные в записи.
//     * @param tags
//     * @param query
//     * @param isOnlyWholeWords
//     * @return
//     */
//    public static List<ITetroidObject> searchInRecordTags(
//            List<TetroidTag> tags, String query, boolean isOnlyWholeWords) {
//        List<ITetroidObject> found = new ArrayList<>();
//        String regex = buildRegex(query);
//        for (TetroidTag tagEntry : tags) {
//            if (tagEntry.getName().matches(regex)) {
//                tagEntry.addFoundType(ITetroidObject.TYPE_TAG);
//                found.add(tagEntry);
//            }
//        }
//        return found;
//    }

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

    /**
     * Добавление типа найденного объекта (в зависимости от того, где он найден).
     * @param obj
     * @param foundType
     */
    private void addFoundObject(ITetroidObject obj, int foundType) {
        if (foundObjects.containsKey(obj)) {
            foundObjects.get(obj).addValue(foundType);
        } else {
            foundObjects.put(obj, new FoundType(foundType));
        }
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setInText(boolean inText) {
        this.inText = inText;
    }

    public void setInRecordsNames(boolean inRecordsNames) {
        this.inRecordsNames = inRecordsNames;
    }

    public void setInAuthor(boolean inAuthor) {
        this.inAuthor = inAuthor;
    }

    public void setInUrl(boolean inUrl) {
        this.inUrl = inUrl;
    }

    public void setInTags(boolean inTags) {
        this.inTags = inTags;
    }

    public void setInNodes(boolean inNodes) {
        this.inNodes = inNodes;
    }

    public void setInFiles(boolean inFiles) {
        this.inFiles = inFiles;
    }

    public void setInIds(boolean inIds) {
        this.inIds = inIds;
    }

    public void setSplitToWords(boolean splitToWords) {
        isSplitToWords = splitToWords;
    }

    public void setOnlyWholeWords(boolean onlyWholeWords) {
        isOnlyWholeWords = onlyWholeWords;
    }

    public void setSearchInNode(boolean searchInNode) {
        isSearchInNode = searchInNode;
    }

    public String getQuery() {
        return query;
    }

    public boolean isInText() {
        return inText;
    }

    public boolean isInRecordsNames() {
        return inRecordsNames;
    }

    public boolean isInAuthor() {
        return inAuthor;
    }

    public boolean isInUrl() {
        return inUrl;
    }

    public boolean isInTags() {
        return inTags;
    }

    public boolean isInNodes() {
        return inNodes;
    }

    public boolean isInFiles() {
        return inFiles;
    }

    public boolean isInIds() {
        return inIds;
    }

    public boolean isSplitToWords() {
        return isSplitToWords;
    }

    public boolean isOnlyWholeWords() {
        return isOnlyWholeWords;
    }

    public boolean isSearchInNode() {
        return isSearchInNode;
    }

    public TetroidNode getNode() {
        return node;
    }

    public boolean isExistCryptedNodes() {
        return existCryptedNodes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(query);
        dest.writeInt((inText) ? 1 : 0);
        dest.writeInt((inRecordsNames) ? 1 : 0);
        dest.writeInt((inAuthor) ? 1 : 0);
        dest.writeInt((inUrl) ? 1 : 0);
        dest.writeInt((inTags) ? 1 : 0);
        dest.writeInt((inNodes) ? 1 : 0);
        dest.writeInt((inFiles) ? 1 : 0);
        dest.writeInt((inIds) ? 1 : 0);
        dest.writeInt((isSplitToWords) ? 1 : 0);
        dest.writeInt((isOnlyWholeWords) ? 1 : 0);
        dest.writeInt((isSearchInNode) ? 1 : 0);
    }

    public static final Creator<ScanManager> CREATOR = new Creator<ScanManager>() {
        @Override
        public ScanManager createFromParcel(Parcel in) {
            return new ScanManager(in);
        }

        @Override
        public ScanManager[] newArray(int size) {
            return new ScanManager[size];
        }
    };
}
