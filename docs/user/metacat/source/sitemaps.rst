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

Metacat's sitemaps functionality is controlled by four properties in Metacat's
configuration settings. (For more details on changing Metacat's configuration, see
:ref:`configuration-properties-overview`):

 - ``sitemap.enabled``: Controls whether sitemaps are automatically generated
   while Metacat is running. Defaults to true.

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

and one or more sitemap XML files named:

  sitemap<X>.xml

where ``<X>`` is a number (e.g., 1 or 2) used to increment each sitemap file.
Because Metacat limits the number of sitemap entries in each sitemap file to
50,000, the servlet creates an additional sitemap file for each group of
50,000 entries.

Verify that your sitemap files are available to the Web by browsing to::

  <your_web_context>/sitemaps/sitemap<X>.xml
  (e.g., https://example.org/metacat/sitemaps/sitemap1.xml)

Serving Your Sitemaps
---------------------

In most scenarios, you'll want to take extra steps to make sure your sitemaps
are served correctly so they're available and indexable by Google. Because
Metacat places sitemap XML files in ``<your_web_context>/sitemaps``, you'll need
to configure your web server to serve these files.

As an example, a sample configuration is presented for the Apache 2 web server
that uses `mod_rewrite` to redirect clients accessing your sitemaps from the top
level of your website to their location under the Metacat deployment context:

 **Note:** Ensure `mod_rewrite` is enabled

  ::

    RewriteRule ^/(sitemap.+) /metacat/sitemaps/$1 [R=303]

You should also ensure your ``robots.txt`` file correctly points to the location
of the ``sitemap_index.xml``. e.g., for example.org:

``robots.txt``:

  ::

    User-agent: *
    Allow: /

    sitemap: https://example.org/sitemap_index.xml

Registering a Sitemap
---------------------
Before Google will begin indexing the public files in your Metacat, you must
register the sitemaps. To register your sitemaps and ensure that they are up
to date:

 1. `Create a Google account`_.

.. _Create a Google account: https://support.google.com/accounts/answer/27441?hl=en

 2. Submit your Metacat site to the `Sitemaps report`_.
    You can also submit it programmatically with the `Google Search Console API`_.
    For more information, please see the Google help site for `how to register sitemaps`_.

.. _Sitemaps report: https://search.google.com/search-console/sitemaps
.. _Google Search Console API: https://developers.google.com/webmaster-tools/v1/sitemaps/submit
.. _how to register sitemaps: https://developers.google.com/search/docs/crawling-indexing/sitemaps/build-sitemap

 Note: Register the full URL path to your sitemap files, including the http:// (or https://) headers.

Once the sitemaps are registered, Google will begin to index the public
documents in your Metacat repository.

 **Reminder:** As you add more publicly accessible data to Metacat, you will need to
 periodically revisit the Google Webmaster Tools utility to refresh your
 sitemap registration.
