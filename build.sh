#!/bin/bash

oldJAVAHome=$JAVA_HOME
oldPATH=$PATH

task=$1

version=""

checkJava()
{
    if [ -d /Library/Java/JavaVirtualMachines/openjdk-11.jdk/Contents/Home ]; then
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-11.jdk/Contents/Home
    fi

    if [ -z "$JAVA_HOME" ] || ! { [ -x "$JAVA_HOME/bin/java" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "11\.'; }; then
        for jdkHome in \
            "/c/Program Files/Eclipse Adoptium/jdk-11"* \
            "/c/Program Files/Java/jdk-11"* \
            "/c/Program Files/Microsoft/jdk-11"* \
            "/c/Program Files/Amazon Corretto/jdk11"* \
            "/Library/Java/JavaVirtualMachines/"*"/Contents/Home"
        do
            if [ -d "$jdkHome/bin" ] && "$jdkHome/bin/java" -version 2>&1 | grep -q 'version "11\.'; then
                export JAVA_HOME="$jdkHome"
                break
            fi
        done
    fi

    if [ -n "$JAVA_HOME" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
    fi

    if ! command -v java >/dev/null 2>&1; then
        echo "Java not found. Please install JDK 11 and set JAVA_HOME."
        exit 1
    fi

    javaVersionOutput=$(java -version 2>&1)
    javaMajorVersion=$(echo "$javaVersionOutput" | awk -F '"' '/version/ {print $2; exit}' | awk -F. '{ if ($1 == "1") print $2; else print $1 }')

    if [[ -z "$javaMajorVersion" || "$javaMajorVersion" -lt "11" ]]; then
        echo "Java version must not be lower than 11.0"
        echo "$javaVersionOutput"
        exit 1
    fi

    if [[ "$javaMajorVersion" -gt "13" ]]; then
        echo "Gradle 6.1.1 is not compatible with Java $javaMajorVersion."
        echo "Please use JDK 11 for this project, then rerun: ./build.sh $task"
        echo "$javaVersionOutput"
        exit 1
    fi

    echo "Using Java $javaMajorVersion from: $(command -v java)"
}

getVersion()
{
    version=$(grep -Eo "^version = .*" $1 | grep -Eo "['\"].*['\"]" | tr -d "'\"")
}

runWebPackageManager()
{
    if command -v yarn >/dev/null 2>&1; then
        yarn "$@"
    elif command -v npm >/dev/null 2>&1; then
        if [[ $# -eq 0 ]]; then
            npm install --legacy-peer-deps
        else
            npm run "$@"
        fi
    else
        echo "Neither yarn nor npm was found. Please install Node.js with npm, or install yarn."
        exit 1
    fi
}

getVersion ./build.gradle.kts

case $task in
    build)
        checkJava
        # 调试打包
        ./gradlew buildReader
    ;;
    run)
        checkJava
        # 运行 javafx UI
        port=$2
        if [[ -z "$port" ]]; then
            port=8080
        fi
        ./gradlew assemble --info
        if test $? -eq 0; then
            shift
            shift
            java -jar build/libs/reader-$version.jar --reader.app.showUI=true --reader.server.port=$port $@
        fi
    ;;
    win)
        checkJava
        # 打包 windows 安装包
        JAVAFX_PLATFORM=win ./gradlew packageReaderWin
    ;;
    linux)
        checkJava
        # 打包 linux 安装包
        JAVAFX_PLATFORM=linux ./gradlew packageReaderLinux
    ;;
    mac)
        checkJava
        # 打包 mac 安装包
        JAVAFX_PLATFORM=mac ./gradlew packageReaderMac
    ;;
    serve)
        checkJava
        # 服务端一键运行
        port=$2
        if [[ -z "$port" ]]; then
            port=8080
        fi
        mv src/main/java/com/htmake/reader/ReaderUIApplication.kt src/main/java/com/htmake/reader/ReaderUIApplication.kt.back
        getVersion ./cli.gradle
        ./gradlew -b cli.gradle assemble --info
        if test $? -eq 0; then
            shift
            shift
            mv src/main/java/com/htmake/reader/ReaderUIApplication.kt.back src/main/java/com/htmake/reader/ReaderUIApplication.kt
            java -jar build/libs/reader-$version.jar --reader.server.port=$port $@
        else
            mv src/main/java/com/htmake/reader/ReaderUIApplication.kt.back src/main/java/com/htmake/reader/ReaderUIApplication.kt
        fi
    ;;
    cli)
        checkJava
        # 服务端打包命令
        shift
        mv src/main/java/com/htmake/reader/ReaderUIApplication.kt src/main/java/com/htmake/reader/ReaderUIApplication.kt.back
        getVersion ./cli.gradle
        ./gradlew -b cli.gradle $@
        mv src/main/java/com/htmake/reader/ReaderUIApplication.kt.back src/main/java/com/htmake/reader/ReaderUIApplication.kt
    ;;
    yarn)
        # 前端包管理器快捷命令，默认 install
        shift
        cd web
        runWebPackageManager "$@"
    ;;
    web)
        # 开发web页面
        cd web
        runWebPackageManager serve
    ;;
    sync)
        # 编译同步web资源
        cd web
        if [ ! -d node_modules ]; then
            runWebPackageManager
        fi
        runWebPackageManager build
        if test $? -eq 0; then
            cd ..
            rm -rf src/main/resources/web
            mv web/dist src/main/resources/web
        fi
    ;;
    *)
        echo "
USAGE: ./build.sh build|run|win|linux|mac|serve|cli|yarn|web|sync

build   调试打包
run     桌面端编译运行，需要先执行 sync 命令编译同步web资源
win     打包 windows 安装包
linux   打包 linux 安装包
mac     打包 mac 安装包
serve   服务端编译运行
cli     服务端打包命令
yarn    web页面 yarn 快捷命令，默认 install
web     开发web页面
sync    编译同步web资源
"
    ;;
esac

if [ -n "$oldJAVAHome" ]; then
    export JAVA_HOME="$oldJAVAHome"
else
    unset JAVA_HOME
fi
export PATH="$oldPATH"
