#!/bin/sh
# Gradle wrapper script for Unix-based systems

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

# Gradle wrapper jar location
GRADLE_WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

# Execute Gradle using the wrapper
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS -jar "$GRADLE_WRAPPER_JAR" "$@"