Appendix: Metacat Properties
============================
The most dynamic Metacat Properties are managed using Metacat's Configuration 
Interface (see :doc:`configuration`). These properties, as well as other, 
rarely modified ones can be found in the metacat.properties file. For more 
information about the properties, click one of the following:

* Server Properties
* Application Properties
* Database Properties
* Authorization and Authentication Properties
* XML/EML Properties

Server Properties
-----------------
All of Metacat's server properties are managed with the form-based 
configuration utility, though they can also be accessed More information on 
each is included below.


Metacat Server Properties

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
| .. _server-httpPort:      |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.httpPort           | The network port used to access Metacat for non-secure (standard) connections.           | 80                     |
|                           | This is usually 80 if Apache Web server is running, and 8080 if Tomcat is running alone. |                        |
|                           |                                                                                          |                        |
|                           | Default Value: 80                                                                        |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+
| .. _server-httpSSLPort:   |                                                                                          |                        |
|                           |                                                                                          |                        |
| server.httpSSLPort        | The network port used to access Metacat for secure connections. This is usually          | 443                    |
|                           | 443 if Apache Web server is running, and 8443 if Tomcat is running alone.                |                        |
|                           |                                                                                          |                        |
|                           | Default Value: 443                                                                       |                        |
+---------------------------+------------------------------------------------------------------------------------------+------------------------+

Application Properties
----------------------

Metacat's application properties are described below. Properties that can only 
be edited manually in the ``metacat.properties`` file are marked. All 
others are managed with the properties configuration utility.

+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| Property                             | Description                                                                 | Example                       |
+======================================+=============================================================================+===============================+
| application.metacatVersion*          | The Metacat version number. It is set by the build engineer                 | 1.9.0                         |
|                                      | at build time. Usually, the value should never be changed.                  |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: X.X.X (where X.X.X is the current version of Metacat)        |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| application.metacatReleaseInfo*      | Release information for display purposes. Typically the property            | Release Candidate 1           |
|                                      | is set during the release candidate cycle to let users know which           |                               |
|                                      | candidate they are downloading.                                             |                               |
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
|                                      | is "knb", but it can be changed to other things.                            |                               |
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
| .. _application.knbSiteURL:          |                                                                             |                               |
|                                      |                                                                             |                               |
| application.knbSiteURL               | The main KNB website.                                                       | http://knb.ecoinformatics.org |
|                                      |                                                                             |                               |
|                                      | Default Value: http://knb.ecoinformatics.org                                |                               |
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
|                                      | because all files in this may be purged programmatically. The temporary     |                               |
|                                      | file directory must be writable by the user that starts Apache.             |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/temporary                                       |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+
| .. _solr.homeDir:                    |                                                                             |                               |
|                                      |                                                                             |                               |
| solr.homeDir                         | The directory where the Metacat index component stores the SOLR index.      | /var/metacat/solr-home        |
|                                      | The directory must be writable by the user that starts Tomcat.              |                               |
|                                      |                                                                             |                               |
|                                      | Default Value: /var/metacat/solr-home                                       |                               |
+--------------------------------------+-----------------------------------------------------------------------------+-------------------------------+

Database Properties
-------------------
Metacat's database properties are described next. Properties that can only be 
edited manually in the metacat.properties file are marked. All others 
are managed with the properties configuration utility.

Metacat Database Properties

