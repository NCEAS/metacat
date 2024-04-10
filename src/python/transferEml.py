#!/usr/bin/python
"""
 Transfers list of documents from one metacat server to another
"""

import metacat
import sys

try:
    docidlist = sys.argv[1]
except:
    print
    print " usage: python transferEml.py docidlist.txt "
    print "        where docidlist.txt is a line-delimited list of docids "
    print
    sys.exit(1)

print "Logging into destination server"
dest = metacat.MetacatClient('yourserver:8180','/metacat/metacat')
dest.login('user','password','NCEAS')

print "Logging into source server"
src = metacat.MetacatClient('knb.ecoinformatics.org', '/knb/metacat')
src.login('user','password','NCEAS')

print "Open docid list and loop"
for docid in open(docidlist,'r').readlines():
    docid = docid.strip().replace('\n','')
    if docid != '' and docid is not None:
       print " reading docid " + docid
       eml = src.read(docid) 
       if eml:
           print " inserting docid " + docid
           response = dest.insert(docid + ".1" ,eml)
           if not response:
              print "   insert failed"
       else:
           print "   docid " + docid + " not found"

print "Logging out"
dest.logout()
src.logout()
       
        


