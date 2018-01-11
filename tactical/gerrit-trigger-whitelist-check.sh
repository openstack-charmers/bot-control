#!/bin/bash -ue
# Fail/bail if the event account is not part of the whitelist.
# Env var values are set by all gerrit-related jenkins jobs in UOSCI.
# Expect both the account name and account mail address to match.

for check in "$GERRIT_EVENT_ACCOUNT_NAME" "$GERRIT_EVENT_ACCOUNT_EMAIL"; do
    grep "$check" "$GERRIT_ACCOUNT_WHITELIST" &> /dev/null &&\
        echo "$check is whitelisted to trigger events." ||\
        (echo "$check is not in the white list to trigger events." && exit 1)
done
