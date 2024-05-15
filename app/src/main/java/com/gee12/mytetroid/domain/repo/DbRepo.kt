package com.gee12.mytetroid.domain.repo

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDatatypeMismatchException
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.toLeft
import java.sql.SQLException

abstract class DbRepo {

    protected fun <T> handleRequest(request: () -> T): Either<Failure, T> {
        return try {
            request.invoke()?.let { Either.Right(it) }
                ?: Failure.Database.ItemNotFound.toLeft()
        } catch (ex: SQLException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteDatatypeMismatchException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteDatabaseLockedException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteDatabaseCorruptException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteConstraintException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Failure.Database.Unknown(ex).toLeft()
        }
    }

    protected fun <T> handleNullableRequest(request: () -> T?): Either<Failure, T?> {
        return try {
            Either.Right(request.invoke())
        } catch (ex: SQLException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteDatatypeMismatchException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteDatabaseLockedException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteDatabaseCorruptException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: SQLiteConstraintException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
            Failure.Database.SqlError(ex).toLeft()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Failure.Database.Unknown(ex).toLeft()
        }
    }

}