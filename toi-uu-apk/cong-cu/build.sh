#!/bin/bash
set -e
S=${S:-$(cd "$(dirname "$0")" && pwd)/work}  # thu muc lam viec: smali_nd/, tools/, fast/src/...
cd $S
AA=tools/b/lib/android-all-15-robolectric-14663667.jar
q() { grep -v "JAVA_TOOL" || true; }
rm -rf smali_new && cp -r smali_nd smali_new && python3 -I scripts/patch.py smali_new | tail -3
java -cp "tools/lib/*" org.jf.smali.Main a smali_new -o build/pre.dex --api 34 2>&1 | q
java -cp "tools/b/lib/*" com.googlecode.dex2jar.tools.Dex2jarCmd -f -o build/pre.jar build/pre.dex 2>&1 | q >/dev/null
rm -rf fast/cls && mkdir -p fast/cls
javac --release 8 -nowarn -cp "build/pre.jar:$AA" -d fast/cls fast/src/vn/personal/vietkey/*.java 2>&1 | grep -v "JAVA_TOOL\|warning\|^Note" || true
java -cp "tools/b/lib/dalvik-dx-16.0.1.jar" com.android.dx.command.Main --dex --min-sdk-version=26 --output=build/fast.dex fast/cls 2>&1 | q
java -cp "tools/lib/*" org.jf.baksmali.Main d --di false build/fast.dex -o smali_new 2>&1 | q
java -cp "tools/lib/*" org.jf.smali.Main a smali_new -o build/classes.dex --api 34 2>&1 | q
rm -rf smali_bench2 && cp -r smali_new smali_bench2 && python3 -I scripts/benchfix.py smali_bench2 >/dev/null
java -cp "tools/lib/*" org.jf.smali.Main a smali_bench2 -o bench/new.dex --api 34 2>&1 | q
java -cp "tools/b/lib/*" com.googlecode.dex2jar.tools.Dex2jarCmd -f -o bench/app_new.jar bench/new.dex 2>&1 | q >/dev/null
ls -la build/classes.dex
