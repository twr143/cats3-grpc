#!/bin/sh

export JAVA_OPTS="$JAVA_OPTS $JAVA_OPTS_EXT"

exec /root/bin/app -Dconfig.file=${config} -Dlogback.configurationFile=${logback}
