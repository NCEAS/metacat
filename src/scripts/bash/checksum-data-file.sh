#!/bin/bash

directory="/var/metacat/data"
prefix="$directory/" 

rm data-checksum.txt
for file in "$directory"/*; do
    if [[  "$file" =~ ^$directory/auto ]]; then
        checksum=$(md5 "$file" | awk '{print $4}')

        # Extract the "foo.2" and "1" parts of the filename
        fullfilename=$(echo "$file" | cut -d. -f1-2)
        filename=${fullfilename#"$prefix"}
        fileversion=$(echo "$file" | cut -d. -f3)

        # Write the filename, version, and checksum to the output file
        echo "$filename $fileversion $checksum" >> data-checksum.txt
    fi
done