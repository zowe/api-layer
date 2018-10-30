"""
Repository changes classifier

Prints ``full`` if a full build is required. Prints something else (e.g. ``doc``) if specific type of change
is recognized.

It considers changes since the last commit.
On Jenkins it checks since last successful commit, otherwise from master.

On Jenkins, it is necessary to use this additional ref spec in the GitHub organization specification:

    +refs/heads/master:refs/remotes/origin/master

Supported change types:
    - ``empty`` - no changes at all
    - ``doc`` - .md files only
    - ``full`` - anything other change
    - ``api-catalog`` - changes only in api-catalog-*
"""

import subprocess
import os
from pprint import pprint


def bytes_to_str(b):
    try:
        return b.decode()
    except AttributeError:
        return b


def parse_modified(output):
    modified_files = set(bytes_to_str(filename) for filename in output)
    modified_dirs = set()
    modified_exts = set()
    modified_top_dirs = set()
    for path in modified_files:
        ext = os.path.splitext(path)[1].lower()
        modified_exts.add(ext)
        dirname = bytes_to_str(os.path.dirname(path))
        while dirname:
            modified_dirs.add(dirname)
            if "/" not in dirname:
                modified_top_dirs.add(dirname)
            dirname = os.path.split(dirname)[0]

    print("Modified top-level directories: {}".format(', '.join(modified_top_dirs)))
    print("Modified directories: {}".format(', '.join(modified_dirs)))
    print("Modified files: {}".format(', '.join(modified_files)))
    print("Modified extensions: {}".format(', '.join(modified_exts)))
    return modified_files, modified_dirs, modified_top_dirs, modified_exts


def class_of_changes():
    previous = os.getenv("GIT_PREVIOUS_SUCCESSFUL_COMMIT")
    if not previous:
        previous = "origin/master"
    args = ["git",  "diff", "--name-only", previous]
    print("Git arguments: {}".format(' '.join(args)))
    completed = subprocess.run(args, stdout=subprocess.PIPE)
    if completed.returncode == 0:
        modified_files, modified_dirs, modified_top_dirs, modified_exts = parse_modified(completed.stdout.splitlines(keepends=False))
        if not modified_files:
            return "empty"
        elif modified_exts == {".md"}:
            print("Only documentation files (Markdown) have changed")
            return "doc"
        elif modified_top_dirs in [{'api-catalog-ui'}, {'api-catalog-services'}, {'api-catalog-ui', 'api-catalog-services'}]:
            return 'api-catalog'
    else:
        print("The was a problem when getting the differences - a full build will be triggered")
    return "full"


if __name__ == "__main__":
    print(class_of_changes())

