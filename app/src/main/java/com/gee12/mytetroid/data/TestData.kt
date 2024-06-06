package com.gee12.mytetroid.data

import com.gee12.mytetroid.model.obj.TetroidNode
import java.util.Random

object TestData {

    fun addNodes(srcNodes: MutableList<TetroidNode?>?, count: Int, depthLevel: Int) {
        if (srcNodes == null) return
        val rand = Random()
        for (i in 0 until count) {
            val firstNode = createNodeRecursively(rand, i, 0, depthLevel)
            srcNodes.add(firstNode)
        }
    }

    fun createNodeRecursively(rand: Random, index: Int, curDepth: Int, maxDepth: Int): TetroidNode {
        val id = rand.nextLong().toString()
        val name = String.format("testNode %d", index + 1)
        val node = TetroidNode(
            id = id,
            sourceName = name,
            level = curDepth,
        )
        if (index < maxDepth) {
            val subNode = createNodeRecursively(rand, curDepth + 1, curDepth + 1, maxDepth)
            node.addSubNode(subNode)
        }
        return node
    }
}
