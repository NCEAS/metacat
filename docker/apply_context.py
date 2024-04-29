#!/usr/bin/env python3
from __future__ import absolute_import, division, print_function
'''
    File name: apply_context.py
    Description: Change the default context in the metacat web.xml file
    Author: Valerie Hendrix <vchendrix@lbl.gov>
    Date created: 11/28/2017
    Python Version: 2.7
'''
import xml.etree.ElementTree as ET

import sys

# The namespace
javaee = {'javaee': 'http://java.sun.com/xml/ns/javaee'}


if __name__ == "__main__":

    # Check for args
    if len(sys.argv) < 4:
        print("Usage: {} web_xml_file old_context new_context".format(sys.argv[0]),file=sys.stderr)
        sys.exit(-1)

    web_xml_file = sys.argv[1]
    old_context = sys.argv[2]
    new_context = sys.argv[3]

    # No prefixes for the namespace
    ET.register_namespace("","http://java.sun.com/xml/ns/javaee")
    tree = ET.parse(web_xml_file)
    root = tree.getroot()

    # Iterate over the servlet-mappings and change the url pattern to the
    # new application context
    mappings = root.findall("./javaee:servlet-mapping", javaee)
    for mapping in mappings:

        servlet_name = mapping.find("javaee:servlet-name", javaee)
        url_pattern = mapping.find("javaee:url-pattern",javaee)
        if servlet_name is not None and servlet_name.text == "metacat":
            url_pattern.text = str(url_pattern.text.replace(old_context, new_context))

    # Overwrite the web xml file
    tree.write(web_xml_file)
