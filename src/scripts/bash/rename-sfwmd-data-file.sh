#!/bin/bash

directory="/var/metacat/data"

for file in "$directory"/*; do
    if [[ ! "$file" =~ ^$directory/auto ]]; then
        mv "$file" "$directory/sfwmd-${file##*/}"
    fi
done