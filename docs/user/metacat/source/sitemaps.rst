Enabling Web Searches: Sitemaps
===============================

Sitemaps are XML files that tell search engines - such as Google, which is 
discussed in this section - which URLs on your websites are available for 
crawling. Currently, the only way for a search engine to crawl and index 
Metacat so that individual metadata entries are available via Web searches 
is with a sitemap. Metacat automatically creates sitemaps for all public 
documents in the repository. However, you must register the sitemaps with 
the search engine before it will take effect.


Creating a Sitemap
------------------

Metacat automatically generates a sitemap file for all public documents in 
the repository on a daily basis. The sitemap file(s) must be available via 
the Web on your server, and must be registered with Google before they take 
effect. For information on the sitemap protocol, please refer to the Google 
page on using the sitemap protocol. You can view Metacat's sitemap files at:: 

  <webapps_dir>/sitemaps

The directory contains one or more XML files named::

  metacat<X>.xml

where ``<X>`` is a number (e.g., 1 or 2) used to increment each sitemap file. 
Because Metacat limits the number of sitemap entries in each sitemap file to 
25,000, the servlet creates an additional sitemap file for each group of 
25,000 entries. 

Verify that your sitemap files are available to the Web by browsing to::

  <your_web_context>/sitemaps/metacat<X>.xml 
  (e.g., your.server.org/metacat/sitemaps/metacat1.xml)

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
