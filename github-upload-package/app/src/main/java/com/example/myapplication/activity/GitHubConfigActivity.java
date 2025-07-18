package com.example.myapplication.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.github.GitHubUserManager;
import com.example.myapplication.model.User;
import com.example.myapplication.ui.MainActivity;

/**
 * GitHub配置界面
 * 用于设置GitHub Personal Access Token
 */
public class GitHubConfigActivity extends AppCompatActivity {
    private static final String TAG = "GitHubConfigActivity";
    
    private EditText etAccessToken;
    private EditText etUsername;
    private Button btnSave;
    private Button btnHelp;
    private TextView tvStatus;
    private TextView tvInstructions;
    
    private GitHubUserManager userManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_github_config);
        
        initViews();
        initData();
        setupListeners();
    }
    
    private void initViews() {
        etAccessToken = findViewById(R.id.etAccessToken);
        etUsername = findViewById(R.id.etUsername);
        btnSave = findViewById(R.id.btnSave);
        btnHelp = findViewById(R.id.btnHelp);
        tvStatus = findViewById(R.id.tvStatus);
        tvInstructions = findViewById(R.id.tvInstructions);
        
        // 设置说明文本
        tvInstructions.setText(
            "为了使用云端数据同步功能，您需要配置GitHub Personal Access Token：\n\n" +
            "1. 访问 GitHub Settings → Developer settings → Personal access tokens\n" +
            "2. 点击 'Generate new token (classic)'\n" +
            "3. 选择 'gist' 权限\n" +
            "4. 复制生成的token并粘贴到下方\n\n" +
            "注意：Token只会显示一次，请妥善保存"
        );
    }
    
    private void initData() {
        userManager = GitHubUserManager.getInstance(this);
        updateStatus();
    }
    
    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveConfiguration());
        btnHelp.setOnClickListener(v -> showHelp());
    }
    
    private void saveConfiguration() {
        String accessToken = etAccessToken.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        
        if (TextUtils.isEmpty(accessToken)) {
            Toast.makeText(this, "请输入Access Token", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入GitHub用户名", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("配置GitHub连接...");
        progressDialog.show();
        
        userManager.configureGitHub(accessToken, username, new GitHubUserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(GitHubConfigActivity.this, "GitHub配置成功", Toast.LENGTH_SHORT).show();
                    updateStatus();
                    
                    // 返回主界面
                    Intent intent = new Intent(GitHubConfigActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(GitHubConfigActivity.this, "配置失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void updateStatus() {
        if (userManager.isGitHubConfigured()) {
            tvStatus.setText("✅ GitHub已配置");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnSave.setText("重新配置");
        } else {
            tvStatus.setText("❌ GitHub未配置");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnSave.setText("保存配置");
        }
    }
    
    private void showHelp() {
        new AlertDialog.Builder(this)
            .setTitle("如何获取GitHub Personal Access Token")
            .setMessage(
                "详细步骤：\n\n" +
                "1. 登录GitHub网站\n" +
                "2. 点击右上角头像 → Settings\n" +
                "3. 左侧菜单 → Developer settings\n" +
                "4. Personal access tokens → Tokens (classic)\n" +
                "5. Generate new token (classic)\n" +
                "6. 填写Note（如：MyApplication3）\n" +
                "7. 选择Expiration（建议No expiration）\n" +
                "8. 勾选 'gist' 权限\n" +
                "9. 点击Generate token\n" +
                "10. 复制生成的token\n\n" +
                "是否现在打开GitHub设置页面？"
            )
            .setPositiveButton("打开GitHub", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://github.com/settings/tokens"));
                startActivity(intent);
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
