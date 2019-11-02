package com.gee12.mytetroid.data;

import java.util.List;
import java.util.Random;

public class TestData {


    public static void addNodes(List<TetroidNode> srcNodes, int count, int depthLevel) {
        if (srcNodes == null)
            return;
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            TetroidNode firstNode = createNodeRecursively(rand, i, 0, depthLevel);
            srcNodes.add(firstNode);
        }
    }

    public static TetroidNode createNodeRecursively(Random rand, int index, int curDepth, int maxDepth) {
        String id = String.valueOf(rand.nextLong());
        String name = String.format("testNode %d", index+1);
        TetroidNode node = new TetroidNode(id, name, curDepth);
        if (index < maxDepth) {
            TetroidNode subNode = createNodeRecursively(rand, curDepth+1, curDepth+1, maxDepth);
            node.addSubNode(subNode);
        }
        return node;
    }
}
