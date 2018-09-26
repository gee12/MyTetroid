package com.gee12.mytetroid.data;

import com.gee12.mytetroid.data.TetroidNode;

import java.util.ArrayList;
import java.util.List;

public class NodesManager {
    private static List<TetroidNode> nodesCollection;

    public static void init() {
        TetroidNode first = new TetroidNode(1,"First",0);
        TetroidNode second = new TetroidNode(2,"Second",1);
        TetroidNode third = new TetroidNode(3,"Third",1);
        TetroidNode fourth = new TetroidNode(4,"Fourth",1);
        TetroidNode fifth  = new TetroidNode(5,"Fifth",2);
        TetroidNode sixth = new TetroidNode(6,"Sixth",2);
        TetroidNode seventh = new TetroidNode(7,"Seventh",3);
        TetroidNode eighth = new TetroidNode(8,"Eighth",0);
        TetroidNode nineth = new TetroidNode(9,"Ninth",0);

        first.addSubNode(second);
        first.addSubNode(third);
            third.addSubNode(fifth);
            third.addSubNode(sixth);
                sixth.addSubNode(seventh);
        eighth.addSubNode(fourth);
        //nineth;

        nodesCollection = new ArrayList<>();
        nodesCollection.add(first);
//        nodesCollection.add(second);
//        nodesCollection.add(third);
//        nodesCollection.add(fourth);
//        nodesCollection.add(fifth);
//        nodesCollection.add(sixth);
//        nodesCollection.add(seventh);
        nodesCollection.add(eighth);
        nodesCollection.add(nineth);
    }

    public static List<TetroidNode> getNodes() {
        return nodesCollection;
    }
}
