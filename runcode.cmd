@Echo off
set MAVEN_OPTS="-XX:+UseG1GC"
cmd /C mvn compile exec:java -Dexec.mainClass="com.ishland.app.HTTPServiceAttacker.App"
pause