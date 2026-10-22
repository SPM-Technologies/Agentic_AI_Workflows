# Session-only setup for THIS Git Bash window. Nothing is saved.
# Local JDK 21 = for java command. Maven runs inside Docker.
export JAVA_HOME="/d/Pravin/jdk-21.0.12.1"
export PATH="$JAVA_HOME/bin:$PATH"

# 'mvn' runs Maven in a Docker container (same as the PowerShell version).
mvn() {
  MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W):/app" -w /app \
    -v maven-repo:/root/.m2 maven:3-eclipse-temurin-21 mvn "$@"
}
export -f mvn

echo "Session: JAVA_HOME = $JAVA_HOME | mvn -> Docker"
