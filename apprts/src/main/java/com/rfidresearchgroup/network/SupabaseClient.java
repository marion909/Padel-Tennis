package com.rfidresearchgroup.network;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Supabase REST API client for syncing match data to cloud database
 */
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    
    private static final String SUPABASE_URL = "https://cbzvnlajcsminrmcqytf.supabase.co";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNienZubGFqY3NtaW5ybWNxeXRmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAyNzM4NTIsImV4cCI6MjA4NTg0OTg1Mn0.gWUvw_iPaG0Ok6der2_7dblEem8hoRfdCWYyf9SwOhA";
    
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    
    private static SupabaseClient instance;
    
    private SupabaseClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }
    
    /**
     * Insert a match into Supabase
     * @param matchData Match object (will be serialized to JSON)
     * @return true if successful, false otherwise
     */
    public boolean insertMatch(Object matchData) {
        return insert("matches", matchData);
    }
    
    /**
     * Insert or upsert a player into Supabase
     * @param playerData Player object (will be serialized to JSON)
     * @return true if successful, false otherwise
     */
    public boolean upsertPlayer(Object playerData) {
        return upsert("players", playerData, "uuid");
    }
    
    /**
     * Insert a match-player relationship into Supabase
     * @param matchPlayerData MatchPlayer object (will be serialized to JSON)
     * @return true if successful, false otherwise
     */
    public boolean insertMatchPlayer(Object matchPlayerData) {
        return insert("match_players", matchPlayerData);
    }
    
    /**
     * Generic insert method for any table
     */
    private boolean insert(String table, Object data) {
        try {
            String jsonBody = JSON.toJSONString(data);
            
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/" + table)
                    .header("apikey", ANON_KEY)
                    .header("Authorization", "Bearer " + ANON_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully inserted into " + table);
                    return true;
                } else {
                    Log.e(TAG, "Failed to insert into " + table + ": " + 
                          response.code() + " - " + response.message());
                    if (response.body() != null) {
                        Log.e(TAG, "Response body: " + response.body().string());
                    }
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error inserting into " + table, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting into " + table, e);
            return false;
        }
    }
    
    /**
     * Upsert (insert or update) method for tables with unique constraints
     */
    private boolean upsert(String table, Object data, String onConflict) {
        try {
            String jsonBody = JSON.toJSONString(data);
            
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/" + table)
                    .header("apikey", ANON_KEY)
                    .header("Authorization", "Bearer " + ANON_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully upserted into " + table);
                    return true;
                } else {
                    Log.e(TAG, "Failed to upsert into " + table + ": " + 
                          response.code() + " - " + response.message());
                    if (response.body() != null) {
                        Log.e(TAG, "Response body: " + response.body().string());
                    }
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error upserting into " + table, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error upserting into " + table, e);
            return false;
        }
    }
}
