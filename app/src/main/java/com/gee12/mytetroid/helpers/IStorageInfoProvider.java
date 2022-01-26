package com.gee12.mytetroid.helpers;

import com.gee12.mytetroid.model.Version;

public interface IStorageInfoProvider {
    void calcCounters();

    Version getFormatVersion();

    int getNodesCount();

    int getCryptedNodesCount();

    int getRecordsCount();

    int getCryptedRecordsCount();

    int getFilesCount();

    int getTagsCount();

    int getUniqueTagsCount();

    int getAuthorsCount();

    int getMaxSubnodesCount();

    int getMaxDepthLevel();
}
