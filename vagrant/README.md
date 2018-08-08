# Metacat Development Environment using Vagrant

This folder and the corresponding `Vagrantfile` at the top level of this repository provides a [Vagrant](https://www.vagrantup.com/) configuration for quickly getting Metacat up and running.

## Getting Started

### Pre-requisites

1. Install [Vagrant](https://www.vagrantup.com/)
2. Install a VM provider such as [VirtualBox](https://www.virtualbox.org)

### Running

1. From the root of this repository (i.e. one folder up from this file), run:

    ```sh
    vagrant up
    ```

    and wait for Vagrant to set up your VM.

2. Visit http://localhost:8080/metacat in a web browser and log in with the credentials:

    - Username: `admin@localhost`
    - Password: `password`

3. Finish configuring Metacat

    - Configure _Metacat Global Properties_
        - Click _Configure Now_ next to Metacat Global Properties and click Save without modifying anything
    - Run _Database Installation/Upgrade_
        - Click _Configure Now_ under Database Installation/Upgrade and click Continue

4. You're all done

Note: The password for the PostgreSQL database is `password` just in case you need it.

### Running ant tasks (like tests) with this setup

Testing Metacat requires both a copy of the source code and a fully-installed Metacat instance.
As you make changes to the metacat code base on your host, you'll need to run `vagrant rsync` in order for commands inside the VM like `ant test` to see your updated code.

```sh
vagrant ssh
cd /metacat
sudo ant test
```

Following this pattern, if you edited a source file or a test, you'd then need to run:

On the host:

```sh
# From the top of the repo on the host
vagrant rsync
```

On the guest:

```sh
# From /metacat on the guest
ant clean test # or ant clean runonetest -Dtesttorun=TESTNAME
```
