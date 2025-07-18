package com.example.myapplication.github;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.model.User;
import com.example.myapplication.utils.Logger;
import com.google.gson.Gson;

import java.util.UUID;

/**
 * GitHub用户管理器
 * 基于GitHub的用户数据管理，替代原有的UserManager
 */
public class GitHubUserManager {
    private static final String TAG = "GitHubUserManager";
    private static final String PREFS_NAME = "github_user_manager";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_GITHUB_CONFIGURED = "github_configured";
    
    private static volatile GitHubUserManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final GitHubDataManager gitHubDataManager;
    private User currentUser;
    
    private GitHubUserManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.gitHubDataManager = new GitHubDataManager(context);
        loadCurrentUser();
    }
    
    /**
     * 获取实例（单例模式）
     */
    public static GitHubUserManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GitHubUserManager.class) {
                if (instance == null) {
                    instance = new GitHubUserManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 配置GitHub凭据
     */
    public void configureGitHub(String accessToken, String username, UserCallback callback) {
        gitHubDataManager.setCredentials(accessToken, username);
        setGitHubConfigured(true);
        
        // 尝试从GitHub加载用户数据
        gitHubDataManager.loadUserData(new GitHubDataManager.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setCurrentUser(user);
                setLoggedIn(true);
                callback.onSuccess(user);
            }
            
            @Override
            public void onError(String error) {
                // 如果GitHub上没有数据，创建新用户
                if (error.contains("用户数据不存在")) {
                    User newUser = createNewUser(username);
                    setCurrentUser(newUser);
                    setLoggedIn(true);
                    
                    // 保存到GitHub
                    saveUserToGitHub(newUser, callback);
                } else {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * 用户登录（本地验证 + GitHub同步）
     */
    public void login(String username, String password, UserCallback callback) {
        // 简单的本地验证（实际应用中可以加强）
        if (currentUser != null && currentUser.getUsername().equals(username)) {
            setLoggedIn(true);
            
            // 如果配置了GitHub，尝试同步数据
            if (isGitHubConfigured()) {
                syncFromGitHub(callback);
            } else {
                callback.onSuccess(currentUser);
            }
        } else {
            callback.onError("用户名或密码错误");
        }
    }
    
    /**
     * 用户注册
     */
    public void register(String username, String email, String password, UserCallback callback) {
        // 检查用户是否已存在
        if (currentUser != null && currentUser.getUsername().equals(username)) {
            callback.onError("用户已存在");
            return;
        }
        
        // 创建新用户
        User newUser = new User();
        newUser.setId(UUID.randomUUID().toString());
        newUser.setUsername(username);
        newUser.setEmail(email);
        // 注意：User类没有setPassword方法，这里移除该行
        // newUser.setPassword(password); // 实际应用中应该加密
        
        setCurrentUser(newUser);
        setLoggedIn(true);
        
        // 如果配置了GitHub，保存到GitHub
        if (isGitHubConfigured()) {
            saveUserToGitHub(newUser, callback);
        } else {
            callback.onSuccess(newUser);
        }
    }
    
    /**
     * 更新用户资料
     */
    public void updateUserProfile(User user, UserCallback callback) {
        setCurrentUser(user);
        
        // 如果配置了GitHub，同步到GitHub
        if (isGitHubConfigured()) {
            saveUserToGitHub(user, callback);
        } else {
            callback.onSuccess(user);
        }
    }
    
    /**
     * 同步学习数据到GitHub
     */
    public void syncStudyData(String studyRecordsJson, String errorRecordsJson, SyncCallback callback) {
        if (!isGitHubConfigured()) {
            callback.onError("GitHub未配置");
            return;
        }
        
        // 同步学习记录
        gitHubDataManager.syncStudyRecords(studyRecordsJson, new GitHubDataManager.DataCallback() {
            @Override
            public void onSuccess(String result) {
                // 同步错题记录
                gitHubDataManager.syncErrorRecords(errorRecordsJson, new GitHubDataManager.DataCallback() {
                    @Override
                    public void onSuccess(String result2) {
                        callback.onSuccess("数据同步成功");
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("错题记录同步失败: " + error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                callback.onError("学习记录同步失败: " + error);
            }
        });
    }
    
    /**
     * 从GitHub获取学习数据
     */
    public void getStudyDataFromGitHub(StudyDataCallback callback) {
        if (!isGitHubConfigured()) {
            callback.onError("GitHub未配置");
            return;
        }
        
        // 获取学习记录
        gitHubDataManager.getStudyRecords(new GitHubDataManager.DataCallback<String>() {
            @Override
            public void onSuccess(String studyRecords) {
                // 获取错题记录
                gitHubDataManager.getErrorRecords(new GitHubDataManager.DataCallback<String>() {
                    @Override
                    public void onSuccess(String errorRecords) {
                        callback.onSuccess(studyRecords, errorRecords);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("获取错题记录失败: " + error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                callback.onError("获取学习记录失败: " + error);
            }
        });
    }
    
    /**
     * 用户登出
     */
    public void logout() {
        setLoggedIn(false);
        // 保留用户数据，只是标记为未登录状态
    }
    
    /**
     * 完全清除用户数据
     */
    public void clearUserData() {
        currentUser = null;
        setLoggedIn(false);
        setGitHubConfigured(false);
        prefs.edit().clear().apply();
        gitHubDataManager.clearAllData();
    }
    
    /**
     * 从GitHub同步用户数据
     */
    private void syncFromGitHub(UserCallback callback) {
        gitHubDataManager.loadUserData(new GitHubDataManager.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setCurrentUser(user);
                callback.onSuccess(user);
            }
            
            @Override
            public void onError(String error) {
                // 同步失败，使用本地数据
                Logger.w(TAG, "GitHub同步失败，使用本地数据: " + error);
                callback.onSuccess(currentUser);
            }
        });
    }
    
    /**
     * 保存用户到GitHub
     */
    private void saveUserToGitHub(User user, UserCallback callback) {
        gitHubDataManager.saveUserData(user, new GitHubDataManager.DataCallback() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(user);
            }
            
            @Override
            public void onError(String error) {
                // 保存失败，但本地数据已更新
                Logger.w(TAG, "GitHub保存失败，数据仅保存在本地: " + error);
                callback.onSuccess(user);
            }
        });
    }
    
    /**
     * 创建新用户
     */
    private User createNewUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(username + "@github.local"); // 临时邮箱
        return user;
    }
    
    // ==================== 基础方法 ====================
    
    public User getCurrentUser() {
        if (currentUser == null) {
            loadCurrentUser();
        }
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        saveCurrentUser();
    }
    
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }
    
    public boolean isGitHubConfigured() {
        return prefs.getBoolean(KEY_GITHUB_CONFIGURED, false) && gitHubDataManager.hasCredentials();
    }
    
    public void setGitHubConfigured(boolean configured) {
        prefs.edit().putBoolean(KEY_GITHUB_CONFIGURED, configured).apply();
    }
    
    public String getUserDisplayName() {
        User user = getCurrentUser();
        return user != null ? user.getDisplayName() : "未知用户";
    }
    
    public String getUserAvatarPath() {
        User user = getCurrentUser();
        return user != null ? user.getAvatarPath() : "avatar_default";
    }
    
    public long getLastSyncTime() {
        return gitHubDataManager.getLastSyncTime();
    }
    
    public boolean needsSync() {
        if (!isGitHubConfigured()) return false;
        
        long lastSync = getLastSyncTime();
        long now = System.currentTimeMillis();
        return (now - lastSync) > 24 * 60 * 60 * 1000; // 24小时
    }
    
    // ==================== 私有方法 ====================
    
    private void loadCurrentUser() {
        String userJson = prefs.getString(KEY_CURRENT_USER, null);
        if (userJson != null) {
            try {
                currentUser = gson.fromJson(userJson, User.class);
            } catch (Exception e) {
                Logger.e(TAG, "加载用户数据失败", e);
                currentUser = null;
            }
        }
    }
    
    private void saveCurrentUser() {
        if (currentUser != null) {
            String userJson = gson.toJson(currentUser);
            prefs.edit().putString(KEY_CURRENT_USER, userJson).apply();
        }
    }
    
    // ==================== 回调接口 ====================
    
    public interface UserCallback {
        void onSuccess(User user);
        void onError(String error);
    }
    
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface StudyDataCallback {
        void onSuccess(String studyRecords, String errorRecords);
        void onError(String error);
    }
}
