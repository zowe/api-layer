#!/usr/bin/env python3
"""Changes the label in Zowe pull request"""

import os
import sys
import requests
import logging
from urllib.parse import quote

log = logging.getLogger(__name__)

zowe_github_auth_headers = {'Authorization': "token {}".format(os.environ["ZOWE_GITHUB_APIKEY"])}
zowe_github_api_url = 'https://api.github.com/repos/zowe/api-layer/'

# Labels:
under_ca_testing = 'under CA testing'
ca_tests_passed = 'passed CA tests'
ca_tests_failed = 'failed CA tests'
ready_to_rerun = 'ready for CA testing'


def delete_label(pr_number, label):
    requests.delete('{}issues/{}/labels/{}'.format(zowe_github_api_url, pr_number, quote(label),
                                                   headers=zowe_github_auth_headers))


def main():
    branch = sys.argv[1]
    change_class = sys.argv[3]

    if change_class == 'doc':
        log.info("Documentation change")

    elif branch.startswith('PR-'):
        pr_number = branch.lstrip('PR-')

        pr = requests.get('{}pulls/{}'.format(zowe_github_api_url, pr_number),
                          headers=zowe_github_auth_headers).json()

        # Get all labels of this Pull Request
        labels = {label['name'] for label in pr['labels']}

        # If there were some labels,
        # give Zowe branch the "ready for CA testing" label so it is marked to run again
        if labels:
            requests.post('{}issues/{}/labels'.format(zowe_github_api_url, pr_number),
                          json=[{"name": ready_to_rerun}],
                          headers=zowe_github_auth_headers)

        if under_ca_testing in labels:
            delete_label(pr_number, under_ca_testing)

        if ca_tests_failed in labels:
            delete_label(pr_number, ca_tests_failed)

        if ca_tests_passed in labels:
            delete_label(pr_number, ca_tests_passed)

    elif branch == 'master':
        log.info("No posting of PR labels for master")

    else:
        log.info("Branch {} is not a pull request".format(branch))


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
