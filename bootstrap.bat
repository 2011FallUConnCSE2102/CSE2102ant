@echo off

REM You will need to specify JAVA_HOME if compiling with 1.2 or later.

set OLDJAVA=%JAVA%
set OLDJAVAC=%JAVAC%
set OLDCLASSPATH=%CLASSPATH%
set OLDANTHOME=%ANT_HOME%

set ANT_HOME=.

if "" == "%JAVA%"  if "" == "%JAVA_HOME%" set JAVA=java
if "" == "%JAVA%"                         set JAVA=%JAVA_HOME%\bin\java

if "" == "%JAVAC%" if "" == "%JAVA_HOME%" set JAVAC=javac
if "" == "%JAVAC%"                        set JAVAC=%JAVA_HOME%\bin\javac

echo.
echo ... Bootstrapping Ant Distribution

SET LOCALCLASSPATH=classes;src\main
if exist lib\ant.jar erase lib\ant.jar
for %%i in (lib\*.jar) do call src\bin\lcp.bat %%i
if exist %JAVA_HOME%\lib\tools.jar call src\bin\lcp.bat %JAVA_HOME%\lib\tools.jar
if exist %JAVA_HOME%\lib\classes.zip call src\bin\lcp.bat %JAVA_HOME%\lib\classes.zip
SET CLASSPATH=%CLASSPATH%;%LOCALCLASSPATH%

echo JAVA_HOME=%JAVA_HOME%
echo JAVA=%JAVA%
echo JAVAC=%JAVAC%
echo CLASSPATH=%CLASSPATH%

if     "%OS%" == "Windows_NT" if exist classes\nul rmdir/s/q classes
if not "%OS%" == "Windows_NT" if exist classes\nul deltree/y classes
mkdir classes

set TOOLS=src\main\org\apache\tools

echo.
echo ... Compiling Ant Classes

%JAVAC% -d classes %TOOLS%\tar\*.java %TOOLS%\ant\*.java %TOOLS%\ant\taskdefs\*.java

echo.
echo ... Copying Required Files

copy %TOOLS%\ant\taskdefs\*.properties classes\org\apache\tools\ant\taskdefs

echo.
echo ... Building Ant Distribution

%JAVA% org.apache.tools.ant.Main clean main bootstrap %1 %2 %3 %4 %5

echo.
echo ... Cleaning Up Build Directories

if     "%OS%" == "Windows_NT" if exist classes\nul rmdir/s/q classes
if not "%OS%" == "Windows_NT" if exist classes\nul deltree/y classes

echo.
echo ... Done Bootstrapping Ant Distribution

set JAVA=%OLDJAVA%
set JAVAC=%OLDJAVAC%
set CLASSPATH=%OLDCLASSPATH%
set ANT_HOME=%OLDANTHOME%
set OLDJAVA=
set OLDJAVAC=
set OLDCLASSPATH=
set LOCALCLASSPATH=
set OLDANTHOME=
set TOOLS=

