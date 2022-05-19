#!/bin/bash

current_path=$(pwd)
case "$(uname)" in
    Linux)
        bin_abs_path=$(readlink -f "$(dirname "$0")")
        ;;
    *)
        bin_abs_path=$(cd "$(dirname "$0")" || exit; pwd)
        ;;
esac
#################### 脚本参数设置 - START ####################
export base="${bin_abs_path}"
export LANG=en_US.UTF-8
export CONF_DIR=/opt/leave-agent/conf
export PROJECT_JAR_NAME=wec-counselor-leave-agent-1.0.0.jar
#################### 脚本参数设置 -   END ####################

if [[ -f ${base}/agent.pid ]]; then
    echo "found agent.pid , Please run stop.sh first ,then startup.sh" 2>&2
    exit 1
fi

## set java path
if [[ -z "$JAVA" ]]; then
    JAVA=$(which java)
    if [[ -z "$JAVA" ]]; then
        echo "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.8) in your PATH." 2>&2
        exit 1
    fi
fi

str=$(file -L "$JAVA" | grep 64-bit)
if [[ -n "$str" ]]; then
    JAVA_OPTS="-server -Xms2048m -Xmx3072m"
else
    JAVA_OPTS="-server -Xms1024m -Xmx1024m"
fi

JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=250 -XX:+UseGCOverheadLimit -XX:+ExplicitGCInvokesConcurrent -XX:+PrintAdaptiveSizePolicy -XX:+PrintTenuringDistribution"
JAVA_OPTS=" $JAVA_OPTS -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
AGENT_OPTS="-DappName=leave-agent"
if [[ -f "$CONF_DIR/application.properties" ]]; then
  AGENT_OPTS="$AGENT_OPTS -Dspring.config.location=$CONF_DIR/"
fi

echo "cd to $bin_abs_path for workaround relative path"
cd "${bin_abs_path}" || exit

nohup $JAVA ${JAVA_OPTS} ${AGENT_OPTS} -jar ${PROJECT_JAR_NAME} &
echo "nohup $JAVA ${JAVA_OPTS} ${JAVA_DEBUG_OPT} ${AGENT_OPTS} -jar ${PROJECT_JAR_NAME} 1>>/dev/null 2>&1 &"
echo $! > "${base}/agent.pid"

echo "cd to $current_path for continue"
cd "${current_path}" || exit