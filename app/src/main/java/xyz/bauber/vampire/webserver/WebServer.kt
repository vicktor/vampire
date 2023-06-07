package xyz.bauber.vampire.webserver

import com.google.gson.Gson
import xyz.bauber.vampire.BaseApplication
import xyz.bauber.vampire.database.DatabaseManager
import fi.iki.elonen.NanoHTTPD
import xyz.bauber.vampire.services.WebServerService.Companion.PORT


class WebServer : NanoHTTPD(PORT) {
    override fun serve(session: IHTTPSession): Response {
        val mimeType = "text/json"

        val uri = session.uri
        val responseText = when (uri) {
            "/" -> "{\"about\": \"Vampire!\", \"version\":\"1.0.0\"}"
            "/glucose" -> getLastGlucose()
            "/glucoses" -> getGlucoses()
            else -> "Page not found"
        }

        return newFixedLengthResponse(Response.Status.OK, mimeType, responseText)
    }

    private fun getLastGlucose() : String {
        val databaseManager = DatabaseManager(BaseApplication.instance)
        val record = databaseManager.getLastGlucose()

        val gson = Gson()
        return gson.toJson(record)
    }

    fun getGlucoses() : String {
        val databaseManager = DatabaseManager(BaseApplication.instance)
        val records = databaseManager.getGlucoseRecords()

        val gson = Gson()
        return gson.toJson(records)

    }

}