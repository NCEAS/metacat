# Metacat Security Advisories
[wfsdf](#ghsa-57g5-qq6w-7jr8-metacat-acts-as-unintended-proxy-to-backend-apache-solr-engine)

## GHSA-57g5-qq6w-7jr8: Metacat acts as unintended proxy to backend Apache SOLR engine

- CVE-2026-50022
- Severity: Moderate
- Affected versions: <= 3.4.1
- Patched versions: >= 3.4.2

### Impact

Metacat versions 3.4.1 and prior are vulnerable to an information disclosure flaw when processing search queries that are passed to its integrated Apache Solr backend.  The web application is a confused deputy — it has privileged access to SOLR that it is incorrectly granting to end users when they request it via the `qt` parameter. By leveraging the `qt` parameter to call the `/admin/file` handler, accessible via endpoints such as `/d1/mn/v2/query/solr/`, unauthenticated clients can request files directly from the Solr core configuration directory.

Even on newer Apache Solr deployments (>= version 7.0) where `handleSelect=false` is enforced by default to reject legacy `qt` parameters, the Apache `SolrJ` client library utilized by Metacat automatically marshals `qt` parameters from incoming client requests and reformats them into an alternate structure that Solr accepts. When Solr processes this request and returns the targeted configuration data, Metacat throws a processing exception and embeds the raw contents of the underlying Solr configuration file directly into the body of the XML error response sent back to the user.

This vulnerability permits an unauthenticated attacker to access internal configuration data. Exposure of these internals (e.g. `solrconfig.xml`) can reveal underlying service details, which potentially exposes the host infrastructure to system profiling and increases the viability of secondary, targeted attack vectors.

### Patches

This vulnerability was remediated fully in Metacat version 3.4.2. All vulnerabilities were verified to no longer be present after the fix was applied. All users are encouraged to upgrade as soon as possible. Metacat only supports the latest release, and this patch will not be backported to earlier versions.

### Workarounds

If upgrading is not immediately feasible, deployments can be secured by applying the following remediation steps:

1. Restrict Parameter Access: These are example steps, and may need to be adapted for different environments:

   1. Install and configure the Apache `ModSecurity` module (or an equivalent) to intercept and drop any inbound requests containing the `qt` parameter across all HTTP methods. (Apache's standard `mod_rewrite` rules work only on URLs; they will miss parameters passed inside HTTP POST request bodies). Ubuntu/Debian example:

       ```shell
       sudo apt update
       sudo apt install libapache2-mod-security2
       sudo a2enmod security2
       ```

   2. In the `ModSecurity` configuration file (for example, in `/etc/modsecurity/`), ensure request body parsing is enabled and change the detection mode to On; for example:

       ```apache
       SecRuleEngine On
       SecRequestBodyAccess On
       ```

   3. Append the custom rule to your metacat-specific configuration; for example:

       ```apache
       # Drop any request attempting to use the forbidden qt parameter
       # (Handles standard, single, and double encoding)
       SecRule ARGS_NAMES "@rx ^qt(=|$)" \
         "id:100004,\
         phase:2,\
         deny,\
         status:403,\
         log,\
         auditlog,\
         t:urlDecode,\
         msg:'Forbidden Metacat qt parameter submission detected'"
       ```

   4. Ensure it is loaded by your main Apache configuration (httpd.conf or apache2.conf) using an include directive.

   5. Check the syntax, and restart apache to pick up the changes. Ubuntu/Debian example:

       ```shell
       sudo apachectl configtest

       # Restart service
       sudo systemctl restart apache2 
       ```

   NOTE: If not using Apache, then configure any upstream reverse proxy, load balancer, or network firewall to do the same.

2. If the underlying Solr deployment has been configured with `handleSelect=true` (either explicitly, or by default in versions < 7.0), then change it to set `handleSelect=false`

---

---

## GHSA-6g6j-wh5h-77h5: Unauthenticated SQL injection vulnerability for Metacat

- CVE-2026-48528
- Severity: Critical
- Affected versions: >= 2.0.0, <= 3.4.0
- Patched versions: >= 3.4.1

### Impact

Metacat versions 2.0.0 through 3.4.0 contain an unauthenticated SQL injection vulnerability in the `/cn/v1/object` and `/cn/v2/object` REST API endpoints due to unsanitized user input that can be passed through to the backend SQL database. The `nodeId` parameter can be modified to inject SQL commands, and the results are returned in error messages. Metacat appends the user-supplied data into the sql query without sanitization or parameterization. This allows extraction of arbitrary data from the underlying PostgresQL database, fully exposing protected information to the attacker. This is accomplished by leveraging the error reporting mechanisms in Metacat, where SQL error responses are mirrored back to the caller in the XML error message returned by Metacat. One approach, for example, is to use the PostgreSQL `CAST` function to generate an error with the results of an arbitrary subquery, which is then injected into the XML error message returned by Metacat. Attackers do not need to be authenticated to execute the attack.  In addition, arbitrary SQL statements that insert, update, and delete data in the Metacat database can be executed, resulting in full compromise of all data in the database. Full proof of concept attacks have been developed and verified for these vulnerabilities.

The impact of this vulnerability is critical for Metacat deployments in the DataONE network where information from the database can be exfiltrated, added, changed, or deleted. This includes management information about the data catalog, access log information about who accessed data, identifying information about individuals including their ORCID identifier and client IP address, access control information about who should be able to access and modify data, and other critical internals of the data system.

### Patches

This sql injection vulnerability was remediated fully in Metacat version 3.4.1. All vulnerabilities were verified to no longer be present after the fix was applied. All users are encouraged to upgrade as soon as possible. Metacat only supports the latest release, and this patch will not be backported to earlier versions.

### Workarounds

If upgrading to Metacat 3.4.1 isn't immediately possible, most deployments can mitigate the issue by disabling the `/cn` REST endpoints in the webapp deployment. This API is not needed or used by member repositories in the DataONE network, as it is only used by the DataONE Coordinating Node deployments. Consequently, this API can be disabled without reduction of functionality for most deployments. To disable the vulnerable endpoints, simply remove the servlet and servlet-mapping for the `/cn` endpoints in the servlet engine associated with the two servlets, `edu.ucsb.nceas.metacat.restservice.v1.CNRestServlet` and `edu.ucsb.nceas.metacat.restservice.v2.CNRestServlet`. For example, in Tomcat, remove the relevant `servlet-mapping` elements from the application web.xml file in Metacat (typically located at `${TOMCAT_HOME}/webapps/metacat/WEB-INF/web.xml`). Here are the lines of text to be removed from a typical web.xml file, but the exact local configuration may vary:

```xml
<servlet>
  <servlet-name>CNRestServletV1</servlet-name>
  <servlet-class>edu.ucsb.nceas.metacat.restservice.v1.CNRestServlet</servlet-class>
  <init-param>
    <param-name>debug</param-name>
    <param-value>1</param-value>
  </init-param>
  <init-param>
    <param-name>listings</param-name>
    <param-value>true</param-value>
  </init-param>
  <load-on-startup>15</load-on-startup>
</servlet>
<servlet>
  <servlet-name>CNRestServletV2</servlet-name>
  <servlet-class>edu.ucsb.nceas.metacat.restservice.v2.CNRestServlet</servlet-class>
  <init-param>
    <param-name>debug</param-name>
    <param-value>1</param-value>
  </init-param>
  <init-param>
    <param-name>listings</param-name>
    <param-value>true</param-value>
  </init-param>
  <load-on-startup>15</load-on-startup>
</servlet>

<servlet-mapping>
  <servlet-name>CNRestServletV1</servlet-name>
  <url-pattern>/d1/cn/v1/*</url-pattern>
</servlet-mapping>
<servlet-mapping>
  <servlet-name>CNRestServletV2</servlet-name>
  <url-pattern>/d1/cn/v2/*</url-pattern>
</servlet-mapping>
```

---

---

## GHSA-m852-f287-7cgw: unauthenticated path traversal in Metacat 2.x

- CVE-2026-47754
- Severity: Critical
- Affected versions: <= 2.19.1
- Patched versions: >= 3.0.0

### Impact

Metacat versions 2.x through 2.19.1 and all 1.x versions contain an unauthenticated path traversal in the `archiveEntryName` parameter of the `action=read` endpoint that is part of the original 1.x Metacat API. `ArchiveHandler.readArchiveEntry()` concatenates the user-supplied parameter into a filesystem path without validation, and the surrounding `hasReadPermission()` check is commented out. An unauthenticated remote attacker can read any file accessible to the Tomcat process by sending a single GET request.

Proof-of-concept exploits have been demonstrated and verified against this vulnerability, and it should be considered easily exploitable for any Metacat deployment < 3.0.0 by any user with access to the 1.x API.

Through this vulnerability, production 2.x deployments are exposed to credential theft, client certificate and private key exfiltration enabling member node impersonation within the federation, embargoed research data disclosure, and broad system reconnaissance. Given Metacat's deployment footprint across the DataONE network of repositories and federally funded research programs, the population of exposed 2.x instances is non-trivial.

### Patches

The vulnerability was eliminated in Metacat version 3.0.0 and after by eliminating the entire Metacat 1.x API that exposed this vulnerability. The vulnerability was remediated in April 2024 with the release of Metacat 3.0.0 via commit 07e034b (PR #1713, issue #1365), which removed the legacy Metacat API including ArchiveHandler.java. The commit message and issue reference architectural cleanup, not a security fix, and no advisory or CVE was issued. The 2.x branch was not and will not be backported, as is standard practice in Metacat, which only supports the most current release. 2.19.1 remains vulnerable with identical code and is beyond its supported lifetime.

### Mitigations

- Best: Upgrade to Metacat > 3.0.0, and preferably the current release (currently at 3.4.0).

- Alternative: Disable or restrict 1.x API servlets

Because the vulnerable 1.x API is no longer used or necessary in most Metacat deployments, restricting access to the old API endpoints can reduce or eliminate exposure for 2.19.x deployments. For example, if the Metacat servlet application is served through Apache Tomcat, one can prevent access to the affected APIs by disabling (or restricting access to) the 1.x API endpoint servlets. An example is provided below. Depending on the deployment scenario, restricting API access could reduce functionality depending on client needs, but equivalent capabilities are present in the 3.x DataONE REST API. Nevertheless, Metacat 2.19.x is beyond its support lifetime and all users are advised to upgrade to the most recent 3.x version that is currently supported.

For example, to disable 1.x servlet endpoints in Tomcat, edit the `web.xml` configuration file for the deployment, removing or restricting any `servlet-mapping` for the servlet-class `edu.ucsb.nceas.metacat.MetaCatServlet` (assuming that is where they are defined, each deployment may vary). While not strictly required for this CVE, other 1.x servlet applications could be preventatively disabled or restricted as well, including `edu.ucsb.nceas.metacat.replication.ReplicationServlet`, `edu.ucsb.nceas.metacat.advancedsearch.AdvancedSearchServlet`, `edu.ucsb.nceas.metacat.harvesterClient.HarvesterServlet`, and `edu.ucsb.nceas.metacat.oaipmh.provider.server.OAIHandler`. These will remove the associated functionality, leaving only the DataONE API operational.

```
    <!-- Example, delete all of the servlet-mappings that expose the `edu.ucsb.nceas.metacat.MetaCatServlet`  -->
    <servlet-mapping>
        <servlet-name>metacat</servlet-name>
        <url-pattern>/metacat</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>metacat</servlet-name>
        <url-pattern>/metacat/*</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>metacat</servlet-name>
        <url-pattern>/servlet/metacat</url-pattern>
    </servlet-mapping>

    <-- Optional but recommended: also disable other 1.x API servlets and mappings, such as: -->
    <servlet>
        <servlet-name>replication</servlet-name>
        <servlet-class>edu.ucsb.nceas.metacat.replication.ReplicationServlet</servlet-class>
        <load-on-startup>3</load-on-startup>
    </servlet>
    <servlet>
        <servlet-name>AdvancedSearchServlet</servlet-name>
        <servlet-class>edu.ucsb.nceas.metacat.advancedsearch.AdvancedSearchServlet</servlet-class>
        <load-on-startup>4</load-on-startup>
    </servlet>
    <servlet>
        <servlet-name>HarvesterServlet</servlet-name>
        <servlet-class>edu.ucsb.nceas.metacat.harvesterClient.HarvesterServlet</servlet-class>
        <load-on-startup>5</load-on-startup>
    </servlet>
    <servlet>
        <servlet-name>DataProvider</servlet-name>
        <servlet-class>edu.ucsb.nceas.metacat.oaipmh.provider.server.OAIHandler</servlet-class>
        <load-on-startup>6</load-on-startup>
    </servlet>
    <servlet>
      <servlet-name>HarvesterRegistrationLogin</servlet-name>
      <servlet-class>edu.ucsb.nceas.metacat.harvesterClient.HarvesterRegistrationLogin</servlet-class>
      <load-on-startup>7</load-on-startup>
    </servlet>
    <servlet>
      <servlet-name>HarvesterRegistration</servlet-name>
      <servlet-class>edu.ucsb.nceas.metacat.harvesterClient.HarvesterRegistration</servlet-class>
      <load-on-startup>8</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>replication</servlet-name>
        <url-pattern>/replication</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>replication</servlet-name>
        <url-pattern>/servlet/replication</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>AdvancedSearchServlet</servlet-name>
        <url-pattern>/advancedSearchServlet</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>DataProvider</servlet-name>
        <url-pattern>/dataProvider</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
      <servlet-name>HarvesterRegistrationLogin</servlet-name>
      <url-pattern>/harvesterRegistrationLogin</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
      <servlet-name>HarvesterRegistration</servlet-name>
      <url-pattern>/harvesterRegistration</url-pattern>
    </servlet-mapping>
```

After removing those features, restart Tomcat or whichever software is hosting your servlets.

---

---

## GHSA-wrc6-rc34-hrcg: unauthenticated SQL injection vulnerability for Metacat 2.x

- CVE-2026-48114
- Severity: Critical
- Affected versions: <= 2.19.1
- Patched versions: >= 3.0.0

### Impact

Metacat versions 2.x through 2.19.1 contain an unauthenticated SQL injection in the `/harvesterRegistration` endpoint. `HarvesterRegistration.dbInsert()` builds an INSERT against HARVEST_SITE_SCHEDULE via string concatenation, using a `quoteString()` helper that performs raw single-quote wrapping without escaping.

Three request parameters reach the sink: `unit`, `contactEmail`, and `documentListURL`.

The servlet does not verify a real LDAP identity.  Allowing the vulnerable insert to proceed. Since the PostgreSQL backend permits stacked queries via `Statement.executeUpdate()`, this vulnerability allows full read/write/execute access in the Metacat database context.

A second-order SQL injection also exists: an attacker-controlled `documentListURL` is fetched by the scheduled harvester, and the parsed `<scope>` element flows unsanitized.

Production Metacat 2.x deployments are exposed to full read/write access to the PostgreSQL database, including credentials, session material, and the xml_documents corpus. Although Metacat's use across DataONE, KNB, the Arctic Data Center, and ESS-DIVE and other repositories, is substantial, most deployments have already upgraded to 3.x releases and so the population of exposed instances is minimal.

### Patches

The vulnerability was remediated in Metacat 3.0.0 (commit 820d5953 in 2023) via the removal of the harvesterClient package. However, no security advisory was issued, and the 2.x branch remains vulnerable. The 2.x branch was not and will not be backported, as is standard practice in Metacat, which only supports the most current release. Version 2.19.1 remains vulnerable with identical code and is beyond its supported lifetime.

### Workarounds

- Best: Upgrade to Metacat > 3.0.0, and preferably the current release (currently >= 3.4.0).

- Alternative: Disable or restrict 1.x API servlets

Because the vulnerable 1.x API is no longer used or necessary in most Metacat deployments, and the Harvester module has been fully removed from subsequent releases, restricting access to the old API endpoints can reduce or eliminate exposure for 2.19.x deployments. For example, if the Harvester servlet application is served through Apache Tomcat, one can prevent access to the affected APIs by disabling (or restricting access to) the 1.x API endpoint servlets. An example is provided below. Depending on the deployment scenario, restricting API access could reduce functionality depending on client needs. Nevertheless, Metacat 2.19.x is beyond its support lifetime and all users are advised to upgrade to the most recent 3.x version that is currently supported.

For example, to disable Harvester servlet endpoints in Tomcat, edit the `web.xml` configuration file for the deployment, removing or restricting any `servlet-mapping` for the servlet-class `edu.ucsb.nceas.metacat.harvesterClient.HarvesterServlet` (assuming that is where they are defined, each deployment may vary) and related servlets.

```
    <!-- Example, delete all of the servlets and servlet-mappings that expose the `edu.ucsb.nceas.metacat.harvesterClient.HarvesterServlet` and related servlets and mappings  -->

    <servlet>
        <servlet-name>HarvesterServlet</servlet-name>
        <servlet-class>edu.ucsb.nceas.metacat.harvesterClient.HarvesterServlet</servlet-class>
        <load-on-startup>5</load-on-startup>
    </servlet>
    <servlet>
        <servlet-name>DataProvider</servlet-name>
        <servlet-class>edu.ucsb.nceas.metacat.oaipmh.provider.server.OAIHandler</servlet-class>
        <load-on-startup>6</load-on-startup>
    </servlet>
    <servlet>
      <servlet-name>HarvesterRegistrationLogin</servlet-name>
      <servlet-class>edu.ucsb.nceas.metacat.harvesterClient.HarvesterRegistrationLogin</servlet-class>
      <load-on-startup>7</load-on-startup>
    </servlet>
    <servlet>
      <servlet-name>HarvesterRegistration</servlet-name>
      <servlet-class>edu.ucsb.nceas.metacat.harvesterClient.HarvesterRegistration</servlet-class>
      <load-on-startup>8</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>DataProvider</servlet-name>
        <url-pattern>/dataProvider</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
      <servlet-name>HarvesterRegistrationLogin</servlet-name>
      <url-pattern>/harvesterRegistrationLogin</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
      <servlet-name>HarvesterRegistration</servlet-name>
      <url-pattern>/harvesterRegistration</url-pattern>
    </servlet-mapping>
```

After these have been deleted and the configuration has been saved, restart the web application container (e.g., Tomcat) for the changes to take effect.
