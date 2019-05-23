#!/usr/bin/env python3
"""Changes the label in Zowe pull request"""

import logging
import os
import sys
from urllib.parse import quote

import requests

log = logging.getLogger(__name__)

ZOWE_GITHUB_AUTH_HEADERS = {
    'Authorization': "token {}".format(os.environ["ZOWE_GITHUB_APIKEY"])}
ZOWE_GITHUB_API_URL = 'https://api.github.com/repos/zowe/api-layer/'

# Labels:
UNDER_CA_TESTING = 'under CA testing'
CA_TESTS_PASSED = 'passed CA tests'
CA_TESTS_FAILED = 'failed CA tests'
READY_TO_RERUN = 'ready for CA testing'
CA_LABELS = [CA_TESTS_PASSED, CA_TESTS_FAILED,
             UNDER_CA_TESTING, READY_TO_RERUN]


def delete_label(pr_number, label):
    """Deletes label from the pull request on github"""
    requests.delete('{}issues/{}/labels/{}'.format(ZOWE_GITHUB_API_URL, pr_number, quote(label),
                                                   headers=ZOWE_GITHUB_AUTH_HEADERS))


def check_labels(all_labels):
    """Check the labels for automation generated ones and return boolean"""
    return bool(any(i in all_labels for i in CA_LABELS))


def main():
    """GitHub label checker main method"""
    branch = sys.argv[1]
    change_class = sys.argv[3]

    if change_class == 'doc':
        log.info("Documentation change")

    elif branch.startswith('PR-'):
        pr_number = branch.lstrip('PR-')

        pr = requests.get('{}pulls/{}'.format(ZOWE_GITHUB_API_URL, pr_number),
                          headers=ZOWE_GITHUB_AUTH_HEADERS).json()

        # Get all labels of this Pull Request
        labels = {label['name'] for label in pr['labels']}

        # If there were some labels,
        # give Zowe branch the "ready for CA testing" label so it is marked to run again
        if check_labels(labels):
            requests.post('{}issues/{}/labels'.format(ZOWE_GITHUB_API_URL, pr_number),
                          json=[{"name": READY_TO_RERUN}],
                          headers=ZOWE_GITHUB_AUTH_HEADERS)

        if UNDER_CA_TESTING in labels:
            delete_label(pr_number, UNDER_CA_TESTING)
        if CA_TESTS_FAILED in labels:
            delete_label(pr_number, CA_TESTS_FAILED)
        if CA_TESTS_PASSED in labels:
            delete_label(pr_number, CA_TESTS_PASSED)

    elif branch == 'master':
        log.info("No posting of PR labels for master")

    else:
        log.info("Branch %s is not a pull request", branch)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
