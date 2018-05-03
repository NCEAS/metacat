# Metacat & MetacatUI Development Environment using Vagrant

This folder and the corresponding `Vagrantfile` at the top level of this repository provides a [Vagrant](https://www.vagrantup.com/) configuration for quickly getting Metacat and MetacatUI up and running.

## Getting Started

### Pre-setup

1. Install [Vagrant](https://www.vagrantup.com/)
2. Check out a copy of https://github.com/NCEAS/metacatui and place it in the same folder as your copy of the Metacat source code. The following commands won't work if this isn't done.

### Running

1. From the root of this repository (i.e. one folder up from this file), run:
    ```sh
    vagrant up
    ```
2. Visit http://localhost:8080/metacat and log in with the credentials:

    - Username: `admin@localhost`
    - Password: `password`
3. Finish configuring Metacat
    - Configure _Metacat Global Properties_
        - Click _Configure Now_ next to Metacat Global Properties
        - Note: You don't need to modify anything on this page and you can just click _Save_ on this page.
    - Run _Database Installation/Upgrade_
        - Click _Configure Now_ under Database Installation/Upgrade
        - Click _Continue_
4. You're all done

Notes: The password for the PostgreSQL database is `password`
