#!/usr/bin/env python
from __future__ import absolute_import, division, print_function
'''
    File name: apply_config.py
    Description: Apply a local set of application properties to the default metacat.properties
    Author: Valerie Hendrix <vchendrix@lbl.gov>
    Date created: 10/12/2017
    Python Version: 2.7
'''

import sys
import os
import uuid
from collections import OrderedDict


def get_properties(properties_file, include_comments=False, expand_variables=False):
    """
    Load properties file into a file into a dictionary

    :param properties_file: the properties file to parse
    :param include_comments: include the blanklines and comments
    :return:

    """

    # Check for the file existence
    if not os.path.exists(properties_file):
        print("Properties file '{}' is missing".format(properties_file), file=sys.stderr)
        sys.exit(-2)

    # Setup
    properties = OrderedDict()
    key = None
    concat_next = False # is the property value on multiple lines?

    # Open properties file and save the
    # contents to a dictionary
    with open(properties_file) as f:
        for line in f:
            # Strip line new line and spaces from the end
            line = line.strip('\n').rstrip()

            if not line.startswith("#") and line.strip():
                # This is a line with a property
                if not concat_next:
                    key, value = line.split("=", 1)
                    properties[key] = value
                else:
                    # the property is on multiple lines
                    # so, the entire line will be captured
                    value = line
                    properties[key] += '\n'
                    properties[key] += value

                concat_next = False
                if value.endswith('\\'):
                    # found a line continuation character
                    concat_next = True

                # resolve environment variables
                if expand_variables:
                    properties[key] = os.path.expandvars(properties[key])
            elif include_comments:
                # This is a blank line or a comment
                key = "#{}".format(uuid.uuid4())
                properties[key]=line

    return properties


if __name__ == "__main__":

    # Check for args
    if len(sys.argv) < 3:
        print("Usage: {} <app_properties_file> <default_properties_file>".format(sys.argv[0]),file=sys.stderr)
        sys.exit(-1)

    app_properties_file=sys.argv[1]
    metacat_properties_file = sys.argv[2]

    # Load the application and metacat properties in dictionaries
    app_properties = get_properties(app_properties_file, expand_variables=True)
    metacat_properties = get_properties(metacat_properties_file, include_comments=True)

    # Check to see if the application properties exist in the metacat properties
    difference = set(app_properties.keys()).difference(set(metacat_properties.keys()))
    if len(difference) > 0:
        print("The following properties do not exist in '{}':".format(metacat_properties_file), file=sys.stderr)
        for p in difference:
            print("\t{}".format(p), file=sys.stderr)
        sys.exit(-4)

    # Merge the application and metacat properties
    with open(metacat_properties_file,'w') as f:
        for key, value in metacat_properties.items():
            if key in app_properties.keys():
                value=app_properties[key]

            if key.startswith("#"):
                f.write("{}\n".format(value))

            else:
                f.write("{}={}\n".format(key,value))



