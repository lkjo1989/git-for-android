#!/bin/bash
# 为当前 shell 切换到 JDK 17（仅影响当前终端会话）
# 用法: source set-jdk17.sh

export JAVA_HOME=/root/git-for-android/.jdk17
export PATH=$JAVA_HOME/bin:$PATH
echo "✅ 已切换到 JDK 17: $(java -version 2>&1 | head -1)"
