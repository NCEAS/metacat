#!/bin/bash

#############################################################################
# Run a series of load tests against a metacat instance. 
#
# 3 April 2009 Michael Daigle (daigle@nceas.ucsb.edu)
#############################################################################

######################
# squery-load-test.py dictionary-letter iterations delay test-server 
######################

./squery-load-test.py m 50 4 dev.nceas.ucsb.edu 2>&1 &

./squery-load-test.py n 50 4 dev.nceas.ucsb.edu 2>&1 &

./squery-load-test.py o 50 4 dev.nceas.ucsb.edu 2>&1 &

./squery-load-test.py p 50 4 dev.nceas.ucsb.edu 2>&1 &

######################
# insert-load-test.py dictionary-letter iterations delay test-server 
######################

./insert-load-test.py a 50 4 dev.nceas.ucsb.edu 2>&1 &

./insert-load-test.py b 50 4 dev.nceas.ucsb.edu 2>&1 &

./insert-load-test.py c 50 4 dev.nceas.ucsb.edu 2>&1 &

./insert-load-test.py d 50 4 dev.nceas.ucsb.edu 2>&1 &

######################
# read-load-test.py prefix iterations delay test-server 
######################

./read-load-test.py a 50 4 dev.nceas.ucsb.edu 2>&1 &

./read-load-test.py b 50 4 dev.nceas.ucsb.edu 2>&1 &

./read-load-test.py c 50 4 dev.nceas.ucsb.edu 2>&1 &

./read-load-test.py d 50 4 dev.nceas.ucsb.edu 2>&1 &

######################
# read-get-load-test.py prefix iterations delay test-server 
######################

./read-get-load-test.py a 50 4 dev.nceas.ucsb.edu 2>&1 &

./read-get-load-test.py b 50 4 dev.nceas.ucsb.edu 2>&1 &

./read-get-load-test.py c 50 4 dev.nceas.ucsb.edu 2>&1 &

./read-get-load-test.py d 50 4 dev.nceas.ucsb.edu 2>&1 &

sleep 4
echo
echo "Running the following load tests:"
ps auxwww |grep load-test.py |grep -v grep

