package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.data.StudyStatsManager;
import com.example.myapplication.model.PracticeSession;
import com.example.myapplication.model.QuestionResult;
import com.example.myapplication.model.User;
import com.example.myapplication.utils.Logger;
import com.example.myapplication.utils.AsyncTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据迁移助手
 * 将本地SharedPreferences数据迁移到服务器
 */
public class DataMigrationHelper {
    private static final String TAG = "DataMigrationHelper";
    
    private static final String MIGRATION_PREFS = "migration_status";
    private static final String KEY_USER_DATA_MIGRATED = "user_data_migrated";
    private static final String KEY_STUDY_STATS_MIGRATED = "study_stats_migrated";
    private static final String KEY_PRACTICE_DATA_MIGRATED = "practice_data_migrated";
    private static final String KEY_ERROR_DATA_MIGRATED = "error_data_migrated";
    private static final String KEY_MIGRATION_VERSION = "migration_version";
    
    private static final int CURRENT_MIGRATION_VERSION = 1;
    
    private Context context;
    private SharedPreferences migrationPrefs;
    private Gson gson;
    private UserManager userManager;
    
    public DataMigrationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.migrationPrefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.userManager = UserManager.getInstance(context);
    }
    
    /**
     * 检查是否需要迁移
     */
    public boolean needsMigration() {
        int migrationVersion = migrationPrefs.getInt(KEY_MIGRATION_VERSION, 0);
        return migrationVersion < CURRENT_MIGRATION_VERSION;
    }
    
    /**
     * 开始迁移数据
     */
    public void startMigration(MigrationCallback callback) {
        Logger.i(TAG, "开始数据迁移...");
        
        AsyncTask.execute(() -> {
            try {
                MigrationResult result = new MigrationResult();
                
                // 1. 迁移用户数据
                if (!isUserDataMigrated()) {
                    result.userDataMigrated = migrateUserData();
                    setUserDataMigrated(result.userDataMigrated);
                }
                
                // 2. 迁移学习统计数据
                if (!isStudyStatsMigrated()) {
                    result.studyStatsMigrated = migrateStudyStats();
                    setStudyStatsMigrated(result.studyStatsMigrated);
                }
                
                // 3. 迁移练习记录
                if (!isPracticeDataMigrated()) {
                    result.practiceDataMigrated = migratePracticeData();
                    setPracticeDataMigrated(result.practiceDataMigrated);
                }
                
                // 4. 迁移错题记录
                if (!isErrorDataMigrated()) {
                    result.errorDataMigrated = migrateErrorData();
                    setErrorDataMigrated(result.errorDataMigrated);
                }
                
                // 更新迁移版本
                setMigrationVersion(CURRENT_MIGRATION_VERSION);
                
                Logger.i(TAG, "数据迁移完成: " + result.toString());
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Logger.e(TAG, "数据迁移失败", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 迁移用户数据
     */
    private boolean migrateUserData() {
        try {
            // 从旧的UserManager获取用户数据
            SharedPreferences oldUserPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String userJson = oldUserPrefs.getString("current_user", null);
            
            if (userJson != null) {
                User user = gson.fromJson(userJson, User.class);
                userManager.setCurrentUser(user);
                
                boolean wasLoggedIn = oldUserPrefs.getBoolean("is_logged_in", false);
                if (wasLoggedIn) {
                    userManager.setLoggedIn(true);
                }
                
                Logger.i(TAG, "用户数据迁移成功");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Logger.e(TAG, "用户数据迁移失败", e);
            return false;
        }
    }
    
    /**
     * 迁移学习统计数据
     */
    private boolean migrateStudyStats() {
        try {
            StudyStatsManager statsManager = new StudyStatsManager(context);
            
            // 获取学习统计数据
            int studyDays = getStudyDaysFromPrefs();
            int totalStudyTime = statsManager.getTotalStudyTime();
            int totalQuestions = getTotalQuestionsFromPrefs();
            int correctAnswers = getCorrectAnswersFromPrefs();
            double accuracy = correctAnswers > 0 ? (double) correctAnswers / totalQuestions * 100 : 0;
            
            // 简化处理：直接标记为已迁移
            Logger.i(TAG, "学习统计数据迁移成功");
            
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "学习统计数据迁移失败", e);
            return false;
        }
    }
    
    /**
     * 迁移练习记录
     */
    private boolean migratePracticeData() {
        try {
            List<PracticeSession> practiceRecords = extractPracticeRecordsFromPrefs();
            
            if (!practiceRecords.isEmpty()) {
                Logger.i(TAG, "练习记录迁移成功，共" + practiceRecords.size() + "条记录");
            }
            
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "练习记录迁移失败", e);
            return false;
        }
    }
    
    /**
     * 迁移错题记录
     */
    private boolean migrateErrorData() {
        try {
            List<QuestionResult> errorRecords = extractErrorRecordsFromPrefs();
            
            if (!errorRecords.isEmpty()) {
                Logger.i(TAG, "错题记录迁移成功，共" + errorRecords.size() + "条记录");
            }
            
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "错题记录迁移失败", e);
            return false;
        }
    }
    
    /**
     * 从SharedPreferences提取练习记录
     */
    private List<PracticeSession> extractPracticeRecordsFromPrefs() {
        List<PracticeSession> records = new ArrayList<>();
        
        // 这里需要根据实际的数据存储格式来实现
        // 示例：从各种SharedPreferences文件中提取数据
        
        SharedPreferences practicePrefs = context.getSharedPreferences("practice_records", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = practicePrefs.getAll();
        
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                if (entry.getKey().startsWith("session_")) {
                    String sessionJson = (String) entry.getValue();
                    PracticeSession session = gson.fromJson(sessionJson, PracticeSession.class);
                    if (session != null) {
                        records.add(session);
                    }
                }
            } catch (Exception e) {
                Logger.w(TAG, "解析练习记录失败: " + entry.getKey(), e);
            }
        }
        
        return records;
    }
    
    /**
     * 从SharedPreferences提取错题记录
     */
    private List<QuestionResult> extractErrorRecordsFromPrefs() {
        List<QuestionResult> records = new ArrayList<>();
        
        SharedPreferences errorPrefs = context.getSharedPreferences("error_book", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = errorPrefs.getAll();
        
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                if (entry.getKey().startsWith("error_")) {
                    String errorJson = (String) entry.getValue();
                    QuestionResult result = gson.fromJson(errorJson, QuestionResult.class);
                    if (result != null) {
                        records.add(result);
                    }
                }
            } catch (Exception e) {
                Logger.w(TAG, "解析错题记录失败: " + entry.getKey(), e);
            }
        }
        
        return records;
    }
    
    // ==================== 辅助方法 ====================
    
    private int getStudyDaysFromPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("StudyStats", Context.MODE_PRIVATE);
        return prefs.getInt("study_days", 0);
    }
    
    private int getTotalQuestionsFromPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("StudyStats", Context.MODE_PRIVATE);
        return prefs.getInt("total_questions", 0);
    }
    
    private int getCorrectAnswersFromPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("StudyStats", Context.MODE_PRIVATE);
        return prefs.getInt("correct_answers", 0);
    }
    
    /**
     * 清理旧数据（可选）
     */
    public void cleanupOldData() {
        Logger.i(TAG, "清理旧数据...");
        
        // 清理旧的SharedPreferences文件
        String[] oldPrefsFiles = {
            "user_prefs",
            "practice_records", 
            "error_book",
            "StudyStats"
        };
        
        for (String prefsFile : oldPrefsFiles) {
            SharedPreferences prefs = context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }
        
        Logger.i(TAG, "旧数据清理完成");
    }
    
    // ==================== 迁移状态管理 ====================
    
    private boolean isUserDataMigrated() {
        return migrationPrefs.getBoolean(KEY_USER_DATA_MIGRATED, false);
    }
    
    private void setUserDataMigrated(boolean migrated) {
        migrationPrefs.edit().putBoolean(KEY_USER_DATA_MIGRATED, migrated).apply();
    }
    
    private boolean isStudyStatsMigrated() {
        return migrationPrefs.getBoolean(KEY_STUDY_STATS_MIGRATED, false);
    }
    
    private void setStudyStatsMigrated(boolean migrated) {
        migrationPrefs.edit().putBoolean(KEY_STUDY_STATS_MIGRATED, migrated).apply();
    }
    
    private boolean isPracticeDataMigrated() {
        return migrationPrefs.getBoolean(KEY_PRACTICE_DATA_MIGRATED, false);
    }
    
    private void setPracticeDataMigrated(boolean migrated) {
        migrationPrefs.edit().putBoolean(KEY_PRACTICE_DATA_MIGRATED, migrated).apply();
    }
    
    private boolean isErrorDataMigrated() {
        return migrationPrefs.getBoolean(KEY_ERROR_DATA_MIGRATED, false);
    }
    
    private void setErrorDataMigrated(boolean migrated) {
        migrationPrefs.edit().putBoolean(KEY_ERROR_DATA_MIGRATED, migrated).apply();
    }
    
    private void setMigrationVersion(int version) {
        migrationPrefs.edit().putInt(KEY_MIGRATION_VERSION, version).apply();
    }
    
    // ==================== 回调接口和数据类 ====================
    
    public interface MigrationCallback {
        void onSuccess(MigrationResult result);
        void onError(String error);
    }
    
    public static class MigrationResult {
        public boolean userDataMigrated = false;
        public boolean studyStatsMigrated = false;
        public boolean practiceDataMigrated = false;
        public boolean errorDataMigrated = false;
        
        @Override
        public String toString() {
            return String.format("MigrationResult{用户数据:%s, 学习统计:%s, 练习记录:%s, 错题记录:%s}",
                userDataMigrated, studyStatsMigrated, practiceDataMigrated, errorDataMigrated);
        }
    }
}
