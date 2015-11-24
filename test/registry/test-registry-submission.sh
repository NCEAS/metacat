#!/bin/sh

#  test-registry-submission.sh
#  
#
#  Created by Lauren Walker on 7/23/14.
#
# Replace the Cookie values or point to a file

> fileDetails.txt
> response.txt

counter="0"

while [ $counter -lt 5 ]
do
    curl -X POST --cookie "JSESSIONID=XXX;CGISESSID=XXX" --form file_0=@testdata.csv --form cfg=metacatui --form stage=insert --form providerGivenName=Lauren --form providerSurName=Walker --form "title=test registry with curl $counter" --form site=NCEAS --form origNamefirst0=walker --form origNamelast0=Walker --form abstract=abstract --form beginningYear=2014 --form geogdesc=Cali --form latDeg1=0 --form longDeg1=0 --form dataMedium=digital --form "useConstraints=no restrictions" --form useOrigAddress=on --form scope=test --form fileCount=1 "https://dev.nceas.ucsb.edu/knb/cgi-bin/register-dataset.cgi" -o response.txt

    upload=$(xmlstarlet sel -t -v "//input[@name='upload_0']/@value" response.txt)
    uploadname=$(xmlstarlet sel -t -v "//input[@name='uploadname_0']/@value" response.txt)
    uploadtype=$(xmlstarlet sel -t -v "//input[@name='uploadtype_0']/@value" response.txt)

    echo "$upload,$uploadname,$uploadtype," >> fileDetails.txt

    counter=$[$counter+1]
done

counter="0"

while [ $counter -lt 5 ]
do
    details=$(tail -n+$counter fileDetails.txt | head -n1)

    #Get the variables needed for the confirmation stage
    commaLoc=`gexpr index $details ","`
    upload=${details:0:commaLoc-1}

    details=${details:commaLoc}
    commaLoc=`gexpr index $details ","`
    uploadname=${details:0:commaLoc-1}

    details=${details:commaLoc}
    commaLoc=`gexpr index $details ","`
    uploadtype=${details:0:commaLoc-1}

    curl -X POST --cookie "JSESSIONID=XXX;CGISESSID=XXX" --form file_0=@testdata.csv --form cfg=metacatui --form stage=confirmed --form providerGivenName=Lauren --form providerSurName=Walker --form "title=test registry with curl $counter" --form site=NCEAS --form origNamefirst0=walker --form origNamelast0=Walker --form abstract=abstract --form beginningYear=2014 --form geogdesc=Cali --form latDeg1=0 --form longDeg1=0 --form dataMedium=digital --form "useConstraints=no restrictions" --form useOrigAddress=on --form scope=test --form fileCount=1 --form upCount=1 --form delCount=1 --form uploadperm_0=public --form upload_0=$upload --form uploadname_0=$uploadname --form uploadtype_0=$uploadtype "https://dev.nceas.ucsb.edu/knb/cgi-bin/register-dataset.cgi" &

    counter=$[$counter+1]
done