+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| Property                                   | Description                                                                                              | Example                                                 |
+============================================+==========================================================================================================+=========================================================+
| .. _database-connectionURI:                |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.connectionURI                     | The JDBC connection URI for the main database instance of Metacat.                                       | ``jdbc:postgresql://yourserver.yourdomain.edu/metacat`` |
|                                            | The URI is formatted like: ``jdbc:<database_type>:thin@<your_server_name>:1521:<metacat_database_name>`` |                                                         |
|                                            | NOTE: You must create an empty database prior to initial Metacat configuration.                          |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: jdbc:postgresql://localhost/metacat                                                       |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-user:                         |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.user                              | The user for the main database instance of Metacat. The user must                                        | metacat-user                                            |
|                                            | have already been created on the database.                                                               |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-password:                     |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.password                          | The password of the user for the main database instance of Metacat.                                      | securepassword4843                                      |
|                                            | The password must have already been created for the user.                                                |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-type:                         |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.type                              | The type of database you are running. Currently, there are two supported                                 | postgres                                                |
|                                            | types, Oracle and Postgres.                                                                              |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-driver:                       |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.driver                            | The JDBC driver to be used to access the main database instance of Metacat.                              | org.postgresql.Driver                                   |
|                                            | There is one driver associated with each type of database.                                               |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-adapter:                      |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.adapter                           | The adapter class that allows Metacat to access your database type.                                      | edu.ucsb.nceas.dbadapter.PostgresqlAdapter              |
|                                            | There is one adapter associated with each type of database.                                              |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-scriptsuf:                    |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.scriptsuffix.<database_type>      | The script suffix tells the system which database scripts to run                                         | postgres.sql                                            |
|                                            | (postgres or oracle) when installing or updating database schema.                                        |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Values:                                                                                          |                                                         |
|                                            | database.scriptsuffix.postgres=postgres.sql                                                              |                                                         |
|                                            | database.scriptsuffix.oracle=oracle.sql                                                                  |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| .. _database-upgradeVersion:               |                                                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
| database.upgradeVersion.<database_version> | Which database scripts to run when updating database schema. There is a                                  | upgrade-db-to-1.2                                       |
|                                            | database.upgradeVersion entry for every Metacat database schema version.                                 |                                                         |
|                                            | Each schema version corresponds to an application version.                                               |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Values:                                                                                          |                                                         |
|                                            | database.upgradeVersion.0.0.0=xmltables,loaddtdschema                                                    |                                                         |
|                                            | database.upgradeVersion.1.2.0=upgrade-db-to-1.2                                                          |                                                         |
|                                            | database.upgradeVersion.1.3.0=upgrade-db-to-1.3                                                          |                                                         |
|                                            | database.upgradeVersion.1.4.0=upgrade-db-to-1.4                                                          |                                                         |
|                                            | database.upgradeVersion.1.5.0=upgrade-db-to-1.5                                                          |                                                         |
|                                            | database.upgradeVersion.1.6.0=upgrade-db-to-1.6                                                          |                                                         |
|                                            | database.upgradeVersion.1.7.0=upgrade-db-to-1.7                                                          |                                                         |
|                                            | database.upgradeVersion.1.8.0=upgrade-db-to-1.8                                                          |                                                         |
|                                            | database.upgradeVersion.1.9.0=upgrade-db-to-1.9                                                          |                                                         |
|                                            | database.upgradeVersion.2.0.0=upgrade-db-to-2.0                                                          |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.initialConnections*               | The number of initial connection that Metacat creates to the database.                                   | 5                                                       |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 5                                                                                         |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.incrementConnections*             | The number of connections Metacat creates when it requires                                               | 5                                                       |
|                                            | more connections.                                                                                        |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 5                                                                                         |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumConnections*               | The maximum number of database connections Metacat can make.                                             | 25                                                      |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 200                                                                                       |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumConnectionAge*             | The maximum time in milliseconds that a database connection can live.                                    | 120000                                                  |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 120000                                                                                    |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumConnectionTime*            | The maximum time in milliseconds that a database connection can                                          | 60000                                                   |
|                                            | accumulate in actual connection time.                                                                    |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 60000                                                                                     |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumUsageNumber*               | The maximum number of times a single connection can be used.                                             | 100                                                     |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 100                                                                                       |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.numberOfIndexingThreads*          | The number of threads available for indexing.                                                            | 5                                                       |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 5                                                                                         |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.indexingTimerTaskTime*            | The time in milliseconds between indexing.                                                               | 604800000                                               |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 604800000                                                                                 |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.indexingInitialDelay*             | The delay in milliseconds before first indexing is executed.                                             | 3600000                                                 |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 3600000                                                                                   |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.maximumIndexDelay*                | The time in milliseconds that an indexing thread will wait when it                                       | 5000                                                    |
|                                            | can't get a doc id before retrying the indexing.                                                         |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 5000                                                                                      |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.runDBConnectionRecycleThread*     | Determines whether the database connection pool should run a thread to                                   | off                                                     |
|                                            | recycle connections. Possible values are "on" and "off"                                                  |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: off                                                                                       |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.cycleTimeOfDBConnection*          | The time in milliseconds between connection recycling runs.                                              | 30000                                                   |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 30000                                                                                     |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.queryignoredparams*               | Parameters to ignore in a structured XML query.                                                          | enableediting                                           |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: enableediting,foo                                                                         |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.usexmlindex*                      | Determines whether to use XML indexes when finding                                                       | true                                                    |
|                                            | documents. Possible values are true and false.                                                           |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: true                                                                                      |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.appResultsetSize*                 | Determines the number of results that can be returned to an application from a query.                    | 7000                                                    |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 7000                                                                                      |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.webResultsetSize*                 | Determines the number of results that can be returned to a                                               | 7000                                                    |
|                                            | Web browser from a query.                                                                                |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 7000                                                                                      |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.xmlReturnfieldCount*              | If the query results of a query are returned more times                                                  | 0                                                       |
|                                            | than this value, then those results will be inserted into the xml_queryresult                            |                                                         |
|                                            | table in the database. For example, if you want results for                                              |                                                         |
|                                            | a query to be stored in xml_queryresult only when it has been requested                                  |                                                         |
|                                            | 50 times, set this value to 50.                                                                          |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 0                                                                                         |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.queryresultStringLength*          | The max size of the query result string in the queryresult table. This                                   | 500000                                                  |
|                                            | should be set to some number less than 4000 if an Oracle                                                 |                                                         |
|                                            | database is being used.                                                                                  |                                                         |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 500000                                                                                    |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.queryresultCacheSize*             | The number of query results that will be cached.                                                         | 500                                                     |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: 500                                                                                       |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+
| database.queryCacheOn*                     | Determines whether query caching is turned on. Possible values are "on" and "off"                        | on                                                      |
|                                            |                                                                                                          |                                                         |
|                                            | Default Value: on                                                                                        |                                                         |
+--------------------------------------------+----------------------------------------------------------------------------------------------------------+---------------------------------------------------------+

