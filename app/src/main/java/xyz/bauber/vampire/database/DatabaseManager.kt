package xyz.bauber.vampire.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseManager(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "glucose.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "glucosa"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_TIMESTAMP_OFFSET = "timestamp_offset"
        private const val COLUMN_GLUCOSE_VALUE = "glucose_value"
        private const val COLUMN_GLUCOSE_UNITS = "glucose_units"
        private const val COLUMN_GLUCOSE_TYPE = "glucose_type"
        private const val COLUMN_TREND = "trend"
        private const val COLUMN_ORIGIN = "origin"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_TIMESTAMP INTEGER PRIMARY KEY," +
                "$COLUMN_TIMESTAMP_OFFSET INTEGER," +
                "$COLUMN_GLUCOSE_VALUE REAL," +
                "$COLUMN_GLUCOSE_UNITS TEXT," +
                "$COLUMN_GLUCOSE_TYPE TEXT," +
                "$COLUMN_TREND TEXT," +
                "$COLUMN_ORIGIN TEXT)"

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Aquí puedes realizar acciones si la versión de la base de datos cambia en futuras actualizaciones
    }

    fun insertGlucoseRecord(
        timestamp: Long,
        timestampOffset: Long,
        glucoseValue: Double,
        glucoseUnits: String,
        glucoseType: String,
        trend: String,
        origin: String
    ) {
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_TIMESTAMP_OFFSET, timestampOffset)
            put(COLUMN_GLUCOSE_VALUE, glucoseValue)
            put(COLUMN_GLUCOSE_UNITS, glucoseUnits)
            put(COLUMN_GLUCOSE_TYPE, glucoseType)
            put(COLUMN_TREND, trend)
            put(COLUMN_ORIGIN, origin)
        }

        val db = writableDatabase
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getGlucoseRecords(): List<GlucoseRecord> {
        val records = mutableListOf<GlucoseRecord>()

        val query = "SELECT * FROM $TABLE_NAME ORDER BY timestamp DESC LIMIT 480"
        val db = readableDatabase
        val cursor: Cursor? = db.rawQuery(query, null)

        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                    val timestampOffset =
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP_OFFSET))
                    val glucoseValue = cursor.getDouble(cursor.getColumnIndex(COLUMN_GLUCOSE_VALUE))
                    val glucoseUnits = cursor.getString(cursor.getColumnIndex(COLUMN_GLUCOSE_UNITS))
                    val glucoseType = cursor.getString(cursor.getColumnIndex(COLUMN_GLUCOSE_TYPE))
                    val trend = cursor.getString(cursor.getColumnIndex(COLUMN_TREND))
                    val origin = cursor.getString(cursor.getColumnIndex(COLUMN_ORIGIN))

                    val record = GlucoseRecord(
                        timestamp,
                        timestampOffset,
                        glucoseValue,
                        glucoseUnits,
                        glucoseType,
                        trend,
                        origin
                    )
                    records.add(record)
                } while (cursor.moveToNext())
            }
        }

        cursor?.close()
        return records
    }

    @SuppressLint("Range")
    fun getLastGlucose(): GlucoseRecord? {
        val query = "SELECT * FROM $TABLE_NAME ORDER BY timestamp DESC LIMIT 1"
        val db = readableDatabase
        val cursor: Cursor? = db.rawQuery(query, null)

        cursor?.use {
            if (cursor.moveToFirst()) {
                val timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val timestampOffset =
                    cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP_OFFSET))
                val glucoseValue = cursor.getDouble(cursor.getColumnIndex(COLUMN_GLUCOSE_VALUE))
                val glucoseUnits = cursor.getString(cursor.getColumnIndex(COLUMN_GLUCOSE_UNITS))
                val glucoseType = cursor.getString(cursor.getColumnIndex(COLUMN_GLUCOSE_TYPE))
                val trend = cursor.getString(cursor.getColumnIndex(COLUMN_TREND))
                val origin = cursor.getString(cursor.getColumnIndex(COLUMN_ORIGIN))

                val record = GlucoseRecord(
                    timestamp,
                    timestampOffset,
                    glucoseValue,
                    glucoseUnits,
                    glucoseType,
                    trend,
                    origin
                )
                cursor.close()
                return record
            }
        }

        cursor?.close()
        return null
    }

}

data class GlucoseRecord(
    val timestamp: Long,
    val timestampOffset: Long,
    val glucoseValue: Double,
    val glucoseUnits: String,
    val glucoseType: String,
    val trend: String,
    val origin: String
)