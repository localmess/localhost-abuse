package com.localhost_abuse.stealthapp;

import android.content.Context;
import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MyWebServer extends NanoHTTPD {

    private static final String TAG = "MyWebServer";
    private final File logFile;

    public MyWebServer(Context context, int port) throws IOException {
        super("0.0.0.0", port);
        logFile = new File(context.getFilesDir(), "server_logs.txt");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> headers = session.getHeaders();
        Map<String, String> queryParams = session.getParms();

        String postBody = "";
        try {
            Map<String, String> bodyParams = session.getParms();
            session.parseBody(bodyParams);
            postBody = bodyParams.get("postData");
        } catch (IOException | ResponseException e) {
            Log.e(TAG, "Error parsing request body", e);
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        String formattedHeaders = formatKeyValuePairs(headers);
        String formattedQueryParams = formatKeyValuePairs(queryParams);
        String formattedBody = formatJson(postBody);

        String logEntry =
                "---- New HTTP Request ----\n" +
                        "Timestamp: " + timestamp + "\n" +
                        "Request URL: " + uri + "\n" +
                        "Headers:\n" + formattedHeaders +
                        "Query Params:\n" + formattedQueryParams +
                        "Request Body:\n" + formattedBody + "\n";

        Log.d(TAG, logEntry);
        writeLogToFile(logEntry);

        // Notify UI
        RequestLogger.getInstance().logRequest(logEntry);

        return newFixedLengthResponse(Response.Status.OK, "text/plain", "");
    }

    private String formatKeyValuePairs(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "None\n";
        }
        StringBuilder formatted = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            formatted.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return formatted.toString();
    }

    private String formatJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "No body\n";
        }
        try {
            if (json.trim().startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                return jsonObject.toString(4);
            } else if (json.trim().startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                return jsonArray.toString(4);
            }
        } catch (JSONException e) {
            return "Invalid JSON Format\n" + json + "\n";
        }
        return json;
    }

    private void writeLogToFile(String logEntry) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(logEntry);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }
}
