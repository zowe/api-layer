Contributing
============

Contributions are welcome, and they are greatly appreciated! Every little bit helps, and credit will always be given.

You can contribute in many ways:

Types of Contributions
----------------------

### Work on User Stories

As a member of core development team you will introduce new functionality from 
your teams' backlog.

### Report Bugs

Report bugs at <https://github.com/zowe/api-layer>.

If you are reporting a bug, please include:

-   Your operating system name and version.
-   Any details about your local setup that might be helpful in troubleshooting.
-   Detailed steps to reproduce the bug.

### Fix Bugs

Look through the GitHub issues for bugs. Anything tagged with "bug" is open to whoever wants to implement it.

### Write Documentation

The code could always use more documentation, whether as part of the API docs, in documentation strings, 
or even in the wiki, in blog posts, articles, and such.


Get Started!
============

Ready to contribute? Here's how to set up the project for local development.

We are following [GitHub Flow workflow](https://guides.github.com/introduction/flow/) with the `master` branch.

Members of core development team (<https://github.com/zowe/api-layer>) are
allowed to create branches in the repository.

1. Clone the repository:

    - _Core development team_:
    
        1. Clone the repository locally (creates `api-layer` directory):

                $ git clone https://github.com/zowe/api-layer

    - _Other contributors_:
        
        1. Fork the repository on GitHub - <https://github.com/zowe/api-layer>.
    
        2. Clone your fork locally (creates `api-layer` directory):

                $ git clone https://github.com/zowe/api-layer

2.  Create a branch for local development:

        $ git checkout master
      
        $ git checkout -b name-of-your-bugfix-or-feature

    Now you can make your changes locally.
    
3.  Review information in [README](README.md).    

4.  When you're done making changes, check that your changes pass all the tests on your computer and on z/OS.

5.  Commit your changes and push your branch to GitHub:

        $ git add .
        $ git commit -m "Your detailed description of your changes."
        $ git push origin name-of-your-bugfix-or-feature

7.  Submit a pull request through the GitHub website.


Pull Request Guidelines
=======================

Before you submit a pull request, check that it meets these guidelines:

1.  Review guidelines and advices [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)
    and [Good Commits](http://vry.cz/post/good-commits/). 
    The information that you provide helps reviewer to understand the code and review your pull request faster. 
    It is helpful for understanding the code in future.
2.  The pull request should include tests and code coverage for new code should be at least 60%. 
    Code coverage should not be lower than on master.
3.  If the pull request adds functionality, the docs should be updated.
4.  Execute all the available automated tests on your machine and on z/OS platform.
5.  If the pull request adds or changes functionality that requires update of packaging or configuration, it needs to be tested on a test system installed from the Zowe PAX file and scripts in [zowe-install](/zowe-install) directory need to be updated.  


Core Development Team
=====================

Members of core development team (<https://github.com/zowe/api-layer>) are
allowed to create branches in the master repository.

1.  Clone the repository locally (creates `api-layer` directory):

        $ git clone https://github.com/zowe/api-layer

2.  Other instructions are same above.
