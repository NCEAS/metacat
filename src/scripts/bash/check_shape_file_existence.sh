#!/bin/bash
#This file will check if shape files exist or not.
#If one is deleted and a email will be sent to tao with the tomcat log.
#Note: if file eml_message already exists in current dir, email wouldn't send again.
#This setting will make sure only one email will sent. Otherwise, my email account will be over quota.
target_file1=/var/www/org.ecoinformatics.knb/knb/data/metacat_shps/data_points.dbf
target_file2=/var/www/org.ecoinformatics.knb/knb/data/metacat_shps/data_points.shp
target_file3=/var/www/org.ecoinformatics.knb/knb/data/metacat_shps/data_points.shx
target_file4=/var/www/org.ecoinformatics.knb/knb/data/metacat_shps/data_bounds.dbf
target_file5=/var/www/org.ecoinformatics.knb/knb/data/metacat_shps/data_bounds.shp
target_file6=/var/www/org.ecoinformatics.knb/knb/data/metacat_shps/data_bounds.shx

log_file=/var/log/tomcat/catalina.out
sender_email_address=knb_tomcat@ecoinformatics.org
receiver_email_address=tao@nceas.ucsb.edu
tmp=/root/email_message
if [ ! -e $target_file1 -o ! -e $target_file2 -o ! -e $target_file3 -o ! -e $target_file4 -o ! -e $target_file5 -o ! -e $target_file6 ]
 then
   if [ ! -e $tmp ]
     then
       touch $tmp && chmod 600 $tmp
       echo -e "subject: Shape file was deleted at `date`\n" > $tmp
       echo -e "\n log file info: " >> $tmp
       echo -e "\n `tail -300 $log_file`" >> $tmp
       /usr/sbin/sendmail -f $sender_email_address $receiver_email_address < $tmp
   fi
fi
