package com.gee12.mytetroid.data;

import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static List<TetroidNode> rootNodesCollection;
    private static List<TetroidRecord> recordsCollection;

    public static void init() {
        TetroidRecord firstRec = new TetroidRecord(1, "First record");
        TetroidRecord secondRec = new TetroidRecord(2, "Second record");
        TetroidRecord thirdRec = new TetroidRecord(3, "Third record");
        TetroidRecord fourthRec = new TetroidRecord(4, "Fourth record");
        TetroidRecord fifthRec = new TetroidRecord(5, "Sixth record");
        TetroidRecord sixthRec = new TetroidRecord(6, "Sixth record");
        TetroidRecord seventhRec = new TetroidRecord(7, "Seventh record");
        TetroidRecord eighthRec = new TetroidRecord(8, "Eighth record");
        TetroidRecord ninethRec = new TetroidRecord(9, "Ninth record");

        TetroidNode first = new TetroidNode(1,"First",0);
        first.addRecord(firstRec);
        first.addRecord(secondRec);
        first.addRecord(thirdRec);
        first.addRecord(fourthRec);
        first.addRecord(fifthRec);
        TetroidNode second = new TetroidNode(2,"Second",1);
        second.addRecord(sixthRec);
        second.addRecord(seventhRec);
        TetroidNode third = new TetroidNode(3,"Third",1);
        TetroidNode fourth = new TetroidNode(4,"Fourth",1);
        TetroidNode fifth  = new TetroidNode(5,"Fifth",2);
        TetroidNode sixth = new TetroidNode(6,"Sixth",2);
        sixth.addRecord(eighthRec);
        TetroidNode seventh = new TetroidNode(7,"Seventh",3);
        TetroidNode eighth = new TetroidNode(8,"Eighth",0);
        TetroidNode nineth = new TetroidNode(9,"Ninth",0);
        nineth.addRecord(ninethRec);

        rootNodesCollection = new ArrayList<>();
        first.addSubNode(second);
        first.addSubNode(third);
            third.addSubNode(fifth);
            third.addSubNode(sixth);
                sixth.addSubNode(seventh);
        rootNodesCollection.add(first);
        eighth.addSubNode(fourth);
        rootNodesCollection.add(eighth);
        rootNodesCollection.add(nineth);

    }

    public static List<TetroidNode> getRootNodes() {
        return rootNodesCollection;
    }
}
