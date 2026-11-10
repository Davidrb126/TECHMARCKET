package com.techmarket.app.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.techmarket.app.DatabaseHelper

object DatabaseProvider {

    @Volatile
    private var helper: DatabaseHelper? = null

    private fun obtenerHelper(context: Context): DatabaseHelper {
        return helper ?: synchronized(this) {
            helper ?: DatabaseHelper(
                context.applicationContext
            ).also {
                helper = it
            }
        }
    }

    fun obtenerLectura(context: Context): SQLiteDatabase {
        return obtenerHelper(context).readableDatabase
    }

    fun obtenerEscritura(context: Context): SQLiteDatabase {
        return obtenerHelper(context).writableDatabase
    }
}