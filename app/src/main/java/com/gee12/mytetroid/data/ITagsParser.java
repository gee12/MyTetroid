package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidRecord;

public interface ITagsParser {
    void parseRecordTags(TetroidRecord record, String tagsString);
}
