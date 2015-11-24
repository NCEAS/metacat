#!/usr/bin/env perl
# eml_get_objectnames.pl
# Given a list of EML docids and dataids, generate a list of objectName elements
# present in the documents mapped to each dataid, and generate UPDATE SQL statements.
#
# This information is useful because both Morpho and the Metacat Registry historically 
# stored the dataid i n the 'docname' field within xml_documents.
use Metacat;
use Data::Dumper;
use XML::LibXML;
use LWP::UserAgent;
use strict;

############################################################################
#
# MAIN program block
#
############################################################################

# check that the correct number or parameters are passed from the commandline
if (($#ARGV + 1) < 1) {die "Usage: %./eml_get_objectnames.pl <metacat_url>\n\n";}
# Get the URL to the metacat server from the command line options
my $url = @ARGV; 

# Open a metacat connection
my $metacat = openMetacatConnection($url);

my @errorMessages;
my $error;

# requires an input CSV file containing two columns:
# the dataid and docid, which can be extracted from the database:
#   SELECT nodedata, docid FROM xml_nodes WHERE nodetype = 'TEXT' AND nodedata LIKE 'ecogrid%'
# and then trimming the ecogrid prefix from the dataid fields.

my $nameFile = 'docid-for-binaries.txt'; 

unless (open (FILE_LIST, $nameFile)) {
    print "file with docids required.\n";
    exit;
}

while (my $line = <FILE_LIST>) {
    my ($dataid, $docid) = split(',', trim($line));

    my $response = $metacat->read($docid);
    my $metadata = $response->content();

    # Now parse the metadata document, grabbing the objectName for the particular docid
    my $doc = getEMLDoc($metadata);
    my $objName = getObjectName($doc, $dataid);
    if ($objName ne $dataid && $objName ne "" && $objName !~ /deleteme/) {
        my $id = $dataid;
        $id =~ s/\.[0-9]+$//;
        print "UPDATE xml_documents SET docname = '$objName' WHERE docid = '$id';\n";
    }
}

exit;

############################################################################
#
# SUBROUTINES
#
############################################################################

#
# Create a connection to the metacat server
#
sub openMetacatConnection {
    my $url = shift;

    my $metacat = Metacat->new();
    if ($metacat) {
        $metacat->set_options( metacatUrl => $url );
    } else {
        die("Could not open connection to Metacat url: $url\n");
    }
    return $metacat;
}

#
# Retrieve EML documents and set up XML parser object
#
sub getEMLDoc {
    my $resultset = shift;

    my $parser = XML::LibXML->new();
    my $node;
    my $docid;
    my $doc = $parser->parse_string($resultset);
    if ($doc eq "") {
        $error ="Error in parsing the eml document";
        push(@errorMessages, $error);
    } elsif ($doc=~ /<error/) {
        if ($doc=~ /public/) {
            $error ="Error in reading the eml document. Please check if you are logged in.";
            push(@errorMessages, $error);
        } else {
          $error ="Error in reading the eml document. Please check if you have access to read the document";
          push(@errorMessages, $error);
        }
    } else {
        my $findType = $doc->findnodes('//dataset/identifier');
        if ($findType->size() > 0) {
            # This is a eml beta6 document
            # Read the documents mentioned in triples also
            push(@errorMessages, "EML2 beta6 support deprecated.");
        }
    }
    return $doc;
}

#
# Inspect an EML document for objectName elements, return the name if it differs from docid
#
sub getObjectName {
    my $doc = shift;
    my $docid = shift;

    # the five types of physical objects, though only dataTable and otherEntity appear to be used
    my @names = qw(dataTable otherEntity spatialRaster spatialVector storedProcedure);
    my $results;
    my $results_urls;
    my $dataid;
    my $objectName;
    my $offset = 0;

    foreach my $name (@names) {
        $results = $doc->findnodes("//$name/physical/objectName");
        $results_urls = $doc->findnodes("//$name/physical/distribution/online/url");
        foreach my $node ($results_urls->get_nodelist) {
            $offset++;
            $dataid = $node->textContent();
            $dataid =~ s/ecogrid:\/\/knb\///;
            # if the docid == docname, return the objectName
            if ($dataid eq $docid) {
                my $nameNode = $results->get_node($offset);
                if ($nameNode) {
                    $objectName = $nameNode->textContent();
                }
            }
        }
    }
    return $objectName;
}

#
# Remove whitespace from the start and end of the string
#
sub trim($)
{
  my $string = shift;
  $string =~ s/^\s+//;
  $string =~ s/\s+$//;
  return $string;
}
