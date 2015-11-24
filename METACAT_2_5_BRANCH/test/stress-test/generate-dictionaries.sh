#!/bin/bash

#############################################################################
# Create dictionary files of five letter words with every possible combination
# of letters.  Each file is named dictionary-X.txt where X is each letter of 
# the alphabet and the file holds all possible words that start with that
# letter.
#
# 6 April 2009 Michael Daigle (daigle@nceas.ucsb.edu)
#############################################################################

ltr=(a b c d e f g h i j k l m n o p q r s t u v w x y z)

> ./dictionary2.txt

for i in {0..25}
do
	for j in {0..25}
	do
		for k in {0..25}
		do
			for l in {0..25}
			do
				for m in {0..25}
				do
					echo ${ltr[$i]}${ltr[$j]}${ltr[$k]}${ltr[$l]}${ltr[$m]} >> ./dictionary-${ltr[$i]}.txt
				done
			done
		done
	done
done