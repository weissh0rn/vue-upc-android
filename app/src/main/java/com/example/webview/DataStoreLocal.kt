package com.example.webview

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "UserInfo.db"
        const val TABLE_NAME = "UserInfo"
        const val COLUMN_NAME = "name"
        const val COLUMN_PASSWORD = "password"

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE $TABLE_NAME (" +
                    "_id INTEGER PRIMARY KEY," +
                    "$COLUMN_NAME TEXT," +
                    "$COLUMN_PASSWORD TEXT)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}

class DataStoreLocal private constructor(private val context: Context) {

    private val dbHelper = SQLiteHelper(context)

    suspend fun saveUserInfo(name: String, password: String) {
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(SQLiteHelper.COLUMN_NAME, name)
                put(SQLiteHelper.COLUMN_PASSWORD, password)
            }
            db.insert(SQLiteHelper.TABLE_NAME, null, values)
        }
    }

    suspend fun getUserInfo(): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val projection = arrayOf(SQLiteHelper.COLUMN_NAME, SQLiteHelper.COLUMN_PASSWORD)
            val cursor: Cursor = db.query(
                SQLiteHelper.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
            )

            var name: String? = null
            var password: String? = null

            with(cursor) {
                while (moveToNext()) {
                    name = getString(getColumnIndexOrThrow(SQLiteHelper.COLUMN_NAME))
                    password = getString(getColumnIndexOrThrow(SQLiteHelper.COLUMN_PASSWORD))
                }
                close()
            }

            if (name != null && password != null) Pair(name!!, password!!)
            else null
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: DataStoreLocal? = null

        fun getInstance(context: Context): DataStoreLocal {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataStoreLocal(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
