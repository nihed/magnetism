JARS=""
for I in lib/*.jar; do JARS=$I:$JARS; done

PROFILE=ajax

echo "Adding to classpath: $JARS"
echo "Building profile: $PROFILE"

CLASSPATH="$JARS"$CLASSPATH ant -Dnosrc=true -Ddocless=true -Dprofile=$PROFILE -Drelease_dir=../../../../target/javascript/dojo  release 
