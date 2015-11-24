Event Logging
=============

Metacat keeps an internal log of events (such as insertions, updates, deletes, 
and reads) that can be accessed with the getlog action. Using the getlog action, 
event reports can be output from Metacat in XML format, and/or customized to 
include only certain events: events from a particular IP address, user, event 
type, or that occurred after a specified start date or before an end date. 

The following URL is used to return the basic log- an XML-formatted log of all 
events since the log was initiated::

  http://some.metacat.host/context/metacat?action=getlog 

Note that you must be logged in to Metacat using the HTTP interface or you 
will get an error message. For more information about logging in, please see 
Logging In with the HTTP Interface.

::

  <!-- Example of XML Log -->
  <?xml version="1.0"?>
  <log>
  <logEntry><entryid>44</entryid><ipAddress>34.237.20.142</ipAddress><principal>uid=jones,
  o=NCEAS,dc=ecoinformatics,dc=org</principal><docid>esa.2.1</docid><event>insert</event>
  <dateLogged>2004-09-08 19:08:18.16</dateLogged></logEntry>
  <logEntry><entryid>47</entryid><ipAddress>34.237.20.142</ipAddress><principal>uid=jones,o=NCEAS,
  dc=ecoinformatics,dc=org</principal><docid>esa.3.1</docid><event>insert</event><dateLogged>2004-
  09-14 19:50:40.61</dateLogged></logEntry>
  </log>

The basic log can be quite extensive. To subset the report, restrict the 
matching events using parameters. Query parameters can be combined to further 
restrict the report.

+-----------+-----------------------------------------------------+
| Parameter | Description and Values                              |
+===========+=====================================================+
| ipAddress | Restrict the report to this IP Address (repeatable) |
+-----------+-----------------------------------------------------+
| principal | Restrict the report to this user (repeatable)       |
+-----------+-----------------------------------------------------+
| docid     | Restrict the report to this docid (repeatable)      |
+-----------+-----------------------------------------------------+
| event     | Restrict the report to this event type (repeatable) |
|           | Values: insert, update, delete, read                |
+-----------+-----------------------------------------------------+
| start     | Restrict the report to events after this date       |
|           | Value: YYYY-MM-DD+hh:mm:ss                          |
+-----------+-----------------------------------------------------+
| end       | Restrict the report to events before this date.     |
|           | Value: YYYY-MM-DD+hh:mm:ss                          |
+-----------+-----------------------------------------------------+

To view only the 'read' events, use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&event=read


To view only the events for a particular IP address, use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&ipaddress=107.9.1.31


To view only the events for a given user, use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&principal=uid=johndoe,o=NCEAS,dc=ecoinformatics,dc=org 


To view only the events for a particular document, use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&docid=knb.5.1 


To view only the events after a given date, use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&start=2004-09-15+12:00:00


To view only the events before a given date, use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&end=2004-09-15+12:00:00


To view the 'insert' events for September 2004 (i.e., to combine parameters) use a URL like::

  http://some.metacat.host/context/metacat?action=getlog&event=insert&start=2004-09-01+12:00:00&end=2004-09-30+23:59:59 

