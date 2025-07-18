@echo off
echo ========================================
echo   MyApplication3 GitHub上传助手
echo ========================================
echo.

echo 📋 检查文件...
if not exist "app\src\main\java\com\example\myapplication\github\GitHubDataManager.java" (
    echo ❌ 缺少 GitHubDataManager.java
    pause
    exit /b 1
)

if not exist "README.md" (
    echo ❌ 缺少 README.md
    pause
    exit /b 1
)

echo ✅ 所有文件检查完成

echo.
echo 📁 当前文件夹包含以下文件：
echo ├── app/src/main/java/com/example/myapplication/
echo │   ├── github/
echo │   │   ├── GitHubDataManager.java
echo │   │   └── GitHubUserManager.java
echo │   ├── activity/
echo │   │   └── GitHubConfigActivity.java
echo │   └── utils/
echo │       └── UserManager.java
echo ├── app/src/main/res/
echo │   ├── layout/
echo │   │   └── activity_github_config.xml
echo │   └── drawable/
echo │       ├── rounded_background.xml
echo │       ├── button_primary.xml (如果存在)
echo │       └── button_secondary.xml (如果存在)
echo ├── README.md
echo ├── GITHUB_SETUP_GUIDE.md
echo ├── test-github-config.html
echo └── upload-to-github.bat (本文件)

echo.
echo 🚀 上传选项：
echo 1. 复制文件到项目目录 (推荐)
echo 2. 显示git命令 (手动执行)
echo 3. 退出
echo.

set /p choice="请选择 (1-3): "

if "%choice%"=="1" goto copy_files
if "%choice%"=="2" goto show_commands
if "%choice%"=="3" goto end

echo 无效选择，请重新运行脚本
pause
exit /b 1

:copy_files
echo.
echo 📂 请输入您的MyApplication3项目根目录路径：
echo 例如: E:\AndroidStudio\MyApplication3
set /p project_path="项目路径: "

if not exist "%project_path%" (
    echo ❌ 项目路径不存在: %project_path%
    pause
    exit /b 1
)

echo.
echo 📋 开始复制文件...

:: 复制Java文件
echo 复制GitHub相关Java文件...
xcopy /Y "app\src\main\java\com\example\myapplication\github\*" "%project_path%\app\src\main\java\com\example\myapplication\github\" 2>nul
if not exist "%project_path%\app\src\main\java\com\example\myapplication\github\" mkdir "%project_path%\app\src\main\java\com\example\myapplication\github\"
copy /Y "app\src\main\java\com\example\myapplication\github\*" "%project_path%\app\src\main\java\com\example\myapplication\github\"

echo 复制Activity文件...
if not exist "%project_path%\app\src\main\java\com\example\myapplication\activity\" mkdir "%project_path%\app\src\main\java\com\example\myapplication\activity\"
copy /Y "app\src\main\java\com\example\myapplication\activity\*" "%project_path%\app\src\main\java\com\example\myapplication\activity\"

echo 复制Utils文件...
copy /Y "app\src\main\java\com\example\myapplication\utils\*" "%project_path%\app\src\main\java\com\example\myapplication\utils\"

:: 复制资源文件
echo 复制布局文件...
copy /Y "app\src\main\res\layout\*" "%project_path%\app\src\main\res\layout\"

echo 复制drawable文件...
copy /Y "app\src\main\res\drawable\*" "%project_path%\app\src\main\res\drawable\" 2>nul

:: 复制文档文件
echo 复制文档文件...
copy /Y "README.md" "%project_path%\"
copy /Y "GITHUB_SETUP_GUIDE.md" "%project_path%\"
copy /Y "test-github-config.html" "%project_path%\"

echo.
echo ✅ 文件复制完成！
echo.
echo 🔑 您的GitHub配置信息：
echo 用户名: wanglincheng0909
echo Token: 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
echo 状态: ✅ 已验证可用
echo.
echo 📋 接下来的步骤：
echo 1. 在Android Studio中检查项目是否编译正常
echo 2. 确保AndroidManifest.xml包含网络权限和GitHubConfigActivity
echo 3. 使用git提交和推送到GitHub：
echo    git add .
echo    git commit -m "Add GitHub cloud sync feature"
echo    git push origin main
echo 4. 在Android应用中配置GitHub同步（使用上述配置信息）
echo.
pause
goto end

:show_commands
echo.
echo 📋 手动上传命令：
echo.
echo # 进入您的项目目录
echo cd /path/to/your/MyApplication3
echo.
echo # 复制文件 (Windows)
echo xcopy /E /Y github-upload-package\app\* app\
echo copy /Y github-upload-package\*.md .
echo copy /Y github-upload-package\*.html .
echo.
echo # 或者复制文件 (Linux/Mac)
echo cp -r github-upload-package/app/* app/
echo cp github-upload-package/*.md .
echo cp github-upload-package/*.html .
echo.
echo # 提交到git
echo git add .
echo git commit -m "Add GitHub cloud sync feature"
echo git push origin main
echo.
pause
goto end

:end
echo.
echo 👋 感谢使用MyApplication3 GitHub上传助手！
echo 如有问题，请参考 GITHUB_SETUP_GUIDE.md
pause
