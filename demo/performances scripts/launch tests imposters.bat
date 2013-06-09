@echo off
set x=%1
for /l %%z in (1, 1, 8) do (
	start "test2" /min jcwde -p 8766 D:\Dropbox\SemesterProject\workspace\SmartCardDevice\bin\monapplet.app
	java -jar javacardreader.jar res\3\1\user15\15_1.ist res\3\1\user%x%\%x%_%%z%.ist finalResults/impostersFVC3DB1StolenKey 8766 0
	ping 192.0.2.2 -n 1 -w 1000 > nul
	taskkill /f /fi "WINDOWTITLE eq test2"
)