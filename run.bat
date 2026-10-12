@echo off
chcp 65001 > nul
echo ===================================================
echo     DỰ ÁN NET SEND MULTICAST - VKU LẬP TRÌNH MẠNG
echo ===================================================
echo Đang biên dịch mã nguồn Java...
javac -encoding UTF-8 -d bin src/module-info.java src/multicast/*.java
if %errorlevel% neq 0 (
    echo [LỖI] Biên dịch thất bại!
    pause
    exit /b %errorlevel%
)
echo Biên dịch thành công! Đang khởi động chương trình...
java -cp bin multicast.NetSendApp %*
pause
