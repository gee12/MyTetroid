package com.gee12.mytetroid.data;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class TetroidNode implements ITetroidObject {
    private String id;
    private String name;
    private int level;
    private List<TetroidNode> subNodes;
    private List<TetroidRecord> records;
    private Drawable icon;
    private String iconName;
    private boolean isCrypted;

    private boolean isDecrypted;

    public TetroidNode(boolean crypt, String id, String name, String iconName, int level) {
        this.id = id;
        this.name = name;
//        setIcon(iconFullName);
        this.iconName = iconName;
        this.isCrypted = crypt;
//        this.subNodes = subNodes;
//        this.records = records;
        this.level = level;
    }

    public TetroidNode(String id, String name, int level) {
        this.id = id;
        this.name = name;
        this.iconName = null;
        this.level = level;
        this.subNodes = new ArrayList<>();
        this.records = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getType() {
        return FoundType.TYPE_NODE;
    }

    @Override
    public String getName() {
        return name;
    }

//    public Uri getIconUri() {
//        return iconUri;
//    }

    public Drawable getIcon() {
        return icon;
    }

//    public void setIconUri(String fullFileName) {
//        File file = new File(fullFileName);
//        if (file.exists()) {
//            this.iconUri = Uri.fromFile(file);
//        }
//    }

    public void loadIcon(String fullFileName) {
        if (fullFileName == null)
            return;
        try {
//            this.icon = Utils.loadSVGFromFile(Environment.getExternalStorageDirectory() + "/KateDownloads/test.svg");
            this.icon = FileUtils.loadSVGFromFile(fullFileName);
        } catch (Exception e) {
            LogManager.addLog(e);
        }
    }

    public void loadIconFromStorage(String iconsStoragePath) {
        if (TextUtils.isEmpty(iconName))
            return;
        loadIcon(iconsStoragePath + iconName);
    }

    public String getIconName() {
        return iconName;
    }

    public String getCryptedName() {
        return (!isCrypted || isDecrypted) ? name : "Закрыто";
    }

    public int getLevel() {
        return level;
    }

    public List<TetroidNode> getSubNodes() {
        return subNodes;
    }

    public int getSubNodesCount() {
        return subNodes.size();
    }

    public List<TetroidRecord> getRecords() {
        return records;
    }

    public int getRecordsCount() {
        return records.size();
    }

    public boolean isCrypted() {
        return isCrypted;
    }

    /**
     * Получение признака, что запись не зашифрована.
     * @return True, если не зашифровано (crypt=0), или уже расшифровано.
     */
    public boolean isNonCryptedOrDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public void setDecrypted(boolean decrypted) {
        isDecrypted = decrypted;
    }

    public void setSubNodes(List<TetroidNode> subNodes) {
        this.subNodes = subNodes;
    }

    public void setRecords(List<TetroidRecord> records) {
        this.records = records;
    }

    public void addSubNode(TetroidNode subNode) {
        if (subNodes != null)
            subNodes.add(subNode);
    }

    public void addRecord(TetroidRecord record) {
        record.setNode(this);
        records.add(record);
    }

    public boolean isExpandable() {
        return !subNodes.isEmpty() /*&& isNonCryptedOrDecrypted()*/;
    }

}
