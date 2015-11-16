javac -d classes -classpath "lib/*" src/*.java
jar -cvf lib/JSONChecker.jar -C classes .
pause
