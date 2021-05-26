#!/bin/sh

# run:
# ./manage-etc-hosts.sh add 10.20.1.2 test.com
# ./manage-etc-hosts.sh remove 10.20.1.2 test.com

# PATH TO YOUR HOSTS FILE
ETC_HOSTS=/etc/hosts


add() {
    IP=$1
    HOSTNAME=$2
    HOSTS_LINE="$IP[[:space:]]$HOSTNAME"
    line_content=$( printf "%s\t%s\n" "$IP" "$HOSTNAME" )
    if [ -n "$(grep -P "$HOSTS_LINE" $ETC_HOSTS)" ]
        then
            echo "$line_content already exists : $(grep "$HOSTNAME" $ETC_HOSTS)"
        else
            echo "Adding $line_content to your $ETC_HOSTS";
            sh -c -e "echo '$line_content' >> /etc/hosts";

            if [ -n "$(grep -P "$HOSTNAME" $ETC_HOSTS)" ]
                then
                    echo "$line_content was added succesfully";
                else
                    echo "Failed to Add $line_content, Try again!";
            fi
    fi
}

$@
