#!/bin/bash
#Call a java client class to query metacat automatically and metacat server
#will record the performance data into a file. Since we need restart postgres,
#this script should be run as root user
#Note: before running the script, user should run both "ant jar" and "ant clientjar"
#in Metacat to get metaca-client.jar and metacat.jar in build directory. 

#variables
JUNIT=/usr/local/devtools/apache-ant/lib/junit-4.3.1.jar
METACAT=../build/metacat.jar
METACAT_CLIENT=../build/metacat-client.jar
UTILITIES=../lib/utilities.jar
METACATURL=http://chico.dyndns.org:8081/metacat/metacat
QUERY=../test/performance_measure_query
TIME=2
TOMCATUSER=tao

#copy the java client to build directory
rm -rf ../build/tests/
mkdir ../build/tests
mkdir ../build/tests/edu
mkdir ../build/tests/edu/ucsb
mkdir ../build/tests/edu/ucsb/nceas
mkdir ../build/tests/edu/ucsb/nceas/metacattest
cp ../test/edu/ucsb/nceas/metacattest/MetaCatQueryPerformanceTest.java ../build/tests/edu/ucsb/nceas/metacattest/.

#compile the class
javac -classpath $JUNIT:$METACAT:$METACAT_CLIENT:$UTILITIES ../build/tests/edu/ucsb/nceas/metacattest/MetaCatQueryPerformanceTest.java 




#Iterate to restart postgres and tomcat, and call java client class
for (( i = 0; i < $TIME; i++))
do
    #retart postgres
    /etc/init.d/postgresql-8.2 restart
    #Check if posgres restart successfully
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
    echo "success value for restarting postgres is $successPQL"

    #stop tomcat
    su - $TOMCATUSER /usr/local/devtools/apache-tomcat-5.5.23/bin/shutdown.sh
     #Check if tomcat start successfully
    successStop=false;
    tomcatRunning=false
    while [ $successStop = false ]
    do
      #if found netstat command still has tcomat lisenter (port 8005) ,
      #reset the tomcatRunning value to true
      for fn in `netstat -plt | grep 8005`
      do
        #reset success value
        tomcatRunning=true;
      done
      if [ $tomcatRunning = true ]
         then
           successStop=false
         else
           successStop=true
      fi
    done
    echo "success value for stoping tomcat is $successStop"
     
    #start tomcat
   su - $TOMCATUSER  /usr/local/devtools/apache-tomcat-5.5.23/bin/startup.sh
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
    java -cp $JUNIT:$METACAT:$METACAT_CLIENT:$UTILITIES:../build/tests edu.ucsb.nceas.metacattest.MetaCatQueryPerformanceTest $METACATURL $QUERY
    echo "Successfully query the metacat"
done
