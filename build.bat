@echo off
echo 正在构建Utopia Bosses模组并自动安装到Minecraft...

if exist "%USERPROFILE%\.gradle\wrapper\dists" (
  echo 使用Gradle Wrapper构建...
  if exist "gradlew.bat" (
    call gradlew.bat build
  ) else (
    echo 未找到gradlew.bat，尝试使用全局Gradle...
    gradle build
  )
) else (
  echo 尝试使用全局Gradle...
  gradle build
)

echo 构建完成，JAR文件应已自动复制到Minecraft的mods目录
echo 目标路径: D:\客户端\MC\.minecraft\versions\1.20.1-Fabric 0.16.10\mods\
pause 