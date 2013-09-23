#!/bin/bash
#Call a java client class to query metacat automatically and metacat server
#will record the performance data into a file.  This script will be put in /etc/init.d directory
#and called in system booting. After script is down, it will call reboot system too.
#So every time to excecute the quyer, memory will be in fresh.
#Note: before running the script, user should run both "ant jar" and "ant clientjar"
#in Metacat to get metaca-client.jar and metacat.jar in build directory. 

#variables
JUNIT=/usr/local/devtools/apache-ant/lib/junit-4.3.1.jar
PROJECT=/home/tao/project/metacat
METACATURL=http://chico.dyndns.org:8081/metacat/metacat
TOMCATUSER=tao


QUERY=$PROJECT/test/performance_measure_query
TIMEFILE=$PROJECT/test/times
METACAT=$PROJECT/build/metacat.jar
METACAT_CLIENT=$PROJECT/build/metacat-client.jar
UTILITIES=$PROJECT/lib/utilities.jar

#copy the java client to build directory
rm -rf $PROJECT/build/tests/
mkdir -p $PROJECT/build/tests/edu/ucsb/nceas/metacattest
cp $PROJECT/test/edu/ucsb/nceas/metacattest/MetaCatQueryPerformanceTest.java $PROJECT/build/tests/edu/ucsb/nceas/metacattest/.

#compile the class
javac -classpath $JUNIT:$METACAT:$METACAT_CLIENT:$UTILITIES $PROJECT/build/tests/edu/ucsb/nceas/metacattest/MetaCatQueryPerformanceTest.java 


case "$1" in
  start)
    echo "Starting running query script"
    #Iterate to restart postgres and tomcat, and call java client class
    TIMES=`cat $TIMEFILE`
    echo "The value from TIMEFILE is $TIMES" 
    #if times greater than 0, it will query the metacat
    if [ $TIMES -gt 0 ]
      then
      #Check if postgres start successfully
      successPQL=false;
      while [ $successPQL = false ]
         do
             #if found netstat command has posgres lisenter is ready,
             #reset the success value to jump the while loop.
             for fn in `netstat -plt | grep postgres`
               do
                 #reset success value
                 successPQL=true
            done
      done
      echo "success value for starting postgres is $successPQL"
      #Check if tomcat start successfully
      successStart=false;
      while [ $successStart = false ]
        do
           #if found netstat command has tcomat lisenter (port 8005) is ready,
           #reset the success value to jump the while loop.
           for fn in `netstat -plt | grep 8005`
             do
                #reset success value
                successStart=true
           done
      done
     echo "success value for starting tomcat is $successStart"
     #run the class -- query the remote metacat
      java -cp $JUNIT:$METACAT:$METACAT_CLIENT:$UTILITIES:$PROJECT/build/tests edu.ucsb.nceas.metacattest.MetaCatQueryPerformanceTest $METACATURL $QUERY
      echo "Successfully query the metacat"
      #Drecease 1 from value of TIMES
      TIMES=`expr $TIMES - 1`
      echo "The new TIMES value  is $TIMES"
      #write the new TIMES to the file
      echo $TIMES >$TIMEFILE
      #reboot machine
      reboot
    fi
   ;;
 stop)
    echo "Stopping running query script - do nothing"
     ;;
  *)
    echo "Usage: /etc/init.d/blah {start|stop}"
    exit 1
    ;;
esac

exit 0
 
