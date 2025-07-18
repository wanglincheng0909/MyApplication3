package com.example.myapplication.github;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication.model.User;
import com.example.myapplication.utils.AsyncTask;
import com.example.myapplication.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * GitHub数据管理器
 * 使用GitHub Gist作为用户数据存储
 */
public class GitHubDataManager {
    private static final String TAG = "GitHubDataManager";
    
    // GitHub API配置
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GISTS_ENDPOINT = GITHUB_API_BASE + "/gists";
    
    // 本地存储配置
    private static final String PREFS_NAME = "github_data_manager";
    private static final String KEY_ACCESS_TOKEN = "github_access_token";
    private static final String KEY_USERNAME = "github_username";
    private static final String KEY_USER_GIST_ID = "user_gist_id";
    private static final String KEY_CACHED_USER_DATA = "cached_user_data";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    
    // Gist文件名
    private static final String USER_DATA_FILE = "user_data.json";
    private static final String STUDY_RECORDS_FILE = "study_records.json";
    private static final String ERROR_RECORDS_FILE = "error_records.json";
    private static final String APP_SETTINGS_FILE = "app_settings.json";
    
    private Context context;
    private SharedPreferences prefs;
    private Gson gson;
    private String accessToken;
    private String username;
    
    public GitHubDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadCredentials();
    }
    
    /**
     * 设置GitHub访问凭据
     */
    public void setCredentials(String accessToken, String username) {
        this.accessToken = accessToken;
        this.username = username;
        
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_USERNAME, username)
            .apply();
    }
    
    /**
     * 检查是否已配置GitHub凭据
     */
    public boolean hasCredentials() {
        return accessToken != null && !accessToken.isEmpty() && 
               username != null && !username.isEmpty();
    }
    
    /**
     * 创建或更新用户数据Gist
     */
    public void saveUserData(User user, DataCallback callback) {
        if (!hasCredentials()) {
            callback.onError("GitHub凭据未配置");
            return;
        }
        
        AsyncTask.execute(() -> {
            try {
                // 准备用户数据
                JsonObject userData = new JsonObject();
                userData.addProperty("id", user.getId());
                userData.addProperty("username", user.getUsername());
                userData.addProperty("email", user.getEmail());
                userData.addProperty("nickname", user.getNickname());
                userData.addProperty("realName", user.getRealName());
                userData.addProperty("avatar", user.getAvatar());
                userData.addProperty("gender", user.getGender() != null ? user.getGender().name() : null);
                userData.addProperty("grade", user.getGrade());
                userData.addProperty("school", user.getSchool());
                userData.addProperty("lastModified", new Date().getTime());
                
                // 获取或创建Gist
                String gistId = getUserGistId();
                if (gistId == null) {
                    gistId = createUserGist(userData.toString());
                    setUserGistId(gistId);
                } else {
                    updateUserGist(gistId, userData.toString());
                }
                
                // 缓存到本地
                cacheUserData(user);
                updateLastSyncTime();
                
                callback.onSuccess("用户数据保存成功");
                
            } catch (Exception e) {
                Logger.e(TAG, "保存用户数据失败", e);
                callback.onError("保存失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 从GitHub加载用户数据
     */
    public void loadUserData(DataCallback<User> callback) {
        if (!hasCredentials()) {
            // 尝试从本地缓存加载
            User cachedUser = getCachedUserData();
            if (cachedUser != null) {
                callback.onSuccess(cachedUser);
            } else {
                callback.onError("GitHub凭据未配置且无本地缓存");
            }
            return;
        }
        
        AsyncTask.execute(() -> {
            try {
                String gistId = getUserGistId();
                if (gistId == null) {
                    // 没有Gist，返回本地缓存或null
                    User cachedUser = getCachedUserData();
                    if (cachedUser != null) {
                        callback.onSuccess(cachedUser);
                    } else {
                        callback.onError("用户数据不存在");
                    }
                    return;
                }
                
                // 从GitHub获取数据
                String gistContent = getGistContent(gistId, USER_DATA_FILE);
                if (gistContent != null) {
                    JsonObject userData = JsonParser.parseString(gistContent).getAsJsonObject();
                    User user = parseUserFromJson(userData);
                    
                    // 缓存到本地
                    cacheUserData(user);
                    updateLastSyncTime();
                    
                    callback.onSuccess(user);
                } else {
                    callback.onError("无法获取用户数据");
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "加载用户数据失败", e);
                
                // 尝试返回本地缓存
                User cachedUser = getCachedUserData();
                if (cachedUser != null) {
                    callback.onSuccess(cachedUser);
                } else {
                    callback.onError("加载失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步学习记录到GitHub
     */
    public void syncStudyRecords(String studyRecordsJson, DataCallback callback) {
        if (!hasCredentials()) {
            callback.onError("GitHub凭据未配置");
            return;
        }
        
        AsyncTask.execute(() -> {
            try {
                String gistId = getUserGistId();
                if (gistId != null) {
                    updateGistFile(gistId, STUDY_RECORDS_FILE, studyRecordsJson);
                    callback.onSuccess("学习记录同步成功");
                } else {
                    callback.onError("用户Gist不存在");
                }
            } catch (Exception e) {
                Logger.e(TAG, "同步学习记录失败", e);
                callback.onError("同步失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 同步错题记录到GitHub
     */
    public void syncErrorRecords(String errorRecordsJson, DataCallback callback) {
        if (!hasCredentials()) {
            callback.onError("GitHub凭据未配置");
            return;
        }
        
        AsyncTask.execute(() -> {
            try {
                String gistId = getUserGistId();
                if (gistId != null) {
                    updateGistFile(gistId, ERROR_RECORDS_FILE, errorRecordsJson);
                    callback.onSuccess("错题记录同步成功");
                } else {
                    callback.onError("用户Gist不存在");
                }
            } catch (Exception e) {
                Logger.e(TAG, "同步错题记录失败", e);
                callback.onError("同步失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 获取学习记录
     */
    public void getStudyRecords(DataCallback<String> callback) {
        if (!hasCredentials()) {
            callback.onError("GitHub凭据未配置");
            return;
        }
        
        AsyncTask.execute(() -> {
            try {
                String gistId = getUserGistId();
                if (gistId != null) {
                    String content = getGistContent(gistId, STUDY_RECORDS_FILE);
                    callback.onSuccess(content != null ? content : "[]");
                } else {
                    callback.onError("用户Gist不存在");
                }
            } catch (Exception e) {
                Logger.e(TAG, "获取学习记录失败", e);
                callback.onError("获取失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 获取错题记录
     */
    public void getErrorRecords(DataCallback<String> callback) {
        if (!hasCredentials()) {
            callback.onError("GitHub凭据未配置");
            return;
        }
        
        AsyncTask.execute(() -> {
            try {
                String gistId = getUserGistId();
                if (gistId != null) {
                    String content = getGistContent(gistId, ERROR_RECORDS_FILE);
                    callback.onSuccess(content != null ? content : "[]");
                } else {
                    callback.onError("用户Gist不存在");
                }
            } catch (Exception e) {
                Logger.e(TAG, "获取错题记录失败", e);
                callback.onError("获取失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 创建用户Gist
     */
    private String createUserGist(String userData) throws Exception {
        JSONObject gistData = new JSONObject();
        gistData.put("description", "MyApplication3 User Data - " + username);
        gistData.put("public", false);
        
        JSONObject files = new JSONObject();
        
        // 用户数据文件
        JSONObject userDataFile = new JSONObject();
        userDataFile.put("content", userData);
        files.put(USER_DATA_FILE, userDataFile);
        
        // 初始化其他文件
        JSONObject studyRecordsFile = new JSONObject();
        studyRecordsFile.put("content", "[]");
        files.put(STUDY_RECORDS_FILE, studyRecordsFile);
        
        JSONObject errorRecordsFile = new JSONObject();
        errorRecordsFile.put("content", "[]");
        files.put(ERROR_RECORDS_FILE, errorRecordsFile);
        
        JSONObject settingsFile = new JSONObject();
        settingsFile.put("content", "{}");
        files.put(APP_SETTINGS_FILE, settingsFile);
        
        gistData.put("files", files);
        
        String response = makeGitHubRequest("POST", GISTS_ENDPOINT, gistData.toString());
        JSONObject responseJson = new JSONObject(response);
        
        return responseJson.getString("id");
    }
    
    /**
     * 更新用户Gist
     */
    private void updateUserGist(String gistId, String userData) throws Exception {
        updateGistFile(gistId, USER_DATA_FILE, userData);
    }
    
    /**
     * 更新Gist中的特定文件
     */
    private void updateGistFile(String gistId, String fileName, String content) throws Exception {
        JSONObject gistData = new JSONObject();
        JSONObject files = new JSONObject();
        JSONObject file = new JSONObject();
        file.put("content", content);
        files.put(fileName, file);
        gistData.put("files", files);
        
        String endpoint = GISTS_ENDPOINT + "/" + gistId;
        makeGitHubRequest("PATCH", endpoint, gistData.toString());
    }
    
    /**
     * 获取Gist文件内容
     */
    private String getGistContent(String gistId, String fileName) throws Exception {
        String endpoint = GISTS_ENDPOINT + "/" + gistId;
        String response = makeGitHubRequest("GET", endpoint, null);
        
        JSONObject gistJson = new JSONObject(response);
        JSONObject files = gistJson.getJSONObject("files");
        
        if (files.has(fileName)) {
            JSONObject file = files.getJSONObject(fileName);
            return file.getString("content");
        }
        
        return null;
    }
    
    /**
     * 发起GitHub API请求
     */
    private String makeGitHubRequest(String method, String endpoint, String body) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "token " + accessToken);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "MyApplication3-Android");
            
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
            }
            
            int responseCode = connection.getResponseCode();
            
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            if (responseCode >= 400) {
                throw new Exception("GitHub API错误: " + responseCode + " - " + response.toString());
            }
            
            return response.toString();
            
        } finally {
            connection.disconnect();
        }
    }
    
    // ==================== 本地缓存方法 ====================
    
    private void loadCredentials() {
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        username = prefs.getString(KEY_USERNAME, null);
    }
    
    private String getUserGistId() {
        return prefs.getString(KEY_USER_GIST_ID, null);
    }
    
    private void setUserGistId(String gistId) {
        prefs.edit().putString(KEY_USER_GIST_ID, gistId).apply();
    }
    
    private void cacheUserData(User user) {
        String userJson = gson.toJson(user);
        prefs.edit().putString(KEY_CACHED_USER_DATA, userJson).apply();
    }
    
    private User getCachedUserData() {
        String userJson = prefs.getString(KEY_CACHED_USER_DATA, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }
    
    private void updateLastSyncTime() {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply();
    }
    
    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0);
    }
    
    /**
     * 从JSON解析用户对象
     */
    private User parseUserFromJson(JsonObject userData) {
        User user = new User();
        
        if (userData.has("id")) user.setId(userData.get("id").getAsString());
        if (userData.has("username")) user.setUsername(userData.get("username").getAsString());
        if (userData.has("email")) user.setEmail(userData.get("email").getAsString());
        if (userData.has("nickname")) user.setNickname(userData.get("nickname").getAsString());
        if (userData.has("realName")) user.setRealName(userData.get("realName").getAsString());
        if (userData.has("avatar")) user.setAvatar(userData.get("avatar").getAsString());
        if (userData.has("grade")) user.setGrade(userData.get("grade").getAsString());
        if (userData.has("school")) user.setSchool(userData.get("school").getAsString());
        
        if (userData.has("gender") && !userData.get("gender").isJsonNull()) {
            String genderStr = userData.get("gender").getAsString();
            try {
                user.setGender(User.Gender.valueOf(genderStr));
            } catch (IllegalArgumentException e) {
                user.setGender(User.Gender.PREFER_NOT_TO_SAY);
            }
        }
        
        return user;
    }
    
    /**
     * 清除所有数据
     */
    public void clearAllData() {
        prefs.edit().clear().apply();
        accessToken = null;
        username = null;
    }
    
    // ==================== 回调接口 ====================
    
    public interface DataCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public interface DataCallback {
        void onSuccess(String result);
        void onError(String error);
    }
}
