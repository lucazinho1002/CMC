@echo off
javac -encoding UTF-8 *.java
echo Main-Class: index > manifest.txt
jar cvfm CMC.jar manifest.txt *.class
jpackage --type app-image --input . --dest output --name CMC --main-jar CMC.jar --main-class index
pause