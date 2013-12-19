Downloading and Installing Metacat
==================================

Instructions for both Linux and Windows systems are included in this section.

.. contents::

System Requirements
-------------------
In addition to meeting the recommended system requirements, the server on which
you wish to install Metacat must have the following software installed and
running correctly:

* PostgreSQL_ (or another SQL92-compliant RDBMS like Oracle_) 
* `Apache Ant`_ (if building from source)
* `Apache Tomcat`_ 
* `Apache HTTPD Server`_ (recommended)

  * In order to use the Metacat Registry (and for a more robust Web-serving environment in general), the Apache Web server should be installed with Tomcat and the two should be integrated. See the installing Apache for more information.

* `Java 6`_ (Note: Java 5 is deprecated)

.. _PostgreSQL: http://www.postgresql.org/

.. _Oracle: http://www.oracle.com/

.. _Apache Ant: http://ant.apache.org/

.. _Apache Tomcat: http://tomcat.apache.org/

.. _Apache HTTPD Server: http://httpd.apache.org/

.. _Java 6: http://www.oracle.com/technetwork/java/javaee/overview/index.html

System requirements for running Metacat:

* a server running an SQL92-compliant database (PostgreSQL_ recommended) 
* at least 512MB RAM
* 200 MB disk space (Note: The amount of disk space required depends on the size of your RDBMS tablespace and the the size and number of documents stored. Metacat itself requires only about 140 MB of free space after installation).


Installing on Linux
-------------------
This section contains instructions for downloading and installing Metacat on 
Linux systems. As Mac OS X is based on BSD Unix, these Linux instructions can
be adapted to also work on Mac OS X (although the exact commands for
downloading and installing packages will differ due to the different package
management approaches on the Mac).

Quick Start Overview
~~~~~~~~~~~~~~~~~~~~
For the impatient or those who have already installed Metacat and know what
they are doing, here are the steps needed to install Metacat. Detailed
instructions for each step are in the next section.

1. Download and install prerequisites (`Java 6`_, `Apache Tomcat`_ 6, PostgreSQL_, `Apache HTTPD Server`_)
2. Create a database in PostgreSQL named 'metacat' and authorize access to it in ``pb_hba.conf`` for the user 'metacat'
3. Log in to PostgreSQL and create the 'metacat' user
4. Download Metacat from the `Metacat Download Page`_ and extract the archive
5. ``sudo mkdir /var/metacat; sudo chown -R <tomcat_user> /var/metacat``
6. ``sudo cp <metacat_package_dir>/metacat.war <tomcat_app_dir>``
7. ``sudo /etc/init.d/tomcat6 restart``
8. Configure Metacat through the Web interface

.. _Metacat Download Page: http://knb.ecoinformatics.org/software/metacat/

Downloading Metacat
~~~~~~~~~~~~~~~~~~~
Before installing Metacat, please ensure that all required software is
installed and running correctly. To obtain a Metacat WAR file, which is needed
for installation, download one of the following: 

* the Metacat installer, which has a pre-built WAR file,
* the Metacat source distribution, which must be built in order to create a WAR file, 
* the Metacat source code from SVN. You must build the source code in order to create a WAR file. 

Instructions for all three options are discussed below. Note that downloading
the installer (described in the next section) is the simplest way to get
started. 

Download the Metacat Installer
..............................
Downloading the Metacat Installer is the simplest way to get started with the
application. To download the installer: 

1.  Browse to the `Metacat Download Page`_. In the Metacat section, select the link to the "GZIP file" (the link should look like: metacat-bin-X.X.X.tar.gz, where X.X.X is the latest version of Metacat e.g., 2.3.1) 
2.  Save the file locally. 
3.  Extract the Metacat package files by typing:

::

  tar -xvzf metacat-bin-X.X.X.tar.gz

You should see a WAR file and several sample supporting files (Table 2.1). The
extraction location will be referred to as the ``<metacat_package_dir>`` for the
remainder of this documentation.

================== ===========================================================
File               Description
================== ===========================================================
metacat.war        The Metacat Web archive file (WAR) 
metacat-site       Sample Web definition file used by Apache on Ubuntu/Debian 
                   Linux systems. 
metacat-site-ssl   Sample SSL definition file used by Apache on Ubuntu/Debian 
                   Linux systems.
