Enabling Web Searches: Sitemaps
===============================

Sitemaps are XML files that tell search engines - such as Google, which is 
discussed in this section - which URLs on your websites are available for 
crawling. Currently, the only way for a search engine to crawl and index 
Metacat so that individual metadata entries are available via Web searches 
is with a sitemap. Metacat automatically creates sitemaps for all public 
documents in the repository that meet these criteria:

- Is publicly readable
- Is metadata
- Is the newest version in a version chain
- Is not archived

However, you must register the sitemaps with the search engine before it will 
take effect.

Configuration
-------------

Metacat's sitemaps functionality is controlled by three properties in 
metacat.properties.

######## Sitemap section              #########################################
# Sitemap Interval (in milliseconds) between rebuilding the sitemap
sitemap.interval=86400000
# Base part of the URLs for the location of the sitemap files themselves. 
# Either full URL or absolute path. Trailing slash optional.
sitemap.location.base=/metacatui
# Base part of the URLs for the location entries in the sitemaps which should
# be the base URL of the dataset landing page.
# Either full URL or absolute path. Trailing slash optional.
sitemap.entry.base=/metacatui/view


- ``sitemap.interval``: Controls the interval, in milliseconds, between 
  rebuilding the sitemap index and sitemap files.
- ``sitemap.location.base``: Controls the URL pattern used in the 
  ``sitemap_index.xml`` file. You can use either a full URL 
  (e.g., ``https://example.com/some_path``) or a URL relative to your server 
  (e.g., ``/some_path``). This is different than the ``sitemap.entry.base`` 
  property (see directly below).
- ``sitemap.entry.base``: Controls the URL pattern used for the entires in the
  individual sitemap files (e.g., ``sitemap1.xml``). You can use either a full 
  URL (e.g., ``https://example.com/some_path``) or a URL relative to your 
  server (e.g., ``/some_path``).

Creating a Sitemap
------------------

Metacat automatically generates a sitemap file for all public documents in 
the repository on a daily basis. The sitemap file(s) must be available via 
the Web on your server, and must be registered with Google before they take 
effect. For information on the sitemap protocol, please refer to the Google 
page on using the sitemap protocol. You can view Metacat's sitemap files at::

  <your_web_context>/sitemaps

The directory contains an index file:

  sitemap_index.xml
  
and one or more sitemap XML files named::

  sitemap<X>.xml

where ``<X>`` is a number (e.g., 1 or 2) used to increment each sitemap file. 
Because Metacat limits the number of sitemap entries in each sitemap file to 
50,000, the servlet creates an additional sitemap file for each group of 
50,000 entries. 

Verify that your sitemap files are available to the Web by browsing to::

  <your_web_context>/sitemaps/sitemap<X>.xml 
  (e.g., https://example.org/metacat/sitemaps/sitemap1.xml)

Registering a Sitemap
---------------------
Before Google will begin indexing the public files in your Metacat, you must 
register the sitemaps. To register your sitemaps and ensure that they are up 
to date:

1. Register for a Google Webmaster Tools account, and add your Metacat 
   site to the Dashboard.
2. From your Google Webmaster Tools site account, register your sitemaps. 
   See the Google help site for more information about how to register sitemaps. 
   Note: Register the full URL path to your sitemap files, including 
   the http:// (or https://) headers.

Once the sitemaps are registered, Google will begin to index the public 
documents in your Metacat repository. 

NOTE: As you add more publicly accessible data to Metacat, you will need to 
periodically revisit the Google Webmaster Tools utility to refresh your 
sitemap registration.
