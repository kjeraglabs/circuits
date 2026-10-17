#!/usr/bin/env bash
# Build the simulator and put a ready-to-upload site in ./dist
# Needs: Java 17+ (and curl + unzip the first time, to fetch GWT)
set -euo pipefail
GWT_VERSION=2.12.2
GWT_DIR=".gwt/gwt-$GWT_VERSION"

if [ ! -d "$GWT_DIR" ]; then
  mkdir -p .gwt
  curl -sSL -o .gwt/gwt.zip "https://github.com/gwtproject/gwt/releases/download/$GWT_VERSION/gwt-$GWT_VERSION.zip"
  unzip -q .gwt/gwt.zip -d .gwt
fi

java -Xmx3g -cp "src:$GWT_DIR/gwt-user.jar:$GWT_DIR/gwt-dev.jar" \
  com.google.gwt.dev.Compiler -war war -style OBFUSCATED -sourceLevel auto \
  com.lushprojects.circuitjs1.circuitjs1

rm -rf dist
mkdir dist
# everything in war except WEB-INF goes on the web server
(cd war && tar --exclude=WEB-INF -cf - .) | (cd dist && tar -xf -)
# serve the simulator at the site root
cp dist/circuitjs.html dist/index.html
echo "Site ready in ./dist"
