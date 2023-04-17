#!/bin/bash

directory="/var/metacat/documents"
prefix="$directory/"

rm document-checksum.txt
for file in "$directory"/*; do
        checksum=$(md5 "$file" | awk '{print $4}')

        # Extract the "foo.2" and "1" parts of the filename
        fullfilename=$(echo "$file" | cut -d. -f1-2)
        filename=${fullfilename#"$prefix"}
        fileversion=$(echo "$file" | cut -d. -f3)

        # Write the filename, version, and checksum to the output file
        echo "$filename $fileversion $checksum" >> document-checksum.txt
done