package com.gee12.mytetroid.helpers

interface ISensitiveDataProvider {
    fun getMiddlePassHashOrNull(): String?
    fun saveMiddlePassHash(value: String)
    fun resetMiddlePassHash()
}

class SensitiveDataProvider : ISensitiveDataProvider {

    var middlePassHash: String? = null

    override fun getMiddlePassHashOrNull(): String? {
        return middlePassHash
    }

    override fun saveMiddlePassHash(value: String) {
        middlePassHash = value
    }

    override fun resetMiddlePassHash() {
        middlePassHash = null
    }

}