jk.conf            Sample JkMount configuration file used by Apache on 
                   Ubuntu/Debian Linux systems. 
workers.properties Sample workers definition file used by Apache on Ubuntu/Debian 
                   Linux systems. 
authority.war      The optional LSID Server application WAR
================== ===========================================================


Download Metacat Source Code
............................
To get the Metacat source distribution:

1. Browse to the `Metacat Download Page`_. In the Metacat section, select the link to the Metacat Source code (it will look something like this: metacat-src-X.X.X.tar.gz, where X.X.X is the latest version of Metacat, e.g., 2.3.1).
2. Save the file locally. 
3. Extract the Metacat package files by typing (replace X.X.X with the current version number): 

::

  tar -xvzf metacat-src-X.X.X.tar.gz

4. Rename the metacat-X.X.X directory to metacat. 

Note that you do not need to create the WAR file directly because the Ant
build-file has an "install" target that will build and deploy the WAR for you. 


Check Out Metacat Source Code from SVN (for Developers)
.......................................................

.. sidebar:: Installing an SVN Client:

    If you have not already installed Subversion and you are running Ubuntu/Debian,
    you can get the SVN client by typing:
    
    ::

        sudo apt-get install subversion

    Otherwise, you can get the SVN client from The Subversion homepage
    (http://subversion.tigris.org/).
    
If you wish to work with the most recent Metacat code, or you'd like to extend
the Metacat code yourself, you may wish to check out the Metacat source code
from SVN. You will need a Subversion (SVN) client installed and configured on
your system (see the end of this section for information about obtaining an SVN
client). 

To check out the code from SVN, go to the directory where you would like the
code to live and type::

  svn co https://code.ecoinformatics.org/code/metacat/tags/METACAT_<rev> metacat

Where ``<rev>`` is the version of the code you want to check out (like 2_0_0). 

To check out the head, type::

  svn co https://code.ecoinformatics.org/code/metacat/trunk metacat

You should see a list of files as they check out.

Note that you do not need to create the WAR file directly because the Ant
build-file has an "install" target that will build and deploy the WAR for you. 


Installing and Configuring Required Software
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Before you can install and run Metacat, you must ensure that a recent Java SDK,
PostgreSQL (or another SQL92-compliant RDBMS like Oracle), Ant (if
installing from source), and Tomcat are installed and running correctly. We
also highly recommend that you install Apache Web server, as it provides a more
robust Web-serving environment and is required by some Metacat functionality. 

* `Java 6`_
* `Apache Tomcat`_ 
* `Apache HTTPD Server`_ (Highly Recommended)
* PostgreSQL_ Database (or Oracle_)
* `Apache Ant`_ (if building from Source)

Java 6
......
To run Metacat, you should use Java 6 (Java 5 is deprecated and will not be
supported after Metacat version 1.9.2). Make sure that the JAVA_HOME
environment variable is properly set and that both ``java`` and ``javac`` 
are on your PATH. 

To install Java if you are running Ubuntu_/Debian, you can download the appropriate self-extracting installer:: 

  wget http://download.oracle.com/otn-pub/java/jdk/6u30-b12/jdk-6u30-linux-x64.bin
  
and follow these commands to install::
  
  sudo mkdir -p /opt/java/64
  sudo mv jdk-6u30-linux-x64.bin /opt/java/64
  cd /opt/java/64
  sudo chmod +x jdk-6u30-linux-x64.bin
  sudo ./jdk-6u30-linux-x64.bin
  sudo update-alternatives --install "/usr/bin/java" "java" "/opt/java/64/jdk1.6.0_30/bin/java" 1

You must accept the license agreement during the install process.

If you are not using Ubuntu_/Debian, you can get Java from the Oracle_ website and install using the RPM or other installer (Windows).

.. _Ubuntu: http://www.ubuntu.com/

Apache Tomcat
.............
We recommend that you install Tomcat 6 into the directory of your choice.
Included with the Metacat download is a Tomcat-friendly start-up script that
should be installed as well.

Note: we will refer to the Tomcat installation directory as ``<tomcat_home>`` for
the remainder of the documentation. 

If you are running Ubuntu_/Debian, get Tomcat by typing::

  sudo apt-get install tomcat6

Otherwise, get Tomcat from the `Apache Tomcat`_ page.

After installing Tomcat, you can switch back to the Sun JDK by typing::

  sudo update-alternatives --config java
  
and selecting the correct Java installation.

If using Tomcat with Apache/mod_jk, enable the AJP connector on port 8009 by uncommenting that section in::

  <tomcat_home>/conf/server.xml
  
For DataONE deployments edit::  

	/etc/tomcat6/catalina.properties
	
to include::

	org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
	org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true
	
Apache HTTPD Server (Highly Recommended)
........................................
Although you have the option of running Metacat with only the Tomcat server, we
highly recommend that you run it behind the Apache Web server for several
reasons; running Tomcat with the Apache server provides a more robust Web
serving environment. The Apache Web server is required if you wish to
install and run the Metacat Registry or to use the Metacat Replication feature. 

.. sidebar:: Configuring Apache on an OS other than Ubuntu/Debian 

  If you are running on an O/S other than Ubuntu/Debian (e.g., Fedora Core or
  RedHat Linux) or if you installed the Apache source or binary, you must
  manually edit the Apache configuration file, where <apache_install_dir> is the
  directory in which Apache is installed:

  ::

    <apache_install_dir>/conf/httpd.conf

  1. Configure the log location and level for Mod JK. If your configuration file does not already have the following section, add it and set the log location to any place you'd like:

    ::

      <IfModule mod_jk.c> 
        JkLogFile "/var/log/tomcat/mod_jk.log" 
        JkLogLevel info 
      </IfModule> 

  2. Configure apache to route traffic to the Metacat application. ServerName should be set to the DNS name of the Metacat server. ScriptAlias and the following Directory section should both point to the cgi-bin directory inside your Metacat installation:

    ::

      <VirtualHost XXX.XXX.XXX.XXX:80> 
        DocumentRoot /var/www 
        ServerName dev.nceas.ucsb.edu 
        ErrorLog /var/log/httpd/error_log 
        CustomLog /var/log/httpd/access_log common 
        ScriptAlias /cgi-bin/ "/var/www/cgi-bin/" 
        <Directory /var/www/cgi-bin/> 
          AllowOverride None 
          Options ExecCGI 
          Order allow,deny 
          Allow from all 
        </Directory> 
        ScriptAlias /metacat/cgi-bin/ "/var/www/webapps/metacat/cgi-bin/" 
        <Directory "/var/www/webapps/metacat/cgi-bin/"> 
          AllowOverride None 
          Options ExecCGI 
          Order allow,deny 
          Allow from all 
        </Directory> 
        JkMount /metacat ajp13 
        JkMount /metacat/* ajp13 
        JkMount /metacat/metacat ajp13 
        JkUnMount /metacat/cgi-bin/* ajp13 
        JkMount /*.jsp ajp13 
      </VirtualHost> 

  3. Copy the "workers.properties" file provided by Metacat into your Apache configuration directory (<apache_install_dir>/conf/).  Depending on whether you are installing from binary distribution or source, the workers.properties file will be in one of two locations:

    * the directory in which you extracted the Metacat distribution (for binary distribution)
    * <metacat_code_dir>/src/scripts/workers.properties (for both the source distribution and source code checked out from SVN)

  4. Edit the workers.properties file and make sure the following properties are set correctly:

    ::

      workers.tomcat_home -  set to the Tomcat install directory. 
      workers.java_home - set to the Java install directory. 

  5. Restart Apache to bring in changes by typing:

    ::

      sudo /etc/init.d/apache2 restart

This section contains instructions for installing and configuring the Apache
Web server for Metacat on an Ubuntu_/Debian system. Instructions for configuring
Apache running on other Linux systems are included in the sidebar.

1. Install the Apache and Mod JK packages (Mod JK is the module Apache uses to talk to Tomcat applications) by typing:

::

  sudo apt-get install apache2 libapache2-mod-jk

If you are installing the Apache server on an Ubuntu/Debian system, and you
installed Apache using apt-get as described above, the Metacat code will have
helper files that can be dropped into directories to configure Apache.
Depending on whether you are installing from binary distribution or source,
these helper files will be in one of two locations: 

* the directory in which you extracted the distribution (for binary distribution)
* ``<metacat_code_dir>/src/scripts`` (for both the source distribution and source code checked out from SVN).  We will refer to the directory with the helper scripts as ``<metacat_helper_dir>`` and the directory where Apache is installed (e.g., ``/etc/apache2/``) as ``<apache_install_dir>``.

2. Set up Mod JK apache configuration by typing:

::

  sudo cp <metacat_helper_dir>/debian/jk.conf <apache_install_dir>/mods-available
  sudo cp <metacat_helper_dir>/debian/workers.properties <apache_install_dir>

3. Disable and re-enable the Apache Mod JK module to pick up the new changes:

::

  sudo a2dismod jk
  sudo a2enmod jk

4. Apache needs to know about the Metacat site. The helper file named "metacat-site" has rules that tell Apache which traffic to route to Metacat. Set up Metacat site by dropping the metacat-site file into the sites-available directory and running a2ensite to enable the site:

::

  sudo cp <metacat_helper_dir>/metacat-site <apache_install_dir>/sites-available
  sudo a2ensite metacat-site
  
5. Disable the default Apache site configuration:

::

  sudo a2dissite 000-default  

6. Restart Apache to bring in changes by typing:

::

  sudo /etc/init.d/apache2 restart


PostgreSQL Database
...................
Metacat has been most widely tested with PostgreSQL_ and we recommend using it.
Instructions for installing and configuring Oracle are also included in the
next section.  To install and configure PostgreSQL_:

1. If you are running Ubuntu_/Debian, get PostgreSQL by typing:

  ::

    sudo apt-get install postgresql

  On other systems, install the rpms for postgres.

2. Start the database by running:

  ::

    sudo /etc/init.d/postgresql-8.4 start

3. Change to postgres user: 

  ::

    sudo su - postgres


4. Set up an empty Metacat database instance by editing the postgreSQL configuration file: 

  ::

    gedit /etc/postgresql/8.4/main/pg_hba.conf


  Add the following line to the configuration file: 

  ::

    host metacat metacat 127.0.0.1 255.255.255.255 password


  Save the file and then create the Metacat instance: 

  ::

    createdb metacat


5. Log in to postgreSQL by typing: 

  ::

    psql metacat


6. At the psql prompt, create the Metacat user by typing:

  ::

    CREATE USER metacat WITH UNENCRYPTED PASSWORD 'your_password';

  where 'your_password' is whatever password you would like for the Metacat user. 

7. Exit PostgreSQL by typing 

  ::

    \q

8. Restart the PostgreSQL database to bring in changes: 

  ::

    /etc/init.d/postgresql-8.4 restart

9. Log out of the postgres user account by typing: 

  ::

    logout

10. Test the installation and Metacat account by typing: 

  ::

    psql -U metacat -W -h localhost metacat

11. Log out of postgreSQL: 

  ::

    \q


The Metacat servlet automatically creates the required database schema. For
more information about configuring the database, please see Database
Configuration.

Installing and Configuring Oracle
.................................
To use Oracle with Metacat, the Oracle RDBMS must be installed and running
as a daemon on the system. In addition the JDBC listener must be enabled.
Enable it by logging in as an Oracle user and typing::

  lsnrctl start

Your instance should have a table space of at least 5 MB (10 MB or higher
recommended). You must also create and enable a username specific to Metacat.
The Metacat user must have most normal permissions including: CREATE SESSION,
CREATE TABLE, CREATE INDEX, CREATE TRIGGER, EXECUTE PROCEDURE, EXECUTE TYPE,
etc. If an action is unexplainably rejected by Metacat, the user permissions
are (most likely) not correctly set.

The Metacat servlet automatically creates the required database schema. For
more information, please see Database Configuration.

Apache Ant (if building from Source)
....................................
If you are building Metacat from a source distribution or from source code
checked out from SVN, Ant is required. (Users installing Metacat from the
binary distribution do not require it.) Ant is a Java-based build application
similar to Make on UNIX systems. It takes build instructions from a file named
"build.xml", which is found in the root installation directory. Metacat source
code comes with a default "build.xml" file that may require some modification
upon installation. 

If you are running Ubuntu/Debian, get Ant by typing::

  sudo apt-get install ant

Otherwise, get Ant from the `Apache Ant`_ homepage.

Ant should be installed on your system and the "ant" executable shell script
should be available in the user's path. The latest Metacat release was tested
with Ant 1.8.2. 

Installing Metacat
~~~~~~~~~~~~~~~~~~
Instructions for a new install, an upgrade, and a source install are included
below.

New Install
...........
Before installing Metacat, please ensure that all required applications are
installed, configured to run with Metacat, and running correctly. If you are
upgrading an existing Metacat servlet, please skip to Upgrade. For information
about installing from source, skip to Source Install and Upgrade.

To install a new Metacat servlet:

1. Create the Metacat directory. Metacat uses a base directory to store data, metadata, temporary files, and configuration backups. This directory should be outside of the Tomcat application directory so that it will not get wiped out during an upgrade. Typically, the directory is '/var/metacat', as shown in the instructions. If you choose a different location, remember it. You will be asked to configure Metacat to point to the base directory at startup.  Create the Metacat directory by typing:

  ::

    sudo mkdir /var/metacat

2. Change the ownership of the directory to the user that will start Tomcat by typing (note: If you are starting Tomcat as the root user, you do not need to run the chown command):

  ::

    sudo chown -R <tomcat_user> /var/metacat


3.  Install the Metacat WAR in the Tomcat web-application directory. For instructions on downloading the Metacat WAR, please see Downloading Metacat.  Typically, Tomcat will look for its application files (WAR files) in the <tomcat_home>/webapps directory (e.g., /usr/share/tomcat6/webapps). Your instance of Tomcat may be configured to look in a different directory. We will refer to the Tomcat application directory as <tomcat_app_dir>.  NOTE: The name of the WAR file (e.g., metacat.war) provides the application context, which appears in the URL of the Metacat (e.g., http://yourserver.com/metacat/). To change the context, simply change the name of the WAR file to the desired name before copying it.  To install the Metacat WAR:

  ::

    sudo cp <metacat_package_dir>/metacat.war <tomcat_app_dir>


4. Restart Tomcat. Log in as the user that runs your Tomcat server (often "tomcat") and type:  

  ::

    sudo /etc/init.d/tomcat6 restart

Congratulations! You have now installed Metacat. If everything is installed
correctly, you should see the Authentication Configuration screen (Figure 2.1)
when you type http://yourserver.com/yourcontext/ (e.g.,
http://knb.ecoinformatics.org/knb) into a browser. For more information about
configuring Metacat, please see the Configuration Section.

.. figure:: images/screenshots/image009.png
   :align: center

   The Authentication Configuration screen appears the first time you open a 
   new installation of Metacat. 

Upgrade Metacat
...............
To upgrade an existing binary Metacat installation follow the steps in this
section. The steps for upgrading Metacat from source are the same as the
instructions for installing from source:

1. Download and extract the new version of Metacat. For more information about downloading and extracting Metacat, please see Downloading Metacat.

2. Stop running Metacat. To stop Metacat, log in as the user that runs your Tomcat server (often "tomcat") and type:

  ::

    /etc/init.d/tomcat6 stop

3. Back up the existing Metacat installation. Although not required, we highly recommend that you back up your existing Metacat to a backup directory (<backup_dir>) before installing a new one. You can do so by typing:

  ::

    cp <web_app_dir>/metacat <backup_dir>/metacat.<yyyymmdd>
    cp <web_app_dir>/metacat.war <backup_dir>/metacat.war.<yyyymmdd>

  Warning: Do not backup the files to the ``<web_app_dir>`` directory.  Tomcat will
  try to run the backup copy as a service.

4. Copy the new Metacat WAR file in to the Tomcat applications directory: 

  ::

    sudo cp <metacat_package_dir>/metacat.war <tomcat_app_dir>

  Note: Typically, Tomcat will look for its application files (WAR files) in the
  ``<tomcat_home>/webapps`` directory. Your instance of Tomcat may be configured to
  look in a different directory. 

5. If you have been (or would like to start) running an LSID server, copy the new authority.war file to the Tomcat applications directory. For more information about the LSID server, please see Optional Installation Options. 

  ::
   
    sudo cp <metacat_package_dir>/authority.war <tomcat_app_dir>

6. Restart Tomcat (and Apache if you have Tomcat integrated with it). Log in as the user that runs your Tomcat server (often "tomcat"), and type:  

  ::

    /etc/init.d/tomcat6 restart


7. Run your new Metacat servlet. Go to a Web browser and visit your installed
Metacat application, using a URL of the form: 

  ::

    http://yourserver.yourdomain.com/yourcontext/

You should substitute your context name for "yourcontext" in the URL above
(your context will be "metacat" unless you change the name of the metacat.war file to
something else). If everything is working correctly, you should be presented
with Metacat's Authorization Configuration screen. Note that if you do not have
Tomcat integrated with Apache you will probably have to type
http://yourserver.yourdomain.com:8080/yourcontext/

Source Install and Upgrade
..........................
Whether you are building Metacat from the source distribution or source code
checked out from SVN, you will need Apache Ant to do the build (see Installing
and Configuring Required Software for more information about Ant). 

To install Metacat from source:

1. Edit the build.properties file found in the directory in which you
   downloaded Metacat. Note: Throughout the instructions, we will refer to this
   directory as ``<metacat_src_dir>``. 

  * Set the build.tomcat.dir property to your Tomcat installation directory.
    Metacat will use some of the native Tomcat libraries during the build. For
    instance: build.tomcat.dir=/usr/local/tomcat
  * Set the app.deploy.dir property to your application deployment directory.
    For instance: app.deploy.dir=/usr/local/tomcat/webapps

2. In the ``<metacat_src_dir>``, run: 

  ::

    sudo ant clean install

  You will see the individual modules get built. You should see a "BUILD
  SUCCESSFUL" message at the end.

  You should see a new file named metacat.war in your application deployment
  directory.

To run your new Metacat servlet, open a Web browser and type::

  http://yourserver.yourdomain.com/yourcontext/ 
  (e.g.  http://knb.ecoinformatics.org/metacat/)

Your context will be "metacat" unless you changed the name of the metacat.war file to
something else. The servlet may require a few seconds to start up, but once it
is running, you will be presented with the Authorization Configuration screen.

Optional Installation Options (LSID Server)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. Note::

  The support for LSID identifiers is deprecated, and is being replaced with
  support for DOI_ identifiers in a future release. We are maintaining support
  for LSIDs on one particular site, but this support will be removed in a
  future version of Metacat.

.. _DOI: http://www.doi.org/

Metacat's optional LSID server allows Metacat to use a standardized syntax for
identifying data sets, in addition to Metacat's internal, custom scheme for
identifiers. LSID's were designed to identify complex biological entities with
short identifiers (much like DOIs in publishing) that are both computer and
human readable. LSID identifiers are URIs and are therefore usable in many
Internet applications, but they also cleanly separate the identity of a data
set (i.e., its permenant identifier) from its current location (e.g., the list
of URLs from which it might be retrieved).  LSIDs accomplish this by using a
level of indirection; the identifier represents simply a name without location,
but an associated resolver service can be used to locate the current location
of the data and medata for the data set.  This is accomplished by establishing
a well-known location for the resolution service for each authority using an
infrequently used feature of the domain name system called SRV records.  At its
most basic, resolution of an identifier is performed when a client looks up the
SRV record for an LSID by querying DNS, which returns the current host and port
of the authority web service, which is in turn used to locate the data and
metadata.

Using LSIDs to identify data records is being debated among members of the
Taxonomic Databases Working Group (TDWG).  There are several alternate
technologies that are under consideration (e.g., DOI_, plain http URIs), and so
at this time the support for LSIDs in Metacat has been created on an
experimental basis only.  If the LSID approach is ratified by the broader
community, we will expand support for LSIDs in Metacat, but until then it is an
optional and experimental feature.

The format of an LSID is:: 

  urn:lsid:<Authority>:<Namespace>:<ObjectID>[:<Version>]
  e.g., urn:lsid:ecoinformatics.org:tao:12039:1

When you enable the Metacat LSID support, you can use LSID clients (such as
LSID Launchpad) and LSID notation to query Metacat for data and metadata. LSID
notation can be used directly in Metacat HTTP queries as well. For example, a
data package with an ID tao.12039.1 that is stored in a Metacat available at:
http://example.com:9999 can be accessed by the following HTTP Metacat queries::

  http://example.com:9999/authority/data?lsid=urn:lsid:ecoinformatics.org:tao:12039:1
  (To return the data)

  http://example.com:9999/authority/metadata?lsid=urn:lsid:ecoinformatics.org:tao:12039:1
  (To return the metadata)

Notice that in the HTTP query strings, the periods in the data package ID have
been replaced with colons. The authority (ecoinformatics.org) must be properly
configured by the Metacat administrator. Note: In order to configure the
authority, you must have access to the DNS server for the Metacat domain.
Further instructions are provided below.

Install and configure the LSID Server shipped with Metacat
..........................................................

To install the LSID server using the binary installation:

1. Copy the authority.war file to Tomcat:

  ::

    sudo cp <metacat_package_directory>/authority.war /usr/share/tomcat6/webapps
 
2. Set up the LSID server by dropping the authority file into Apache's
   sites-available directory and running a2ensite to enable the site:

   ::

     sudo cp <metacat_helper_dir>/authority /etc/apache2/sites-available
     sudo a2ensite authority

3. Restart Tomcat. Log in as the user that runs your Tomcat server (often
   "tomcat") and type:

   ::

     /etc/init.d/tomcat5.5 restart

4. Restart Apache to bring in changes by typing:

  ::

    sudo /etc/init.d/apache2 restart

5. See notes beneath LSID server source installation for instructions for
   modifying the SRV record(s)

To install the LSID server from a source
........................................

1. In the build.properties file found in the directory into which you
   extracted the Metacat source code, set the authority and config.lsidauthority
   properties. For example:
  
  ::
   
   authority.context=authority
   config.lsidauthority=ecoinformatics.org

2. In the <metacat-src-dirctory> create the authority.war by running:

  ::

    sudo ant war-lsid

3. Copy the LSID WAR file into the Tomcat application directory.

  ::

    sudo cp <metacat_package_dir>/dist/authority.war <tomcat_app_dir>

4. Restart Tomcat. Log in as the user that runs your Tomcat server (often
   "tomcat") and type:   

  ::

    /etc/init.d/tomcat6 restart

5. If you are running Tomcat behind the Apache server (the recommended
   configuration), set up and enable the authority service site configurations by
   typing:

  ::

    sudo cp <metacat_helper_dir>/authority <apache_install_dir>/sites-available
    sudo a2ensite authority

  Where <metacat_helper_dir> can be found in <metacat_code_dir>/src/scripts

6.  Restart Apache to bring in changes by typing: 

  ::

    sudo /etc/init.d/apache2 restart

  Once the authority.war is installed, you must also modify the SRV record(s)
  on the DNS server for the domain hosting the Metacat. The record should be
  added to the master zone file for the metacat's DNS server:

    ::

      _lsid._tcp      IN      SRV     1       0       8080    <metacat.edu>.

  Where <metacat.edu> is the name of the machine that will serve as the
  physical location of the AuthorityService.

  For example, the value of <metacat.edu> for the below example URL would be
  example.com:
  
    ::
    
      http://example.com:9999/authority/data?lsid=urn:lsid:ecoinformatics.org:tao:12039:1

  For more information, please see http://www.ibm.com/developerworks/opensource/library/os-lsid/

Troubleshooting
~~~~~~~~~~~~~~~
We keep and update a list of common problems and their solutions on the KNB
website. See http://knb.ecoinformatics.org/software/metacat/troubleshooting.html 
for more information.

Installing on Windows
---------------------
Metacat can be installed on Windows. Please follow the instructions in this
section for downloading Metacat, installing the required software, and
installing Metacat. Note that Registry and Data Upload functionality has not
been tested on Windows.

Download Metacat
~~~~~~~~~~~~~~~~
To obtain a Metacat WAR file, which is used when installing the Metacat
servlet:

1. Browse to the KNB Software Download Page. In the Metacat section, select
   the link that looks like: metacat-bin-X.X.X.zip, where X.X.X is the latest
   version of Metacat (e.g., 2.0.4).

2. Choose to download and Save the file locally. 

3. Extract the Metacat package files using your Windows zip utility. You
   should see a WAR file and several supporting files (we will only use the WAR
   file when installing Metacat). 

Note: The location where these files were extracted will be referred to as the
``<metacat_package_dir>`` for the remainder of this documentation. 

Note: Before installing Metacat, please ensure that all required software is
installed and running correctly.


Install Required Software
~~~~~~~~~~~~~~~~~~~~~~~~~
Before you can install and run Metacat, you must ensure that a recent Java SDK,
PostgreSQL and Tomcat are installed, configured, and running correctly. 

* `Java 6`_
* `Apache Tomcat`_
* PostgreSQL_ Database

Java 6
......
To run Metacat, you must have Java 6. (Java 5 is deprecated). Make sure that
the JAVA_HOME environment variable is properly set and that both java and javac
are on your PATH.

To download and install Java:

1. Browse to: http://java.sun.com/javase/downloads/widget/jdk6.jsp and follow
   the instructions to download JDK 6.

2. Run the downloaded installer to install Java.

3. Set the JAVA_HOME environment variable: In "My Computer" properties, go to
   "advanced settings > environment variables". Add:

  ::

    System Variable: JAVA_HOME C:\Program Files\Java\jdk1.6.0_18 
    (or whichever version you downloaded)

Apache Tomcat
.............
We recommend that you install Tomcat version 6.  To download and install Tomcat:

1. Browse to: http://tomcat.apache.org/
2. Download the Tomcat core zip file 
3. Extract Tomcat files to C:\Program Files\tomcat using the windows zip
   utility. 

PostgreSQL Database
...................
Metacat can be run with several SQL92-compliant database systems, but it has 
been most widely tested with PostgreSQL_. Instructions for installing and 
configuring PostgreSQL for use with Metacat are included in this section.

To download and install PostgreSQL:

1. Browse to http://www.postgresql.org/download/windows and download the
   one-click installer 
2. Run the installer 
3. Edit C:\Program Files\PostgreSQL\8.3\data and add:
  
  ::

    host metacat metacat 127.0.0.1 255.255.255.255 password

4. Create a super user. At the command line, enter:

  ::

    C:\Program Files\PostgreSQL\8.3\bin 
    createdb -U postgres metacat (enter super user password)

5. Log in to PostgreSQL: 

  ::

    psql -U postgres metacat (enter super user password)

6. Create a Metacat user:

  ::

    CREATE USER metacat WITH UNENCRYPTED PASSWORD 'your_password'

7. Exit PostgreSQL: 

  ::

    \q

8. Restart PostgreSQL from the start menu by selecting:
  
  ::

    run start/All Programs/PostgreSQL 8.3/Stop Server
    run start/All Programs/PostgreSQL 8.3/Start Server


9. Test the installation by logging in as the metacat user: 

  ::
  
    psql -U metacat -W -h localhost metacat

10. Exit PostgreSQL:

  ::

    \q

The Metacat servlet automatically creates the required database schema. For
more information, please see Database Configuration.

Installing Metacat
~~~~~~~~~~~~~~~~~~
Instructions for a new install and for an upgrade are included below.

New Install
...........
Before installing Metacat, please ensure that all required applications are
installed, configured to run with Metacat, and running correctly. If you are
upgrading an existing Metacat servlet, please skip to Upgrade.

To install a new Metacat servlet:

1. Create the Metacat base directory at: 

  ::

    C:/Program Files/metacat

2. Copy the Metacat WAR file to Tomcat (for information about obtaining a
   Metacat WAR file, see Download Metacat): 
  
  ::

    copy <metacat_package_dir>\metacat.war C:\Program Files\tomcat\webapps

3.  Restart Tomcat: 

  ::

    C:\Program Files\tomcat\shutdown.bat
    C:\Program Files\tomcat\startup.bat


Congratulations! You are now ready to configure Metacat. Please see the
Configuration Section for more information. 

Upgrade
.......
To upgrade an existing Metacat installation:

1. Download and extract the new version of Metacat. For more information about
   downloading and extracting Metacat, please see Download Metacat.

2. Back up the existing Metacat installation. Although not required, we highly
   recommend that you back up your existing Metacat to a backup directory 
   (<backup_dir>) before installing a new version. You can do so by copying:

  ::

    <web_app_dir>/metacat to <backup_dir>/metacat.<yyyymmdd>
    <web_app_dir>/metacat.war to <backup_dir>/metacat.war.<yyyymmdd>

  Warning: Do not backup the metacat directory in the <web_app_dir> directory.
  Tomcat will try to run the backup copy as a service.

3.  Copy the new Metacat WAR file in to Tomcat applications directory: 

  ::

    copy metacat.war C:\Program Files\tomcat\webapps

4.  Restart Tomcat: 
  
  ::
  
    C:\Program Files\tomcat\shutdown.bat
    C:\Program Files\tomcat\startup.bat

Congratulations! You are now ready to configure Metacat. Please see Configuring
Metacat for more information.

