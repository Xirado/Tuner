package at.xirado.tuner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

suspend fun PreparedStatement.executeAsync(): Boolean {
    return withContext(Dispatchers.IO) {
        execute()
    }
}

suspend fun PreparedStatement.executeQueryAsync(): ResultSet {
    return withContext(Dispatchers.IO) {
        executeQuery()
    }
}