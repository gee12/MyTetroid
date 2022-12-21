package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.Base64
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import java.lang.Exception

/**
 * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
 */
class SavePasswordCheckDataUseCase : UseCase<Boolean, SavePasswordCheckDataUseCase.Params>() {

    data class Params(
        val databaseConfig: DatabaseConfig,
        val password: String,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        return try {
            val salt = Utils.createRandomBytes(32)
            val passHash = Crypter.calculatePBKDF2Hash(params.password, salt)
            params.databaseConfig.savePass(
                Base64.encodeToString(passHash, false),
                Base64.encodeToString(salt, false), true
            ).toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PassCheckData(ex).toLeft()
        }
    }

}