package com.gee12.mytetroid.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Поиск объектов хранилища.
 * Название такое, а не SearchManager, из-за существования одноименного класса в пакете android.app.
 */
public class ScanManager implements Parcelable {

    public static final String QUERY_SEPAR = " ";
//    private DataManager dataManager;
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

    public ScanManager(String query, boolean isSplitToWords, boolean isOnlyWholeWords, boolean isSearchInNode) {
        this.query = query;
        this.isSplitToWords = isSplitToWords;
        this.isOnlyWholeWords = isOnlyWholeWords;
        this.isSearchInNode = isSearchInNode;
    }

    protected ScanManager(Parcel in) {
        this.query = in.readString();
        this.isSplitToWords = in.readInt() == 1;
        this.isOnlyWholeWords = in.readInt() == 1;
        this.isSearchInNode = in.readInt() == 1;
    }

    public List<String> globalSearch(DataManager data, TetroidNode node) {
        List<String> res = new ArrayList<>();

        if (isSplitToWords) {
            for (String word : query.split(QUERY_SEPAR)) {
                res.addAll(globalSearch(data, node, word));
            }
        } else {
            res.addAll(globalSearch(data, node, query));
        }
        return res;
    }

    public List<String> globalSearch(DataManager data, TetroidNode node, String query) {
        List<String> res = new ArrayList<>();

            if (isSearchInNode && node != null) {

            }

            if (inNodes) {

            }
            if (inTags) {

            }
            if (inFiles) {

            }
            if (inRecordsNames) {

            }
            if (inText) {

            }
            if (inAuthor) {

            }
            if (inUrl) {

            }

            return res;
    }

    /**
     * Поиск по названиям веток.
     * Пропускает зашифрованные ветки.
     * @param nodes
     * @param query
     * @return
     */
    public static List<TetroidNode> searchInNodesNames(List<TetroidNode> nodes, String query) {
        List<TetroidNode> found = new ArrayList<>();
        String regex = "(?is)" + ".*" + Pattern.quote(query) + ".*";
        for (TetroidNode node : nodes) {
            if (!node.isNonCryptedOrDecrypted())
                continue;
            if (node.getName().matches(regex)) {
                found.add(node);
            }
            if (node.getSubNodesCount() > 0) {
                found.addAll(searchInNodesNames(node.getSubNodes(), query));
            }
        }
        return found;
    }

    /**
     * Поиск по названиям записей.
     * @param srcRecords
     * @param query
     * @return
     */
    public static List<TetroidRecord> searchInRecordsNames(List<TetroidRecord> srcRecords, String query) {
        List<TetroidRecord> found = new ArrayList<>();
        String regex = "(?is)" + ".*" + Pattern.quote(query) + ".*";
        for (TetroidRecord record : srcRecords) {
            if (record.getName().matches(regex)) {
                found.add(record);
            }
        }
        return found;
    }

    /**
     * Поиск по именам меток.
     * @param query
     * @return
     */
    public static TreeMap<String, List<TetroidRecord>> searchInTags(Map<String, List<TetroidRecord>> tagsMap, String query) {
        TreeMap<String, List<TetroidRecord>> found = new TreeMap<>();
        String regex = "(?is)" + ".*" + Pattern.quote(query) + ".*";
        for (Map.Entry<String, List<TetroidRecord>> tagEntry : tagsMap.entrySet()) {
            if (tagEntry.getKey().matches(regex)) {
                found.put(tagEntry.getKey(), tagEntry.getValue());
            }
        }
        return found;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(query);
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
