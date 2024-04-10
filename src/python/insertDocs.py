"""
 Inserts a list of documents into a Metacat server
"""

import metacat
import sys

try:
    docidlist = sys.argv[1]
    docdir = sys.argv[2]
except:
    print
    print " usage: python insertDocs.py docidlist.txt docdir"
    print "        where docidlist.txt is a line-delimited list of docids "
    print "        and docdir is the path to the directory of documents"
    print
    sys.exit(1)

print "Logging into destination server"
dest = metacat.MetacatClient('localhost:8080','/metacat/metacat')
dest.login('username','password','NCEAS')

print "Open docid list and loop"
for docid in open(docidlist,'r').readlines():
    docid = docid.strip().replace('\n','')
    if docid != '' and docid is not None:
       print " reading docid " + docid

       with open(docdir + "/" + docid) as f:
           eml = f.read()
           f.close()

       if eml:
           print " inserting docid " + docid
           response = dest.insert(docid, eml)
           if not response:
              print "   insert failed"
       else:
           print "   docid " + docid + " not found"

print "Logging out"
dest.logout()
