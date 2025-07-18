package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.github.GitHubUserManager;
import com.example.myapplication.model.User;
import com.example.myapplication.utils.Logger;
import com.google.gson.Gson;

/**
 * 用户管理器 - 兼容版本
 * 优先使用GitHub云端同步，降级到本地存储
 */
public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private static volatile UserManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final GitHubUserManager gitHubUserManager;
    private User currentUser;
    
    private UserManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        this.gitHubUserManager = GitHubUserManager.getInstance(context);
        loadCurrentUser();
    }
    
    /**
     * 获取UserManager实例（单例模式）
     */
    public static UserManager getInstance(Context context) {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        saveCurrentUser();
        setLoggedIn(true);
        Logger.i(TAG, "设置当前用户: " + (user != null ? user.getUsername() : "null"));
    }
    
    /**
     * 获取当前用户
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            loadCurrentUser();
        }
        
        // 如果仍然为null，创建一个默认用户
        if (currentUser == null) {
            currentUser = createDefaultUser();
        }
        
        return currentUser;
    }
    
    /**
     * 创建默认用户
     */
    private User createDefaultUser() {
        User defaultUser = new User();
        defaultUser.setId("default_user");
        defaultUser.setUsername("学习者");
        defaultUser.setNickname("学习者");
        defaultUser.setEmail("learner@example.com");
        defaultUser.setUserType(User.UserType.STUDENT);
        defaultUser.setStatus(User.UserStatus.ACTIVE);
        defaultUser.setLevel(1);
        defaultUser.setTotalPoints(0);
        defaultUser.setExperience(0);
        
        // 创建默认学习统计
        User.LearningStats stats = new User.LearningStats();
        stats.setTotalStudyTime(0);
        stats.setStudyDays(0);
        stats.setCompletedCourses(0);
        stats.setCorrectAnswers(0);
        stats.setTotalAnswers(0);
        stats.setAccuracy(0.0);
        defaultUser.setLearningStats(stats);
        
        Logger.i(TAG, "创建默认用户");
        return defaultUser;
    }
    
    /**
     * 检查用户是否已登录
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * 设置登录状态
     */
    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
        Logger.i(TAG, "设置登录状态: " + loggedIn);
    }
    
    /**
     * 用户登录
     */
    public void login(User user) {
        setCurrentUser(user);
        Logger.i(TAG, "用户登录: " + user.getUsername());
    }
    
    /**
     * 用户登出
     */
    public void logout() {
        currentUser = null;
        setLoggedIn(false);
        clearUserData();
        Logger.i(TAG, "用户登出");
    }
    
    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        if (currentUser != null && user != null) {
            this.currentUser = user;
            saveCurrentUser();
            Logger.i(TAG, "更新用户信息: " + user.getUsername());
        }
    }
    
    /**
     * 获取用户显示名称
     */
    public String getUserDisplayName() {
        User user = getCurrentUser();
        return user != null ? user.getDisplayName() : "未知用户";
    }
    
    /**
     * 获取用户头像路径
     */
    public String getUserAvatarPath() {
        User user = getCurrentUser();
        return user != null ? user.getAvatarPath() : "avatar_default";
    }
    
    /**
     * 保存当前用户到SharedPreferences
     */
    private void saveCurrentUser() {
        if (currentUser != null) {
            String userJson = gson.toJson(currentUser);
            prefs.edit().putString(KEY_CURRENT_USER, userJson).apply();
            Logger.d(TAG, "保存用户信息到本地");
        }
    }
    
    /**
     * 从SharedPreferences加载当前用户
     */
    private void loadCurrentUser() {
        String userJson = prefs.getString(KEY_CURRENT_USER, null);
        if (userJson != null) {
            try {
                currentUser = gson.fromJson(userJson, User.class);
                Logger.d(TAG, "从本地加载用户信息: " + (currentUser != null ? currentUser.getUsername() : "null"));
            } catch (Exception e) {
                Logger.e(TAG, "加载用户信息失败", e);
                currentUser = null;
            }
        }
    }
    
    /**
     * 清除用户数据
     */
    private void clearUserData() {
        prefs.edit()
                .remove(KEY_CURRENT_USER)
                .remove(KEY_IS_LOGGED_IN)
                .apply();
        Logger.d(TAG, "清除用户数据");
    }
    
    /**
     * 更新用户学习统计
     */
    public void updateLearningStats(int studyTime, int correctAnswers, int totalAnswers) {
        User user = getCurrentUser();
        if (user != null) {
            User.LearningStats stats = user.getLearningStats();
            if (stats == null) {
                stats = new User.LearningStats();
                user.setLearningStats(stats);
            }
            
            // 更新统计数据
            stats.setTotalStudyTime(stats.getTotalStudyTime() + studyTime);
            stats.setCorrectAnswers(stats.getCorrectAnswers() + correctAnswers);
            stats.setTotalAnswers(stats.getTotalAnswers() + totalAnswers);
            
            // 计算准确率
            if (stats.getTotalAnswers() > 0) {
                double accuracy = (double) stats.getCorrectAnswers() / stats.getTotalAnswers();
                stats.setAccuracy(accuracy);
            }
            
            // 增加学习天数（简化处理）
            stats.setStudyDays(stats.getStudyDays() + 1);
            
            updateUser(user);
            Logger.i(TAG, "更新学习统计数据");
        }
    }
    
    /**
     * 增加用户经验值
     */
    public void addExperience(int exp) {
        User user = getCurrentUser();
        if (user != null) {
            int currentExp = user.getExperience() + exp;
            user.setExperience(currentExp);
            
            // 简单的升级逻辑
            int newLevel = (currentExp / 100) + 1;
            if (newLevel > user.getLevel()) {
                user.setLevel(newLevel);
                Logger.i(TAG, "用户升级到等级: " + newLevel);
            }
            
            updateUser(user);
        }
    }
    
    /**
     * 增加用户积分
     */
    public void addPoints(int points) {
        User user = getCurrentUser();
        if (user != null) {
            user.setTotalPoints(user.getTotalPoints() + points);
            updateUser(user);
            Logger.i(TAG, "增加用户积分: " + points);
        }
    }

    // ==================== GitHub云端同步功能 ====================

    /**
     * 检查是否配置了GitHub云端同步
     */
    public boolean isGitHubConfigured() {
        return gitHubUserManager.isGitHubConfigured();
    }

    /**
     * 配置GitHub云端同步
     */
    public void configureGitHub(String accessToken, String username, GitHubCallback callback) {
        gitHubUserManager.configureGitHub(accessToken, username, new GitHubUserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // 更新本地用户数据
                setCurrentUser(user);
                setLoggedIn(true);
                callback.onSuccess("GitHub配置成功");
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 同步数据到GitHub
     */
    public void syncToGitHub(String studyRecordsJson, String errorRecordsJson, GitHubCallback callback) {
        if (!isGitHubConfigured()) {
            callback.onError("GitHub未配置");
            return;
        }

        gitHubUserManager.syncStudyData(studyRecordsJson, errorRecordsJson, new GitHubUserManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 从GitHub获取数据
     */
    public void getDataFromGitHub(StudyDataCallback callback) {
        if (!isGitHubConfigured()) {
            callback.onError("GitHub未配置");
            return;
        }

        gitHubUserManager.getStudyDataFromGitHub(new GitHubUserManager.StudyDataCallback() {
            @Override
            public void onSuccess(String studyRecords, String errorRecords) {
                callback.onSuccess(studyRecords, errorRecords);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 获取最后同步时间
     */
    public long getLastSyncTime() {
        return gitHubUserManager.getLastSyncTime();
    }

    /**
     * 检查是否需要同步
     */
    public boolean needsSync() {
        return gitHubUserManager.needsSync();
    }

    // ==================== 回调接口 ====================

    public interface GitHubCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface StudyDataCallback {
        void onSuccess(String studyRecords, String errorRecords);
        void onError(String error);
    }
}
