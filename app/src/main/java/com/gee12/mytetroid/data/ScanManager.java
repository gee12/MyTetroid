package com.gee12.mytetroid.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Поиск объектов хранилища.
 * Название такое, а не SearchManager, из-за существования одноименного класса в пакете android.app.
 */
public class ScanManager implements Parcelable {

    public static final String QUERY_SEPAR = " ";
//    private DataManager dataManager;

    /**
     * Словарь найденных объектов
     */
    HashMap<ITetroidObject, FoundType> foundObjects = new HashMap<>();

    /**
     * Запрос
     */
    private String query;
    /**
     * Источники поиска
     */
    boolean inText;
    boolean inRecordsNames;
    boolean inAuthor;
    boolean inUrl;
    boolean inTags;
    boolean inNodes;
    boolean inFiles;
    /**
     * Разбивать ли запрос на слова
     */
    private boolean isSplitToWords;
    /**
     * Искать только целые слова
     */
    private boolean isOnlyWholeWords;
    /**
     * Искать только в текущей ветке
     */
    private boolean isSearchInNode;


    public ScanManager(String query) {
        this.query = query;
    }

//    public ScanManager(String query, boolean isSplitToWords, boolean isOnlyWholeWords, boolean isSearchInNode) {
//        this.query = query;
//        this.isSplitToWords = isSplitToWords;
//        this.isOnlyWholeWords = isOnlyWholeWords;
//        this.isSearchInNode = isSearchInNode;
//    }

    protected ScanManager(Parcel in) {
        this.query = in.readString();
        this.inText = in.readInt() == 1;
        this.inRecordsNames = in.readInt() == 1;
        this.inAuthor = in.readInt() == 1;
        this.inUrl = in.readInt() == 1;
        this.inTags = in.readInt() == 1;
        this.inNodes = in.readInt() == 1;
        this.inFiles = in.readInt() == 1;
        this.isSplitToWords = in.readInt() == 1;
        this.isOnlyWholeWords = in.readInt() == 1;
        this.isSearchInNode = in.readInt() == 1;
    }

    public HashMap<ITetroidObject,FoundType> globalSearch(/*DataManager data, */TetroidNode node) {
//        List<ITetroidObject> res = new ArrayList<>();

        if (isSplitToWords) {
            for (String word : query.split(QUERY_SEPAR)) {
                foundObjects.putAll(globalSearch(/*data, */node, word));
            }
        } else {
            foundObjects.putAll(globalSearch(/*data, */node, query));
        }
        return foundObjects;
    }

    public HashMap<ITetroidObject,FoundType> globalSearch(/*DataManager data, */TetroidNode node, String query) {
        List<TetroidNode> srcNodes;
        if (isSearchInNode) {
            if (node != null) {
                srcNodes = new ArrayList<>();
                srcNodes.add(node);
            } else {
                return null;
            }
        } else {
            srcNodes = DataManager.getRootNodes();
        }
        // поиск по веткам, записям, реквизитам записей, файлам
        boolean inRecords = inRecordsNames || inText || inAuthor || inUrl || inFiles;
        if (inNodes || inRecords) {
            searchInNodes(srcNodes, query, isOnlyWholeWords, inRecords);
        }
        // поиск по всем меткам в базе, если не указана ветка для поиска
        if (inTags && !isSearchInNode) {
//            res.addAll(globalSearchInTags(DataManager.getTagsHashMap(), query, isOnlyWholeWords));
            searchInTags(DataManager.getTags(), query, isOnlyWholeWords);
        }
        return foundObjects;
    }

    /**
     * Поиск по названиям веток.
     * Пропускает зашифрованные ветки.
     * @param nodes
     * @param query
     * @return
     */
    public void searchInNodesNames(List<TetroidNode> nodes, String query, boolean isOnlyWholeWords) {
        String regex = buildRegex(query);
        for (TetroidNode node : nodes) {
            if (!node.isNonCryptedOrDecrypted())
                continue;
            if (node.getName().matches(regex)) {
                addFoundObject(node, FoundType.TYPE_NODE);
            }
            if (node.getSubNodesCount() > 0) {
                searchInNodesNames(node.getSubNodes(), query, isOnlyWholeWords);
            }
        }
    }

