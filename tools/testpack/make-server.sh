#!/usr/bin/env bash
# Makes a dedicated Natural Exceptions server: Forge 1.7.10 on Java 21 (lwjgl3ify's launcher, as the test pack's
# client runs), with the server's side of the test pack (every required mod but the client-only Angelica) pulled from
# the pack server, which also keeps it up to date. See the README, "Playing together".
#
#   tools/testpack/make-server.sh DIR [PACK_URL]
#
# PACK_URL defaults to this machine's pack (http://<hostname>.local:8642/pack.toml). Then run DIR/start.sh.
set -euo pipefail

dir=${1:?usage: make-server.sh DIR [PACK_URL]}
pack=${2:-http://$(uname -n).local:8642/pack.toml}
FORGE=10.13.4.1614
LWJGL3IFY=3.0.33
JAVA=${JAVA:-$(ls -d /usr/lib/jvm/java-21-openjdk 2>/dev/null || true)/bin/java}
[[ -x $JAVA ]] || JAVA=java
CACHE=$(cd "$(dirname "$0")/../.." && pwd)/build/testpack-cache

mkdir -p "$dir" "$CACHE"
cd "$dir"

# Forge's server, installed with Java 21 is fine (the installer only unpacks).
if [[ ! -f forge-1.7.10-$FORGE-1.7.10-universal.jar ]]; then
    curl -fsSL -o forge-installer.jar \
        "https://maven.minecraftforge.net/net/minecraftforge/forge/1.7.10-$FORGE-1.7.10/forge-1.7.10-$FORGE-1.7.10-installer.jar"
    "$JAVA" -jar forge-installer.jar --installServer >/dev/null
    rm -f forge-installer.jar forge-installer.jar.log
fi

# lwjgl3ify's launcher for Java 21, and the JVM arguments it needs, from its Prism package.
mmc=$CACHE/lwjgl3ify-$LWJGL3IFY-multimc.zip
[[ -s $mmc ]] || curl -fsSL -o "$mmc" \
    "https://github.com/GTNewHorizons/lwjgl3ify/releases/download/$LWJGL3IFY/lwjgl3ify-$LWJGL3IFY-multimc.zip"
unzip -o -q -j "$mmc" "libraries/lwjgl3ify-$LWJGL3IFY-forgePatches.jar" -d .
unzip -p "$mmc" patches/me.eigenraven.lwjgl3ify.forgepatches.json |
    python3 -c 'import json,sys; print("\n".join(json.load(sys.stdin)["+jvmArgs"]))' > java9args.txt

[[ -s $CACHE/packwiz-installer-bootstrap.jar ]] || curl -fsSL -o "$CACHE/packwiz-installer-bootstrap.jar" \
    https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar
cp "$CACHE/packwiz-installer-bootstrap.jar" .

# Accepting Mojang's EULA (https://aka.ms/MinecraftEULA) is the server owner's to do: set eula=true in eula.txt.
[[ -f eula.txt ]] || echo "eula=false" > eula.txt
[[ -f server.properties ]] || printf '%s\n' "motd=Natural Exceptions" "view-distance=8" > server.properties

cat > start.sh <<EOF
#!/usr/bin/env bash
# Pulls the newest Natural Exceptions and its mods from the pack, then runs the server on Java 21.
cd "\$(dirname "\$0")"
"$JAVA" -jar packwiz-installer-bootstrap.jar -g -s server "$pack" || echo "Could not reach the pack; starting with the mods already here."
exec "$JAVA" -Xms2G -Xmx4G @java9args.txt -jar lwjgl3ify-$LWJGL3IFY-forgePatches.jar nogui
EOF
chmod +x start.sh
echo "Server made in $dir. Run $dir/start.sh (it pulls $pack each start)."
grep -q "eula=true" eula.txt || echo "First accept Mojang's EULA (https://aka.ms/MinecraftEULA): set eula=true in $dir/eula.txt."
