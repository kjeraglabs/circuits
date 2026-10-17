#!/usr/bin/env bash
# Build the simulator and put a ready-to-upload site in ./dist
# Needs: Java 17+ (and curl + unzip the first time, to fetch GWT)
# STAGING=1 ./build.sh marks the site as a test build (banner, title, not indexed).
set -euo pipefail
# Windows (Git Bash) uses ; between classpath entries
CP_SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) CP_SEP=";" ;; esac
GWT_VERSION=2.12.2
GWT_DIR=".gwt/gwt-$GWT_VERSION"

if [ ! -d "$GWT_DIR" ]; then
  mkdir -p .gwt
  curl -sSL -o .gwt/gwt.zip "https://github.com/gwtproject/gwt/releases/download/$GWT_VERSION/gwt-$GWT_VERSION.zip"
  unzip -q .gwt/gwt.zip -d .gwt
fi

java -Xmx3g -cp "src$CP_SEP$GWT_DIR/gwt-user.jar$CP_SEP$GWT_DIR/gwt-dev.jar" \
  com.google.gwt.dev.Compiler -war war -style OBFUSCATED -sourceLevel auto \
  com.lushprojects.circuitjs1.circuitjs1

rm -rf dist
mkdir dist
# everything in war except WEB-INF goes on the web server
(cd war && tar --exclude=WEB-INF -cf - .) | (cd dist && tar -xf -)

if [ "${STAGING:-}" = "1" ]; then
  sed -i -e 's|<title>Kjerag Circuits</title>|<title>Kjerag Circuits (dev)</title><meta name="robots" content="noindex">|' dist/circuitjs.html
  sed -i -e 's|<body>|<body><div style="background:#ffd000;color:#000;padding:4px 6px;margin:-6px -4px 8px;font-weight:bold">DEV BUILD: testing only. <a href="/" target="_top">Go to live site</a></div>|' dist/iframe.html
fi

# serve the simulator at the site root
cp dist/circuitjs.html dist/index.html
# GitHub Pages serves 404.html for unknown paths, so /led-demo opens the app,
# which then loads shared/led-demo.txt.  <base href="/"> keeps asset paths
# working at any path depth, and kjeragNotFound tells the app it was opened
# through an unknown path.
sed 's|<head>|<head><base href="/"><script>window.kjeragNotFound=true</script>|' dist/circuitjs.html > dist/404.html
echo "Site ready in ./dist"