    public static List<TetroidNode> searchInNodesNames(List<TetroidNode> nodes, String query) {
        List<TetroidNode> res = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidNode node : nodes) {
            if (!node.isNonCryptedOrDecrypted())
                continue;
            if (node.getName().matches(regex)) {
                res.add(node);
            }
            if (node.getSubNodesCount() > 0) {
                searchInNodesNames(node.getSubNodes(), query);
            }
        }
        return res;
    }


    public void searchInNodes(List<TetroidNode> nodes, String query,
                                              boolean isOnlyWholeWords, boolean inRecords) {
        String regex = buildRegex(query);
        for (TetroidNode node : nodes) {
            if (!node.isNonCryptedOrDecrypted())
                continue;
            if (inNodes && node.getName().matches(regex)) {
                addFoundObject(node, FoundType.TYPE_NODE);
            }
            if (inRecords && node.getRecordsCount() > 0) {
                searchInRecords(node.getRecords(), query, isOnlyWholeWords);
            }
            if (node.getSubNodesCount() > 0) {
                searchInNodes(node.getSubNodes(), query, isOnlyWholeWords, inRecords);
            }
        }
    }

    /**
     * Поиск по названиям записей.
     * @param srcRecords
     * @param query
     * @return
     */
    public void searchInRecords(
            List<TetroidRecord> srcRecords, String query, boolean isOnlyWholeWords) {
        String regex = buildRegex(query);
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
                searchInFiles(record.getAttachedFiles(), query, isOnlyWholeWords);
            }
            // поиск по тексту записи (читаем текст html файла)
            if (inText) {
                String text = DataManager.getRecordTextDecrypted(record);
                if (text != null && text.matches(regex)) {
                    addFoundObject(record, FoundType.TYPE_RECORD_TEXT);
                }
            }
            // поиск по меткам (только если указана ветка для поиска)
            if (inTags && isSearchInNode) {
//                found.addAll(searchInRecordTags(record.getTags(), query, isOnlyWholeWords));
                searchInTags(record.getTags(), query, isOnlyWholeWords);
            }
        }
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

    public static List<TetroidFile> searchInFiles(List<TetroidFile> srcFiles, String query) {
        List<TetroidFile> found = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidFile file : srcFiles) {
            if (file.getName().matches(regex)) {
                found.add(file);
            }
        }
        return found;
    }

    public <T extends ITetroidObject>  List<T> searchInFiles(
            List<TetroidFile> srcFiles, String query, boolean isOnlyWholeWords)
    {
        List<T> found = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidFile file : srcFiles) {
            if (file.getName().matches(regex)) {
                addFoundObject(file, FoundType.TYPE_FILE);
            }
        }
        return found;
    }

//    /**
//     * Поиск по именам меток.
//     * @param tagsMap
//     * @param query
//     * @param isOnlyWholeWords
//     * @return
//     */
//    public static TreeMap<String, TetroidTag> searchInTags(
//            Map<String, TetroidTag> tagsMap, String query, boolean isOnlyWholeWords) {
//        TreeMap<String, TetroidTag> found = new TreeMap<>();
//        String regex = buildRegex(query);
//        for (TetroidTag tagEntry : tagsMap.values()) {
//            if (tagEntry.getName().matches(regex)) {
//                found.put(tagEntry.getName(), tagEntry);
//            }
//        }
//        return found;
//    }

    public static List<TetroidTag> searchInTags(
            List<TetroidTag> tags, String query, boolean isOnlyWholeWords) {
        List<TetroidTag> found = new ArrayList<>();
        String regex = buildRegex(query);
        for (TetroidTag tag : tags) {
            if (tag.getName().matches(regex)) {
                found.add(tag);
            }
        }
        return found;
    }

//    public static List<TetroidTag> globalSearchInTags(
//            Map<String, TetroidTag> tagsMap, String query, boolean isOnlyWholeWords) {
//        List<TetroidTag> found = new ArrayList<>();
//        String regex = buildRegex(query);
//        for (TetroidTag tagEntry : tagsMap.values()) {
//            if (tagEntry.getName().matches(regex)) {
//                tagEntry.addFoundType(ITetroidObject.TYPE_TAG);
//                found.add(tagEntry);
//            }
//        }
//        return found;
//    }

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

    private static String buildRegex(String query) {
        return "(?is)" + ".*" + Pattern.quote(query) + ".*";
    }

    public void addFoundObject(ITetroidObject obj, int foundType) {
//        obj.foundType |= (1 << foundType);
        if (foundObjects.containsKey(obj)) {
            foundObjects.get(obj).addType(foundType);
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

    public boolean isSplitToWords() {
        return isSplitToWords;
    }

    public boolean isOnlyWholeWords() {
        return isOnlyWholeWords;
    }

    public boolean isSearchInNode() {
        return isSearchInNode;
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