Authorization and Authentication Properties
-------------------------------------------
Metacat's authorization and authentication properties are described in the 
table below. Properties that can only be edited manually in the ``metacat.properties`` 
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
| auth.administrators               | A colon separated list of LDAP users or groups that have administrative       | uid=youruser,o=NCEAS,dc=ecoinformatics,dc=org |
|                                   | Metacat privileges. At least one user or group must be entered when           | cn=yourgroup,o=NCEAS,dc=ecoinformatics,dc=org |
|                                   | Metacat is first installed and configured. All accounts must exist            |                                               |
|                                   | in LDAP in order to continue with the configuration.                          |                                               |
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
| auth.allowedSubmitters            | A colon delimited list of users who should be allowed to submit documents     | uid=youruser,o=NCEAS,dc=ecoinformatics,dc=org |
|                                   | to Metacat. If no value is specified, all users will be                       |                                               |
|                                   | allowed to submit documents.                                                  |                                               |
|                                   |                                                                               |                                               |
|                                   | Default Value: (none)                                                         |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _auth-deniedSubmitters:        |                                                                               |                                               |
|                                   |                                                                               |                                               |
| auth.deniedSubmitters             | A colon delimited list of users who should NOT be allowed to                  | uid=youruser,o=NCEAS,dc=ecoinformatics,dc=org |
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
edited manually in the metacat.properties file. 

XML/EML Properties

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


Data Manager Properties
------------------------
The EML Data Manager is also included for extended data-query operations. Note that this feature is still experimental. 

+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| Property                          | Description                                                                   | Example                                       |
+===================================+===============================================================================+===============================================+
| .. _datamanager.server:           |                                                                               |                                               |
|                                   |                                                                               |                                               |
| datamanager.server                | The server for the Datamanager library to use for temporary db storage        | localhost                                     |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _datamanager.database:         |                                                                               |                                               |
|                                   |                                                                               |                                               |
| datamanager.database              | The database name for the Datamanager                                         | datamananger                                  |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _datamanager.user:             |                                                                               |                                               |
|                                   |                                                                               |                                               |
| datamanager.user                  | The username for the Datamanager DB                                           | datamananger                                  |
|                                   |                                                                               |                                               |
+-----------------------------------+-------------------------------------------------------------------------------+-----------------------------------------------+
| .. _datamanager.password:         |                                                                               |                                               |
|                                   |                                                                               |                                               |
| datamanager.password              | The password for the Datamanager user                                         | datamananger                                  |
|                                   |                                                                               |                                               |
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

