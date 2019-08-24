@Echo off
set MAVEN_OPTS="-XX:+UseG1GC"
cmd /C mvn package
pause