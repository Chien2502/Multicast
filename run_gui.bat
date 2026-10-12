@echo off
chcp 65001 > nul
echo ===================================================
echo     VKU - NET SEND MULTICAST DESKTOP (SWING GUI)
echo ===================================================
echo Đang kiểm tra và biên dịch mã nguồn...
javac -encoding UTF-8 -d bin src/module-info.java src/multicast/*.java
if %errorlevel% neq 0 (
    echo [LỖI] Biên dịch thất bại!
    pause
    exit /b %errorlevel%
)
echo Khởi động giao diện Desktop Swing...
start "Net Send GUI" javaw -cp bin multicast.NetSendGui %*
exit
