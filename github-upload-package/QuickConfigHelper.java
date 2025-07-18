package com.example.myapplication.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.myapplication.github.GitHubUserManager;
import com.example.myapplication.model.User;

/**
 * 快速配置助手
 * 帮助快速设置GitHub配置（仅用于测试，生产环境请删除）
 */
public class QuickConfigHelper {
    
    // ⚠️ 警告：这些是测试配置，生产环境中请删除此文件
    private static final String TEST_GITHUB_USERNAME = "wanglincheng0909";
    private static final String TEST_GITHUB_TOKEN = "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";
    
    /**
     * 快速配置GitHub（仅用于测试）
     * ⚠️ 生产环境请删除此方法
     */
    public static void quickSetupGitHub(Context context) {
        GitHubUserManager userManager = GitHubUserManager.getInstance(context);
        
        userManager.configureGitHub(TEST_GITHUB_TOKEN, TEST_GITHUB_USERNAME, 
            new GitHubUserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    Toast.makeText(context, "GitHub配置成功！", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(context, "配置失败: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }
    
    /**
     * 检查是否为测试配置
     */
    public static boolean isTestConfiguration(Context context) {
        GitHubUserManager userManager = GitHubUserManager.getInstance(context);
        return userManager.isGitHubConfigured();
    }
    
    /**
     * 清除测试配置
     */
    public static void clearTestConfiguration(Context context) {
        GitHubUserManager userManager = GitHubUserManager.getInstance(context);
        userManager.clearUserData();
        Toast.makeText(context, "已清除GitHub配置", Toast.LENGTH_SHORT).show();
    }
}

/*
使用方法：

1. 在您的Activity中添加以下代码进行快速测试：

// 在onCreate或按钮点击事件中
QuickConfigHelper.quickSetupGitHub(this);

2. 测试完成后，请删除此文件以确保安全！

3. 正式使用时，请通过GitHubConfigActivity界面进行配置。
*/
