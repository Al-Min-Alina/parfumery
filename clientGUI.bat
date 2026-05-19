@echo off
chcp 65001 > nul
cd /d D:\Yandex.Disk\progSP\program\untitled
C:\Users\Lenovo\.jdks\openjdk-18.0.2\bin\java.exe --module-path "D:\NewFolder\MySQL\javafx-sdk-21.0.5\lib" --add-modules javafx.controls,javafx.fxml -Dfile.encoding=UTF-8 -cp "out\production\untitled;lib\*;." parfumery.ClientGUI
pause