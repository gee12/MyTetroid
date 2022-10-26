package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.model.Version

interface IStorageInfoProvider {

    var formatVersion: Version?
    var nodesCount: Int
    var cryptedNodesCount: Int
    var recordsCount: Int
    var cryptedRecordsCount: Int
    var filesCount: Int
    var tagsCount: Int
    var uniqueTagsCount: Int
    var authorsCount: Int
    var maxSubnodesCount: Int
    var maxDepthLevel: Int

    fun calcCounters()

}