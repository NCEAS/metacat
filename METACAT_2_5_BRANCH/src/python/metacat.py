#!/usr/bin/python
#
#  '$RCSfile$'
#  Copyright: 2000 Regents of the University of California
#
#   '$Author$'
#     '$Date$'
# '$Revision$'
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
# TODO:
#  validate
#  getNextDocid
#  getNextRevision(docid)
#  getDocids(keyword=None)
#  query(keyword, returnfields)
#  queryDict => same as above but returns python dictionary data structure

import httplib, urllib

class MetacatClient:

    def __init__(self, server="localhost:8080", urlPath="/metacat/metacat"):
        self.metacatUrlPath = urlPath
        self.metacatServer = server
        self.sessionid = None

    def getMetacatUrl(self):
        return "http://" + self.metacatServer +  self.metacatUrlPath

    def login(self, username, password, organization=None):

        if organization == 'NCEAS':
            uid = 'uid=%s,o=NCEAS,dc=ecoinformatics,dc=org' % username
        else:
            uid = username

        postdata = { 'action'   : 'login',
                     'qformat'  : 'xml',
                     'username' : uid,
                     'password' : password }

        response = self.postRequest(postdata) 
        if response.find("<login>") != -1:
            return True
        else:
            return False

    def logout(self):
        postdata = { 'action'   : 'logout',
                     'qformat'  : 'xml'}

        response = self.postRequest(postdata) 
        if response.find("<logout>") != -1:
            return True
        else:
            return False

    def read(self, docid, qformat="xml"):
        postdata = { 'action'   : 'read',
                     'qformat'  : qformat,
                     'docid'    : docid }
        response = self.postRequest(postdata) 
        # if error node returned
        if response.find("<error>") != -1:
            return False
        else:
            return response


    def insert(self, docid, doctext):
        postdata = { 'action'   : 'insert',
                     'doctext'  : doctext,
                     'docid'    : docid }
        response = self.postRequest(postdata) 
        # if error node returned
        if response.find("<error>") != -1:
            return False
        else:
            return response

    def update(self, docid, doctext):
        postdata = { 'action'   : 'update',
                     'doctext'  : doctext,
                     'docid'    : docid }
        response = self.postRequest(postdata) 
        return response

    def delete(self, docid):
        postdata = { 'action'   : 'delete',
                     'docid'    : docid }
        response = self.postRequest(postdata) 
        return response

    def squery(self, pathquery, qformat="xml"):
        postdata = { 'action'   : 'squery',
                     'qformat'  : qformat,
                     'query'    : pathquery }
        response = self.postRequest(postdata) 
        return response

    def postRequest(self, postdata):
        conn = httplib.HTTPConnection( self.metacatServer )
        params = urllib.urlencode( postdata )
        headers = { "Content-type" : "application/x-www-form-urlencoded", 
                    "Accept"       : "*/*"}

        # If we have an active session, set the cookie
        if self.sessionid is not None:
            headers['Cookie'] = self.sessionid

        conn.request( "POST", self.metacatUrlPath, params, headers )
        response = conn.getresponse()

        # If metacat responds with a new session id,
        # register it with the metacat client instance
        setcookie = response.getheader("set-cookie", None)
        if setcookie:
            jsid = setcookie.split(';')[0]
            if jsid[:11] == "JSESSIONID=":
               self.sessionid = jsid
            
        if response.status == 200:
           content = response.read()
        else:
           print " SERVER DID NOT RETURN 'OK'.... STATUS is " + str(response.status) 
           content = ""
        conn.close()
        return content

