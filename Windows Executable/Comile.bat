mkdir ..\Class
mkdir ..\images
copy ..\Source\*.css ..\Class\

copy ..\Source\*.jar ..\Class\
javac -classpath "..\Source\sql.jar"; -d ..\Class\ ..\Source\*.java
