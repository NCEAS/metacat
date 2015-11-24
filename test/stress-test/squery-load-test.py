#!/usr/bin/env python

import metacat
import sys

from time import time
from time import sleep

letter = sys.argv[1]
iterations = int(sys.argv[2])
interval = int(sys.argv[3])
host = sys.argv[4]

# set debug_level=0 for no debugging.  Set it to 0 - 3 for increasing amounts of debug.
debug_level=2

output_file_name = "./squery-" + letter + ".out"
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

dictionary_file_name = './dictionary-' + letter + '.txt'
dictionary_file = open(dictionary_file_name, 'r')

iter_count = 0

debug("************************************************** ")
debug("Starting squery-load-test for ") 
debug("       letter: $letter ") 
debug("       iterations: $iterations ")
debug("       interval: $interval ")
debug("       host: $host ")
debug("************************************************** ")

mc = metacat.MetacatClient(host,'/metacat/metacat')

query_template_file = open('squery.xml.tmpl', 'r')
query_template = query_template_file.read()
query_template_file.close

for line in dictionary_file:
	word = line.strip()
	debug("[%s] -- Processing Search" % (word))
	query = query_template.replace("@!search-word!@", word)
	t1 = time()
	response = mc.squery(query)
	t2 = time()

	if response.find("<resultset>") != -1:
		debug("[%s] -- SUCCESS: elapsed time: %.5f seconds" % (word, t2-t1))
	else:
		debug("[%s] -- ERROR: %s " % (word, response))
			
	iter_count = iter_count + 1
	debug2("iter_count: %d, iterations: %d" % (iter_count, iterations))
	if iter_count >= iterations:		
		sys.exit()
			
	sleep(interval)
	
dictionary_file.close()
output_file.close()

