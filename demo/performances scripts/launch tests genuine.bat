@echo off
set x=%1
for /l %%z in (1, 1, 8) do (
	start "test" /min jcwde -p 8989 D:\Dropbox\SemesterProject\workspace\SmartCardDevice\bin\monapplet.app
	java -jar javacardreader.jar res\3\1\user%x%\%x%_1.ist res\3\1\user%x%\%x%_%%z%.ist draft/genuineFVC3DB1 8989 0
	ping 192.0.2.2 -n 1 -w 1000 > nul
	taskkill /f /fi "WINDOWTITLE eq test"
)