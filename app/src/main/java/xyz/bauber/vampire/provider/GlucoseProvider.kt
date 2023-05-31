package xyz.bauber.vampire.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import xyz.bauber.vampire.BaseApplication
import xyz.bauber.vampire.database.DatabaseManager


class GlucoseProvider : ContentProvider() {
    // Aquí puedes usar SQLiteOpenHelper y SQLiteDatabase para interactuar con tu base de datos.
    override fun onCreate(): Boolean {

        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        // Implementa este método para manejar las solicitudes de consulta de tus datos.

        var last_record = sortOrder
        val databaseManager = DatabaseManager(BaseApplication.instance)

        val queryBuilder = SQLiteQueryBuilder()
        queryBuilder.setTables("glucosa");

        if (uri.getLastPathSegment().equals("last")) {
            last_record = "time DESC LIMIT 1";
        }

        val cursor: Cursor =
            queryBuilder.query(databaseManager.readableDatabase, projection, selection, selectionArgs, null, null, last_record)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }


    override fun getType(uri: Uri): String? {
        TODO("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        TODO("Not yet implemented")
    }

    companion object {
        private const val AUTHORITY = "xyz.bauber.vampire.provider"
        private const val BASE_PATH = "glucose"
        val CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH)
    }
}