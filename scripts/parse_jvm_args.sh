#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2021
################################################################################

# Parse custom JVM parameters from environment variables with ZWE_configs_jvm_ prefix
# This allows users to pass any JVM parameter without modifying start.sh
#
# Usage: Source this file to populate CUSTOM_JVM_OPTS variable
#   . ./parse_jvm_args.sh


CUSTOM_JVM_OPTS=""
for var in $(env | grep "^ZWE_configs_jvm_" | cut -d= -f1); do
    value=$(eval echo \"\$$var\")
    # Remove the ZWE_configs_jvm_ prefix to get the JVM option name
    opt_name=$(echo "$var" | sed 's/^ZWE_configs_jvm_//')
    
    if [ -n "$opt_name" ]; then
        # Handle different JVM option types
        case "$opt_name" in
            D*)
                # System property: convert underscores to dots in property name
                prop_name=$(echo "$opt_name" | sed 's/^D//' | sed 's/_/./g')
                if [ -n "$value" ]; then
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -D${prop_name}=${value}"
                else
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -D${prop_name}"
                fi
                ;;
            XX_*)
                # XX options: handle boolean (+/-) and value options
                xx_opt=$(echo "$opt_name" | sed 's/^XX_//')
                if [ -z "$value" ] || [ "$value" = "true" ]; then
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -XX:+${xx_opt}"
                elif [ "$value" = "false" ]; then
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -XX:-${xx_opt}"
                else
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -XX:${xx_opt}=${value}"
                fi
                ;;
            *)
                # Other options (like Xss, Xmn, etc.): pass as -option or -optionvalue
                if [ -n "$value" ]; then
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -${opt_name}${value}"
                else
                    CUSTOM_JVM_OPTS="$CUSTOM_JVM_OPTS -${opt_name}"
                fi
                ;;
        esac
    fi
done

if [ -n "$CUSTOM_JVM_OPTS" ]; then
    echo "Custom JVM options: ${CUSTOM_JVM_OPTS}"
fi
