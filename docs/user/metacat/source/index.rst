
Metacat Administrator's Guide
=============================

.. sidebar:: Version: |release|

    .. image:: themes/metacatui/static/metacat-logo-darkgray.png
       :height: 130pt

    Feedback and bugs - please create a GitHub Issue:
        * https://github.com/NCEAS/metacat/issues/new

    License: GPL
    
    Release Date: |today|

.. role:: note2

Metacat is a repository for data and metadata (documentation about data) that helps 
scientists find, understand and effectively use data sets they manage or that 
have been created by others. Thousands of data sets are currently documented in 
a standardized way and stored in Metacat systems, providing the scientific 
community with a broad range of science data that--because the data are well and 
consistently described--can be easily searched, compared, merged, or used 
in other ways.  

:note2:`Note:` 

An external Solr HTTP server is required as of Metacat version 2.13.0. You should add the ``tomcat8`` user to the ``solr`` group and the ``solr``  user to the ``tomcat8`` group in order to avoid the file permission issues. The details can be found on the `Solr Server`_ part of the installation page.

If this is the first time you have installed an external Solr server, you need to re-index all existing objects in the Metacat instance. The instruction can be found on the `Regenerating The Index`_ part of the Metacat indexing page.

- Download Metacat

    - Binary Distribution (A war file installation)
        - GZIP File: |bin-link-pre|\ |release|\ |bin-gz-link-post1|\ |release|\ |gz-link-post2|
        - ZIP File: |bin-link-pre|\ |release|\ |bin-zip-link-post1|\ |release|\ |zip-link-post2|
    - Source Distribution (Full source, requiring build)
        - GZIP File: |src-link-pre|\ |release|\ |src-gz-link-post1|\ |release|\ |gz-link-post2|
        - ZIP File: |src-link-pre|\ |release|\ |src-zip-link-post1|\ |release|\ |zip-link-post2|
    - `Older versions`_

.. |bin-link-pre| raw:: html

    <a href="https://knb.ecoinformatics.org/software/dist/metacat-bin-

.. |bin-gz-link-post1| raw:: html

    .tar.gz">metacat-bin-

.. |gz-link-post2| raw:: html

    .tar.gz</a>
    
.. |bin-zip-link-post1| raw:: html

    .zip">metacat-bin-

.. |zip-link-post2| raw:: html

    .zip</a>

.. |src-link-pre| raw:: html

    <a href="https://knb.ecoinformatics.org/software/dist/metacat-src-

.. |src-gz-link-post1| raw:: html

    .tar.gz">metacat-src-

.. |src-zip-link-post1| raw:: html

    .zip">metacat-src-
   
- For Developers: Metacat `API documentation`_

.. _Administrators Guide: https://knb.ecoinformatics.org/software/metacat/MetacatAdministratorGuide.pdf

.. _API documentation: ./api/index.html

.. _Older versions: https://knb.ecoinformatics.org/software/dist/

.. _Solr Server: ./install.html#solr-server

.. _Regenerating The Index: ./query-index.html#regenerating-the-index



Contents
========
.. toctree::
   :numbered:
   :maxdepth: 3

   intro
   contributors
   license
   install
   configuration
   kubernetes
   admin-api
   dataone
   submitting
   query-index
   themes
   authinterface
   replication
   harvester
   oaipmh
   event-logging
   sitemaps
   metacat-properties
   development


Indices and tables
==================

* :ref:`genindex`
* :ref:`search`
