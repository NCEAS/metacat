Appendix: Metacat Properties
============================

.. include:: ./properties-overview.rst

.. contents:: The properties themselves are detailed below
    :local:

Server Properties
-----------------
All of Metacat's server properties are managed with the form-based configuration utility,
though they can also be accessed directly by editing the ``metacat-site.properties`` file.
More information on each is included below.

+---------------------------+------------------------------------------------------------------------------------------+------------------------+
| Property                  | Description                                                                              | Example                |
+===========================+==========================================================================================+========================+
| .. _server-name:          |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.name               | The network host name used to access Metacat. Note that this is not necessarily          | knb.ecoinformatics.org |
|                           | the physical name of the server running Metacat. The host name should not                |                        |
|                           | include the protocol prefix (http://).                                                   |                        |
|                           |                                                                                          |                        |
|                           | Default Value: localhost                                                                 |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+
| .. _server-port:          |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.port               | The network port used to access Metacat for connections.                                 | 443                    |
|                           | This can be either an https port, such as 443, or an http port, such as 80.              |                        |
|                           |                                                                                          |                        |
|                           | Default Value: 443                                                                       |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+
| .. _server-https:         |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.https              | To indicate the server port is an https one or an http one.                              | true                   |
|                           | True means an https port; false means an http port.                                      |                        |
|                           |                                                                                          |                        |
|                           | Default Value: true                                                                      |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+
| .. _server-internalName:  |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.internalName       | The internal network host name used to access Metacat. It is used to improve performance | localhost              |
|                           | since it bypasses the external network interface to directly access files, e.g. schema   |                        |
|                           | and style sheet files located within Metacat itself. The host name should not include    |                        |
|                           | the protocol prefix (http://).                                                           |                        |
|                           |                                                                                          |                        |
|                           | Default Value: localhost                                                                 |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+
| .. _server-internalPort:  |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.internalPort       | The network port used to access Metacat for the internal server name.                    | 80                     |
|                           | This is usually 80 if Apache Web server is running, and 8080 if Tomcat is running alone. |                        |
|                           |                                                                                          |                        |
|                           | Default Value: 80                                                                        |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+

.. _application-properties:

Application Properties
----------------------

Metacat's application properties are described below. Properties that can only 
be edited manually in the ``metacat-site.properties`` file are marked with an asterisk (\*). All 
others are managed with the properties configuration utility.

+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| Property                             | Description                                                                 | Example                       |
+======================================+=============================================================================+===============================+
| application.metacatVersion*          | The Metacat version number. It is set by the build engineer                 | 3.0.0                         |
|                                      | at build time. Usually, the value should never be changed.                  |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: X.X.X (where X.X.X is the current version of Metacat)        |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| application.metacatReleaseInfo*      | Release information for display purposes. Typically the property            | Release Candidate 1           |
|                                      | is set during the release candidate cycle to let users know which           |                               |
|                                      | candidate they are downloading.                                             |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.envSecretKeys:       |                                                                             |                               |
|                                      |                                                                             |                               |
| application.envSecretKeys*           | See :ref:`secret-properties`                                                |                               |
|                                      | A colon-delimited list of mappings between "secret" properties              |                               |
|                                      | (e.g. passwords) and environment variables used to pass them to Metacat.    |                               |
|                                      | Passing secrets to Metacat via environment variables avoids having them in  |                               |
|                                      | the properties file as plain text.                                          |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.deployDir:           |                                                                             |                               |
|                                      |                                                                             |                               |
| application.deployDir                | The directory where Web applications are deployed. Usually, the value       | /usr/local/tomcat/webapps     |
|                                      | is a directory named "webapps" in the Tomcat installation directory.        |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.context:             |                                                                             |                               |
|                                      |                                                                             |                               |
| application.context                  | The name of the Metacat application directory in                            | knb                           |
|                                      | the deployment directory. This corresponds to the first part of the         |                               |
|                                      | WAR file name (the part before .war). Most commonly, this                   |                               |
|                                      | is "metacat", but it can be changed to other things.                        |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _index.context:                   |                                                                             |                               |
|                                      |                                                                             |                               |
| index.context                        | The name of the Metacat index webapp in                                     | metacat-index                 |
|                                      | the deployment directory. Most commonly, this                               |                               |
|                                      | is "metacat-index", but it can be changed if needed.                        |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _ui.context:                      |                                                                             |                               |
|                                      |                                                                             |                               |
| ui.context                           | The name of the Metacat UI directory in                                     | metacatui                     |
|                                      | the deployment directory. Often the UI is deployed                          |                               |
|                                      | as the ROOT webapp, in which case the property should be blank ("").        |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.default-style:       |                                                                             |                               |
|                                      |                                                                             |                               |
| application.default-style            | A custom Metacat Web skin usually associated with                           | default                       |
|                                      | an organizational theme. If your organization has no                        |                               |
|                                      | custom skin, leave the value as "default".                                  |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.sitePropertiesDir:   |                                                                             |                               |
|                                      |                                                                             |                               |
| application.sitePropertiesDir        | The directory in which to store the ``metacat-site.properties`` file. The   | /var/metacat/config           |
|                                      | directory should be outside the Metacat installation directories so custom  |                               |
|                                      | settings will not be lost when Metacat is upgraded. The site properties     |                               |
|                                      | file directory must be writable by the user that starts Tomcat (and thus    |                               |
|                                      | Metacat).                                                                   |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/config                                          |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.datafilepath:        |                                                                             |                               |
|                                      |                                                                             |                               |
| application.datafilepath             | The directory in which to store data files. The directory should            | /var/metacat/data             |
|                                      | be outside the Metacat installation directories so data files will not      |                               |
|                                      | be lost when Metacat is upgraded. The data file directory must be           |                               |
|                                      | writable by the user that starts Tomcat (and thus Metacat).                 |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/data                                            |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.inlinedatafilepath:  |                                                                             |                               |
|                                      |                                                                             |                               |
| application.inlinedatafilepath       | The directory where inline data files will be stored. Inline                | /var/metacat/inline-data      |
|                                      | data files are created from data that is embedded in EML                    |                               |
|                                      | metadata. The directory should be outside the Metacat installation          |                               |
|                                      | directories so data files will not be lost when Metacat is upgraded.        |                               |
|                                      | For clarity of data, this should probably not be the same as                |                               |
|                                      | ``application.datafilepath``. The data file directory must be               |                               |
|                                      | writable by the user that starts Tomcat (and thus Metacat).                 |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/inline-data                                     |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.documentfilepath:    |                                                                             |                               |
|                                      |                                                                             |                               |
| application.documentfilepath         | The directory where metadata files will be stored.                          | /var/metacat/documents        |
|                                      | The directory should be outside the Metacat installation directories        |                               |
|                                      | so document files will not be lost when Metacat is upgraded. For            |                               |
|                                      | clarity of organization, this should probably not be the same as            |                               |
|                                      | ``application.datafilepath`` or ``application.inlinedatafilepath``.         |                               |
|                                      | The data file directory must be writable by the user that                   |                               |
|                                      | starts Tomcat (and thus Metacat).                                           |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/documents                                       |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _application.tempDir:             |                                                                             |                               |
|                                      |                                                                             |                               |
| application.tempDir                  | The directory where the Metacat data registry stores temporary              | /var/metacat/temporary        |
|                                      | files. The directory should not be the same as ``application.datafilepath`` |                               |
|                                      | or ``application.inlinedatafilepath`` (or any other persistent file path)   |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _cn.server.publiccert.filename:   |                                                                             |                               |
|                                      |                                                                             |                               |
| cn.server.publiccert.filename        | The location(s) of one or more certificate files containing public keys     |                               |
|                                      | that will be used to verify incoming request auth (JWT) tokens, in addition |                               |
|                                      | to verifying them against the configured CN server.                         |                               |
|                                      | Multiple paths should be delimited by semicolons (;)                        |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: (empty: only cn is used for token verification)              |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _dataone.nodeToken.file:          |                                                                             |                               |
|                                      |                                                                             |                               |
| dataone.nodeToken.file               | The path to a file that contains an authentication token. This token will be|                               |
|                                      | used by the dataone-indexer, to enable indexing of private datasets.        |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/certs/token                                     |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+

Solr Properties
----------------------

Metacat's Solr properties are described below. Properties that can only 
be edited manually in the ``metacat-site.properties`` file are marked with an asterisk (\*). All 
others are managed with the properties configuration utility.

+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| Property                             | Description                                                                 | Example                       |
+======================================+=============================================================================+===============================+
| .. _solr-baseURL:                    |                                                                             |                               |
|                                      |                                                                             |                               |
|                                      |                                                                             |                               |
| solr.baseURL                         | The URL of the Solr server which Metacat can access.                        | http://localhost:8983/solr    |
|                                      |                                                                             |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _solr-homeDir:                    |                                                                             |                               |
|                                      |                                                                             |                               |
| solr.homeDir                         | The Solr home directory (not to be confused with the Solr installation      | /var/metacat/solr-home2       |
|                                      | directory) is where Solr manages core directories with index files.         |                               |
|                                      | The directory must be writable by the user that starts the Solr service.    |                               |
|                                      |                                                                             |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _solr-coreName:                   |                                                                             |                               |
|                                      |                                                                             |                               |
| solr.coreName                        | The name of the Solr core which holds the index of the Metacat objects.     | metacat-index                 |
|                                      |                                                                             |                               |
|                                      |                                                                             |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _solr-env-script-path:            |                                                                             |                               |
|                                      |                                                                             |                               |
| solr.env.script.path                 | An environment specific include file overrides defaults used by the         |/etc/default/solr.in.sh        |
|                                      | bin/solr script. Metacat modifies this file to add the solr.home as the     |                               |
|                                      | default data directory. This file should be writable by the Tomcat user.    |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+

Database Properties
-------------------
Metacat's database properties are described next. Properties that can only be 
edited manually in the ``metacat-site.properties`` file are marked with an asterisk (\*). All others 
are managed with the properties configuration utility.

+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| Property                                   | Description                                                                   | Example                                                 |
+============================================+===============================================================================+=========================================================+
| .. _database-connectionURI:                |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.connectionURI                     | The JDBC connection URI for the main database instance of Metacat.            | ``jdbc:postgresql://yourserver.yourdomain.edu/metacat`` |
|                                            | The URI is formatted like this:                                               |                                                         |
|                                            | ``jdbc:<database_type>:thin@<your_server_name>:1521:<metacat_database_name>`` |                                                         |
|                                            | NOTE:                                                                         |                                                         |
|                                            | You must create an empty database prior to initial Metacat configuration.     |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: jdbc:postgresql://localhost/metacat                            |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-user:                         |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.user                              | The user for the main database instance of Metacat. The user must             | metacat-user                                            |
|                                            | have already been created on the database.                                    |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-password:                     |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.password                          | The password of the user for the main database instance of Metacat.           | securepassword4843                                      |
|                                            | The password must have already been created for the user.                     |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-type:                         |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.type                              | The type of database you are running. Currently, there are two supported      | postgres                                                |
|                                            | types, Oracle and Postgres.                                                   |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: postgres                                                       |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-driver:                       |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.driver                            | The JDBC driver to be used to access the main database instance of Metacat.   | org.postgresql.Driver                                   |
|                                            | There is one driver associated with each type of database.                    |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: org.postgresql.Driver                                          |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-adapter:                      |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.adapter                           | The adapter class that allows Metacat to access your database type.           | edu.ucsb.nceas.dbadapter.PostgresqlAdapter              |
|                                            | There is one adapter associated with each type of database.                   |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: edu.ucsb.nceas.dbadapter.PostgresqlAdapter                     |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-scriptsuf:                    |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.scriptsuffix.<database_type>      | The script suffix tells the system which database scripts to run              | postgres.sql                                            |
|                                            | (postgres or oracle) when installing or updating database schema.             |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Values:                                                               |                                                         |
|                                            | database.scriptsuffix.postgres=postgres.sql                                   |                                                         |
|                                            | database.scriptsuffix.oracle=oracle.sql                                       |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-upgradeVersion:               |                                                                               |                                                         |
|                                            |                                                                               |                                                         |
| database.upgradeVersion.<database_version> | Which database scripts to run when updating database schema. There is a       | upgrade-db-to-1.2                                       |
|                                            | database.upgradeVersion entry for every Metacat database schema version.      |                                                         |
|                                            | Each schema version corresponds to an application version.                    |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Values:                                                               |                                                         |
|                                            | database.upgradeVersion.0.0.0=xmltables,loaddtdschema                         |                                                         |
|                                            | database.upgradeVersion.1.2.0=upgrade-db-to-1.2                               |                                                         |
|                                            | database.upgradeVersion.1.3.0=upgrade-db-to-1.3                               |                                                         |
|                                            | database.upgradeVersion.1.4.0=upgrade-db-to-1.4                               |                                                         |
|                                            | database.upgradeVersion.1.5.0=upgrade-db-to-1.5                               |                                                         |
|                                            | database.upgradeVersion.1.6.0=upgrade-db-to-1.6                               |                                                         |
|                                            | database.upgradeVersion.1.7.0=upgrade-db-to-1.7                               |                                                         |
|                                            | database.upgradeVersion.1.8.0=upgrade-db-to-1.8                               |                                                         |
|                                            | database.upgradeVersion.1.9.0=upgrade-db-to-1.9                               |                                                         |
|                                            | database.upgradeVersion.2.0.0=upgrade-db-to-2.0                               |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.initialConnections*               | The number of initial connection that Metacat creates to the database.        | 5                                                       |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 5                                                              |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.incrementConnections*             | The number of connections Metacat creates when it requires                    | 5                                                       |
|                                            | more connections.                                                             |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 5                                                              |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumConnections*               | The maximum number of database connections Metacat can make.                  | 25                                                      |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 200                                                            |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumConnectionAge*             | The maximum time in milliseconds that a database connection can live.         | 120000                                                  |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 120000                                                         |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumConnectionTime*            | The maximum time in milliseconds that a database connection can               | 60000                                                   |
|                                            | accumulate in actual connection time.                                         |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 60000                                                          |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumUsageNumber*               | The maximum number of times a single connection can be used.                  | 100                                                     |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 100                                                            |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.numberOfIndexingThreads*          | The number of threads available for indexing.                                 | 5                                                       |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 5                                                              |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.indexingTimerTaskTime*            | The time in milliseconds between indexing.                                    | 604800000                                               |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 604800000                                                      |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.indexingInitialDelay*             | The delay in milliseconds before first indexing is executed.                  | 3600000                                                 |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 3600000                                                        |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumIndexDelay*                | The time in milliseconds that an indexing thread will wait when it            | 5000                                                    |
|                                            | can't get a doc id before retrying the indexing.                              |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 5000                                                           |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.runDBConnectionRecycleThread*     | Determines whether the database connection pool should run a thread to        | off                                                     |
|                                            | recycle connections. Possible values are "on" and "off"                       |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: off                                                            |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.cycleTimeOfDBConnection*          | The time in milliseconds between connection recycling runs.                   | 30000                                                   |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 30000                                                          |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+
| database.webResultsetSize*                 | Determines the number of results that can be returned to a                    | 7000                                                    |
|                                            | Web browser from a query.                                                     |                                                         |
|                                            |                                                                               |                                                         |
|                                            | Default Value: 7000                                                           |                                                         |
+--------------------------------------------+-------------------------------------------------------------------------------+---------------------------------------------------------+

Authorization and Authentication Properties
-------------------------------------------
Metacat's authorization and authentication properties are described in the 
table below. Properties that can only be edited manually in the ``metacat-site.properties`` 
file are marked. All others are managed with the properties configuration utility.

Authorization and Authentication Properties

.. _Authentication details: ./authinterface.html

+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| Property                          | Description                                                                   | Example                                       |
+===================================+===============================================================================+===============================================+
| .. _auth-class:                   |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.class                        | The class used for user authentication. Currently, both the AuthFile and      | edu.ucsb.nceas.metacat.AuthLdap               |
|                                   | AuthLdap classes are included in the Metacat distribution.                    |                                               |
|                                   | Note: If you implement another authentication strategy by implementing a Java |                                               |
|                                   | class that extends the AuthInterface interface and rebuilding Metacat,        |                                               |
|                                   | change this property to the fully qualified class name of your custom         |                                               |
|                                   | authentication mechanism.                                                     |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: edu.ucsb.nceas.metacat.authentication.AuthFile                 |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| auth.timeoutMinutes*              | The number of minutes that a user will stay logged in to Metacat              | 180                                           |
|                                   | without any activity.                                                         |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: 180                                                            |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-administrators:          |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.administrators               | A semicolon-separated list of ORCID IDs for users who have administrative     | https://orcid.org/0000-0001-2345-6789;        |
|                                   | Metacat privileges. At least one user must be entered when Metacat is         | https://orcid.org/0000-0002-2345-678X         |
|                                   | first installed and configured.                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-user-management-url:     |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.userManagementUrl            | A web page provides the user management such as creating a new user and       | https://identity.nceas.ucsb.edu               |
|                                   | changing password.                                                            |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-file-path:               |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.file.path                    | The absolute path of the password file which stores the username/password     | /var/metacat/certs/password                   |
|                                   | and users' information. This file is used for the file-based authentication   |                                               |
|                                   | mechanism.                                                                    |                                               |
|                                   |                                                                               |                                               |
|                                   | Please see the `Authentication details`_ page for more information.           |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: /var/metacat/certs/password                                    |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-url:                     |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.url                          | The URL of the server that Metacat should use for authentication.             | ldap://ldap.ecoinformatics.org:389/           |
|                                   |                                                                               |                                               |
|                                   | Default Value: ldap://ldap.ecoinformatics.org:389/                            |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-surl:                    |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.surl                         | The URL of the server that Metacat should use for secure authentication.      | ldap://ldap.ecoinformatics.org:389/           |
|                                   |                                                                               |                                               |
|                                   | Default Value: ldap://ldap.ecoinformatics.org:389/                            |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-base:                    |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.base                         | The base part of the distinguished name that Metacat uses for authentication. | dc=ecoinformatics,dc=org                      |
|                                   |                                                                               |                                               |
|                                   | Default Value: dc=ecoinformatics,dc=org                                       |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-allowedSubmitters:       |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.allowedSubmitters            | A semicolon delimited list of users who should be allowed to submit documents | uid=youruser,o=NCEAS,dc=ecoinformatics,dc=org |
|                                   | to Metacat. If no value is specified, all users will be                       |                                               |
|                                   | allowed to submit documents.                                                  |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: (none)                                                         |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-deniedSubmitters:        |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.deniedSubmitters             | A semicolon delimited list of users who should NOT be allowed to              | uid=youruser,o=NCEAS,dc=ecoinformatics,dc=org |
|                                   | submit documents. If no value is specified, all users will be allowed to      |                                               |
|                                   | submit documents.                                                             |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: (none)                                                         |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| ldap.connectTimeLimit*            | The time in milliseconds allowed for LDAP server connections.                 | 5000                                          |
|                                   |                                                                               |                                               |
|                                   | Default Value: 5000                                                           |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| ldap.searchTimeLimit*             | The time in milliseconds allowed for LDAP server searches.                    | 3000                                          |
|                                   |                                                                               |                                               |
|                                   | Default Value: 30000                                                          |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| ldap.searchCountLimit*            | The number of return entries allowed for LDAP server searches.                | 30000                                         |
|                                   |                                                                               |                                               |
|                                   | Default Value: 30000                                                          |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| ldap.referral*                    | The type of LDAP referrals to use. Possible values are "follow",              | follow                                        |
|                                   | "throw" or "none". Refer to LDAP documentation for further information.       |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: follow                                                         |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| ldap.onlySecureConnection*        | Determines whether to use only a secure LDAP server.                          | false                                         |
|                                   | Acceptable values are "true" and "false".                                     |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: false                                                          |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| ldap.onlySecureReferalsConnection*| Determines whether to only use a secure referral server.                      | false                                         |
|                                   | Acceptable values are "true" and "false".                                     |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: false                                                          |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+

XML/EML Properties
------------------
Metacat's XML/EML properties are described below. These properties can only be 
edited manually in the ``metacat-site.properties`` file. 

+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
| Property              | Description                                                      | Example                                           |
+=======================+==================================================================+===================================================+
| xml.saxparser         | The SAX parser used to parse XML documents. Metacat              | org.apache.xerces.parsers.SAXParser               |
|                       | requires a SAX2-compatible XML parser.                           |                                                   |
|                       |                                                                  |                                                   |
|                       | Default Value: org.apache.xerces.parsers.SAXParser               |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
| xml.eml2_0_0namespace | The namespace of EML 2.0.0 documents.                            | eml://ecoinformatics.org/eml-2.0.0                |
|                       |                                                                  |                                                   |
|                       | Default Value: eml://ecoinformatics.org/eml-2.0.0                |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
| xml.eml2_0_1namespace | The namespace of EML 2.0.1 documents.                            | eml://ecoinformatics.org/eml-2.0.1                |
|                       |                                                                  |                                                   |
|                       | Default Value: eml://ecoinformatics.org/eml-2.0.1                |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
| xml.eml2_1_0namespace | The namespace of EML 2.1.0 documents.                            | eml://ecoinformatics.org/eml-2.1.0                |
|                       |                                                                  |                                                   |
|                       | Default Value: eml://ecoinformatics.org/eml-2.1.0                |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
|                       |                                                                  |                                                   |
| xml.packagedoctype    | The doctype of a package file. The system will only              | -//ecoinformatics.org//eml-dataset-2.0.0beta6//EN |
|                       | recognize documents of this type as package files.               | -//ecoinformatics.org//eml-dataset-2.0.0beta4//EN |
|                       | See: package documentation.                                      |                                                   |
|                       |                                                                  |                                                   |
|                       | Default Value: -//ecoinformatics.org//eml-dataset-2.0.0beta6//EN |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
| xml.accessdoctype     | The doctype of an access control list (ACL) file. The system     | -//ecoinformatics.org//eml-access-2.0.0beta6//EN  |
|                       | will only recognize documents of this type as                    | -//ecoinformatics.org//eml-access-2.0.0beta4//EN  |
|                       | access files. See: access control documentation.                 |                                                   |
|                       |                                                                  |                                                   |
|                       | Default Value: -//ecoinformatics.org//eml-access-2.0.0beta6//EN  |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+

EZID Properties
---------------
The EZID service assigning Digital Object Identifiers (DOIs) is included in the Metacat service. 

+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| Property                          | Description                                                                   | Example                                       |
+===================================+===============================================================================+===============================================+
| .. _guid.ezid.enabled:            |                                                                               |                                               |
|                                   |                                                                               |                                               |
| guid.ezid.enabled                 | The enabled status of the EZID service                                        | true                                          |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _guid.ezid.username:           |                                                                               |                                               |
|                                   |                                                                               |                                               |
| guid.ezid.username                | A registered user name in the EZID service                                    | apitest                                       |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _guid.ezid.password:           |                                                                               |                                               |
|                                   |                                                                               |                                               |
| guid.ezid.password                | The password for the user name                                                |                                               |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _guid.ezid.baseurl:            |                                                                               |                                               |
|                                   |                                                                               |                                               |
| guid.ezid.baseurl                 | The base ulr of the specified EZID service                                    | https://ezid.cdlib.org/                       |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _guid.ezid.doishoulder.1:      |                                                                               |                                               |
|                                   |                                                                               |                                               |
| guid.ezid.doishoulder.1           | The DOI shoulder associated with the EZId account                             | doi:10.5072/FK2                               |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+

Sitemap Properties
------------------------

Metacat automatically generates sitemaps for all all publicly-readable datasets and stores them in the sitemaps subdirectory under Metacat's deployment directory.

+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| Property                          | Description                                                                   | Example                                       |
+===================================+===============================================================================+===============================================+
| .. _sitemap.enabled:              |                                                                               |                                               |
|                                   |                                                                               |                                               |
| sitemap.enabled                   | Whether or not sitemaps are enabled.                                          | true                                          |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _sitemap.interval:             |                                                                               |                                               |
|                                   |                                                                               |                                               |
| sitemap.interval                  | The interval, in milliseconds, between rebuilding the sitemap(s).             | 86400000 (24hrs)                              |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _sitemap.location.base:        |                                                                               |                                               |
|                                   |                                                                               |                                               |
| sitemap.location.base             | Base part of the URLs for the location of the sitemap files and the sitemap.  | https://my-metacat.com                        |
|                                   | index. Either full URL or absolute path. Trailing slash optional.             |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _sitemap.entry.base:           |                                                                               |                                               |
|                                   |                                                                               |                                               |
| sitemap.entry.base                | Base part of the URLs for the location entries in the sitemaps.               | https://my-metacat.com/dataset                |
|                                   | Either full URL or absolute path. Trailing slash optional.                    |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+


Additional Properties
----------------------
Additional configuration properties are described below, though there are many more that can be manually edited in the properties file directly. 

+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
| Property              | Description                                                      | Example                                           |
+=======================+==================================================================+===================================================+
| .. _plugin.handlers:  |                                                                  |                                                   |
|                       |                                                                  |                                                   |
| plugin.handlers       | Implementations of the plugin interface:                         | org.example.CustomActionHandler                   |
|                       | edu.ucsb.nceas.metacat.plugin.MetacatHandlerPlugin can be listed |                                                   |
|                       |                                                                  |                                                   |
|                       | Default Value: blank                                             |                                                   |
+-----------------------+------------------------------------------------------------------+---------------------------------------------------+
