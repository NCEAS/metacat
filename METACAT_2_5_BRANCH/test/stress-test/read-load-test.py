#!/usr/bin/env python

import metacat
import sys

from time import time
from time import sleep

prefix = sys.argv[1]
iterations = int(sys.argv[2])
interval = int(sys.argv[3])
host = sys.argv[4]

# set debug_level=0 for no debugging.  Set it to 0 - 3 for increasing amounts of debug.
debug_level=2

run_time = str(time()).replace('.','')

output_file_name = "./read-" + prefix + ".out"
output_file = open(output_file_name, 'w')
output_file.close()

def debug (message):
	output_file = open(output_file_name, 'a')
	output_file.write(message + '\n')
	output_file.close()
	
def debug1 (message):
	if debug_level > 0:
		output_file = open(output_file_name, 'a')
		output_file.write(message + '\n')
		output_file.close()

def debug2 (message):
	if debug_level > 1:
		output_file = open(output_file_name, 'a')
		output_file.write(message + '\n')
		output_file.close()

def debug3 (message):
	if debug_level > 2:
		output_file = open(output_file_name, 'a')
		output_file.write(message + '\n')
		output_file.close()

iter_count = 0

debug("************************************************** ")
debug("Starting read-load-test for ") 
debug("       letter: $letter ") 
debug("       iterations: $iterations ")
debug("       interval: $interval ")
debug("       host: $host ")
debug("************************************************** ")

# Initialize a metacat client connection and log in as test user
t1 = time()
mc = metacat.MetacatClient(host,'/metacat/metacat')
debug("[test] -- Processing Login")
response = mc.login('test', 'test', 'NCEAS')
t2 = time()

if (response):
	debug("[test] -- SUCCESS: elapsed time: %.5f seconds" %  (t2-t1))
else:
	debug("[test] -- ERROR: could not log in")

# Insert a document
insert_template_file = open('insert.xml.tmpl', 'r')
insert_template = insert_template_file.read()
insert_template_file.close

t1 = time()
docid = 'readtest-' + prefix + run_time + '.1.1'
doc = insert_template.replace("@!docid!@", docid)
debug("[%s] -- Processing Insert" % (docid))
response = mc.insert(docid, doc)
t2 = time()

if (response.lower().find('<error>') == -1):
	debug("[%s] -- SUCCESS: elapsed time: %.5f seconds" % (docid, t2-t1))
else:
	debug("[%s] -- ERROR: %s" % (docid, response))
t2 = time()

for i in range(0,iterations):
	t1 = time()

	debug("[%s] -- Processing Read" % (docid))
	response = mc.read(docid)
	t2 = time()

	if (response.lower().find('<error>') == -1):
		debug("[%s] -- SUCCESS: elapsed time: %.5f seconds" % (docid, t2-t1))
	else:
		debug("[%s] -- ERROR: %s" % (docid, response))
		
	iter_count = iter_count + 1
	debug2("iter_count: %d, iterations: %d" % (iter_count, iterations))
			
	sleep(interval)
	
output_file.close()