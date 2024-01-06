package com.gee12.mytetroid.domain.provider

interface ISensitiveDataProvider {
    fun getMiddlePasswordHashOrNull(): String?
    fun saveMiddlePasswordHash(value: String)
    fun resetMiddlePasswordHash()
}

class SensitiveDataProvider : ISensitiveDataProvider {

    var middlePassHash: String? = null

    override fun getMiddlePasswordHashOrNull(): String? {
        return middlePassHash
    }

    override fun saveMiddlePasswordHash(value: String) {
        middlePassHash = value
    }

    override fun resetMiddlePasswordHash() {
        middlePassHash = null
    }

}