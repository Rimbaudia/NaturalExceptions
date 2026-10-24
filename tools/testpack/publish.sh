#!/usr/bin/env bash
# Builds Natural Exceptions and publishes it, with the mods it requires, as a test pack on the home network: a packwiz
# pack that the test machine's Prism Launcher instance pulls on every Play, and that instance itself, ready to import.
#
#   tools/testpack/publish.sh                   build, assemble the pack in build/testpack, make sure it is served
#   tools/testpack/publish.sh --no-build        publish the jar already built
#   tools/testpack/publish.sh --install-server  also install and start the server (a systemd user service), once
#   tools/testpack/publish.sh --site URL        assemble it for a website at URL instead (GitHub Pages, from CI),
#                                               with a download page; nothing is served from here
#                                               (NE_REPO_URL: the source repository, linked from the page)
#
# NE_PACK_HOST sets the name the test machine reaches this one by (default: <hostname>.local).
set -euo pipefail

cd "$(dirname "$0")/../.."
ROOT=$PWD
SRC=tools/testpack/pack
OUT=build/testpack
CACHE=build/testpack-cache
PORT=8642
HOST=${NE_PACK_HOST:-$(uname -n).local}
INSTANCE="Natural Exceptions (test)"
FORGE=10.13.4.1614
PACKWIZ=$(command -v packwiz || echo "$HOME/go/bin/packwiz")
BOOTSTRAP_URL=https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar
# The version the dev client's runClient21 uses; also in the pack (tools/testpack/pack/mods/lwjgl3ify.pw.toml).
LWJGL3IFY=3.0.33
LWJGL3IFY_MMC_URL=https://github.com/GTNewHorizons/lwjgl3ify/releases/download/$LWJGL3IFY/lwjgl3ify-$LWJGL3IFY-multimc.zip

build=1
install_server=0
site=
while (( $# )); do
    case $1 in
        --no-build) build=0 ;;
        --install-server) install_server=1 ;;
        --site) site=${2:?--site needs the URL of the site}; shift ;;
        *) echo "unknown option: $1" >&2; exit 2 ;;
    esac
    shift
done
# Where the pack is fetched from: this machine on the home network, or the website.
BASE=${site:-http://$HOST:$PORT}
BASE=${BASE%/}
[[ -n $site ]] && INSTANCE="Natural Exceptions"

if [[ ! -x $PACKWIZ ]]; then
    echo "packwiz is not installed: go install github.com/packwiz/packwiz@latest" >&2
    exit 1
fi

if (( build )); then ./gradlew assemble -q; fi
jar=$(ls -t build/libs/naturalexceptions-*.jar | grep -v -e '-dev\.jar$' -e '-sources\.jar$' | sed -n 1p)
echo "Natural Exceptions: $jar"

# The pack: its source (pack.toml and a metafile per required mod), the new jar, and packwiz's index of them.
rm -rf "$OUT"
mkdir -p "$OUT/mods" "$CACHE"
cp "$SRC/pack.toml" "$OUT/"
cp "$SRC"/mods/*.pw.toml "$OUT/mods/"
cp "$jar" "$OUT/mods/"
printf '%s\n' '*.zip' > "$OUT/.packwizignore"
: > "$OUT/index.toml"
(cd "$OUT" && "$PACKWIZ" refresh -y >/dev/null)

# The Prism instance: Minecraft 1.7.10 with Forge on Java 21, as the dev client runs it (runClient21), from
# lwjgl3ify's own Prism package (its components, patches and early classpath); and packwiz-installer pulling the
# pack before each launch.
[[ -s $CACHE/packwiz-installer-bootstrap.jar ]] || curl -fsSL -o "$CACHE/packwiz-installer-bootstrap.jar" "$BOOTSTRAP_URL"
mmc=$CACHE/lwjgl3ify-$LWJGL3IFY-multimc.zip
[[ -s $mmc ]] || curl -fsSL -o "$mmc" "$LWJGL3IFY_MMC_URL"
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
mkdir -p "$stage/NaturalExceptions-Test/.minecraft"
unzip -q "$mmc" -d "$stage/NaturalExceptions-Test"
if ! grep -q "\"version\": \"$FORGE\"" "$stage/NaturalExceptions-Test/mmc-pack.json"; then
    echo "lwjgl3ify $LWJGL3IFY's Prism package is not for Forge $FORGE" >&2
    exit 1
fi
cp "$CACHE/packwiz-installer-bootstrap.jar" "$stage/NaturalExceptions-Test/.minecraft/"
cat > "$stage/NaturalExceptions-Test/instance.cfg" <<EOF
InstanceType=OneSix
name=$INSTANCE
iconKey=default
OverrideMemory=true
MinMemAlloc=512
MaxMemAlloc=3072
OverrideCommands=true
PreLaunchCommand="\$INST_JAVA" -jar packwiz-installer-bootstrap.jar $BASE/pack.toml
EOF
(cd "$stage" && zip -qr "$ROOT/$OUT/NaturalExceptions-Test.zip" NaturalExceptions-Test)

if [[ -n $site ]]; then
    # The download page, filled in for this build.
    version=${jar#build/libs/naturalexceptions-}
    version=${version%.jar}
    sed -e "s|@BASE@|$BASE|g" -e "s|@VERSION@|$version|g" -e "s|@JAR@|${jar##*/}|g" -e "s|@REPO@|${NE_REPO_URL:-#}|g" \
        tools/testpack/site/index.html > "$OUT/index.html"
    : > "$OUT/.nojekyll"
    echo "Assembled for $BASE in $OUT."
    exit 0
fi

# The server: a systemd user service serving build/testpack.
unit=$HOME/.config/systemd/user/ne-testpack.service
if (( install_server )); then
    mkdir -p "$(dirname "$unit")"
    sed "s|@ROOT@|$ROOT|g; s|@PORT@|$PORT|g" tools/testpack/ne-testpack.service > "$unit"
    systemctl --user daemon-reload
    systemctl --user enable --now ne-testpack.service
fi
if systemctl --user is-active --quiet ne-testpack.service; then
    ip=$(ip route get 1.1.1.1 2>/dev/null | grep -oE 'src [0-9.]+' | cut -d' ' -f2)
    echo "Published. The Mac pulls it on its next Play."
    echo "  Pack:      http://$HOST:$PORT/pack.toml   (or http://$ip:$PORT/pack.toml)"
    echo "  Instance:  http://$HOST:$PORT/NaturalExceptions-Test.zip   (Prism: Add Instance > Import)"
else
    echo "The pack is built, but its server is not running: tools/testpack/publish.sh --install-server" >&2
fi
