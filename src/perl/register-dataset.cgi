#!/bin/sh

#  test-registry-submission.sh
#  
#
#  Created by Lauren Walker on 7/23/14.
#

> fileDetails.txt
> results.html

counter="0"

while [ $counter -lt 3 ]
do
    response=$(curl -X POST --cookie "JSESSIONID=81E77F52A2CA1D632B6CF01925D54FE0;SESS6790668dc29fdba8b64e6f4c1193c83d=yRZg3Ap9kK1TJGef_oOsPu4nMUe_SMEB8ICQrpsuOHI;CGISESSID=5c43858469558146ec0439e5b20500fd" --form file_0=@testdata.csv --form cfg=metacatui --form stage=insert --form providerGivenName=Lauren --form providerSurName=Walker --form "title=test with curl $counter" --form site=NCEAS --form origNamefirst0=walker --form origNamelast0=Walker --form abstract=abstract --form beginningYear=2014 --form geogdesc=Cali --form latDeg1=0 --form longDeg1=0 --form dataMedium=digital --form "useConstraints=no restrictions" --form useOrigAddress=on --form fileCount=1 --form justGetUploadDetails=true "https://dev.nceas.ucsb.edu/knb/cgi-bin/register-dataset.cgi")

    echo $response >> fileDetails.txt
    counter=$[$counter+1]
done

counter="0"

while [ $counter -lt 3 ]
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

    curl -X POST --cookie "JSESSIONID=81E77F52A2CA1D632B6CF01925D54FE0;SESS6790668dc29fdba8b64e6f4c1193c83d=yRZg3Ap9kK1TJGef_oOsPu4nMUe_SMEB8ICQrpsuOHI;CGISESSID=5c43858469558146ec0439e5b20500fd" --form file_0=@testdata.csv --form cfg=metacatui --form stage=confirmed --form providerGivenName=Lauren --form providerSurName=Walker --form "title=test with curl $counter" --form site=NCEAS --form origNamefirst0=walker --form origNamelast0=Walker --form abstract=abstract --form beginningYear=2014 --form geogdesc=Cali --form latDeg1=0 --form longDeg1=0 --form dataMedium=digital --form "useConstraints=no restrictions" --form useOrigAddress=on --form fileCount=1 --form upCount=1 --form delCount=1 --form uploadperm_0=public --form upload_0=$upload --form uploadname_0=$uploadname --form uploadtype_0=$uploadtype "https://dev.nceas.ucsb.edu/knb/cgi-bin/register-dataset.cgi" &

    counter=$[$counter+1]
done

