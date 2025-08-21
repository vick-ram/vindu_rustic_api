# Extract environment variables from IntelliJ configuration
ENV_VARS=$(grep -A1 "ENV_VARIABLES" .idea/workspace.xml | \
           grep -oP '(?<=<option name="ENV_VARIABLES" value=")[^"]*')

# Convert to export statements
echo $ENV_VARS | tr ';' '\n' | sed 's/^/export /' > runenv.sh

# Run with these variables
source runenv.sh && ./gradlew run