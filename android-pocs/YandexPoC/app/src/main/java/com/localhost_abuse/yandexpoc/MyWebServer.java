package com.localhost_abuse.yandexpoc;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MyWebServer extends NanoHTTPD {
    private static final String TAG = "MyWebServer";
    private final File logFile;
    private final Context context;

    public MyWebServer(Context context, int port) throws IOException {
        super("0.0.0.0", port);
        this.context = context;
        logFile = new File(context.getFilesDir(), "server_logs.txt");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String method = session.getMethod().name();
        String uri = session.getUri();
        Map<String, String> headers = session.getHeaders();
        Map<String, String> queryParams = session.getParms();
        String postBody = "";
        try {
            Map<String, String> bodyParams = session.getParms();
            session.parseBody(bodyParams);
            postBody = bodyParams.get("postData");
        } catch (Exception ignored) {
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String formattedHeaders = formatKeyValuePairs(headers);
        String formattedQueryParams = formatKeyValuePairs(queryParams);
        String formattedBody = formatJson(postBody);
        String logEntry = "---- New HTTP Request ----\n"
                + "Method: " + method + "\n"
                + "Timestamp: " + timestamp + "\n"
                + "Request URL: " + uri + "\n"
                + "Headers:\n" + formattedHeaders
                + "Query Params:\n" + formattedQueryParams
                + "Request Body:\n" + formattedBody + "\n";
        Log.d(TAG, logEntry);
        writeLogToFile(logEntry);
        if (!method.equals("OPTIONS")) {
            String origin = headers.get("origin");
            if (origin != null && !origin.equals("null")) {
                String browser = BrowserDetector.detectBrowser(headers);
                LogDatabaseHelper.getInstance(context).insertLog(timestamp, origin, browser);
            }
        }
        RequestLogger.getInstance().logRequest(logEntry);
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "");
    }

    private String formatKeyValuePairs(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "None\n";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String formatJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "No body\n";
        }
        try {
            if (json.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(json);
                return obj.toString(4);
            } else if (json.trim().startsWith("[")) {
                JSONArray arr = new JSONArray(json);
                return arr.toString(4);
            }
        } catch (JSONException e) {
            return "Invalid JSON Format\n" + json + "\n";
        }
        return json;
    }

    private void writeLogToFile(String logEntry) {
        try (FileWriter w = new FileWriter(logFile, true)) {
            w.append(logEntry);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }
}
