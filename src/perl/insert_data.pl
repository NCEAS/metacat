#!/usr/bin/perl

# This script is INCOMPLETE -- see TODO markers throughout

# This script loops through a list of metadata documents and associated data
# files, and for each one uploads the data file to metacat, puts the accession 
# number in the metadata, then inserts the metadata to metacat.
# Matt Jones February 14, 2008

use Metacat;
use XML::DOM;
use strict;

############################################################################
#
# MAIN program block
#
############################################################################

# check that the correct number or parameters are passed from the commandline
if (($#ARGV +1) != 1) {die "Usage: %./insert_fgdc.pl <metacat_url> \n\n";}
# Get the URL to the metacat server from the command line options
my ($url) = @ARGV; 
my $dn = 'uid=jones,o=NCEAS,dc=ecoinformatics,dc=org';
my $password = 'foobar';

# Open a metacat connection and login
my $metacat = openMetacatConnection($url);
loginToMetacat($metacat, $dn, $password);

# Read in the list of FGDC documents
my $dataDir = "/tmp/data";
# datalist key = data filename, value = metadata filename
my $datalist = getDataList($dataDir); 
my $datafilename;

# Loop through the list of datafiles
foreach $datafilename (keys %$datalist) {
    print("Processing...  $datafilename $$datalist{$datafilename}\n");

    # Upload the data file into Metacat, catching accession # errors
    my $dataId = uploadData($metacat, $datafilename);

    # Reference the accession # URL for the data file in the FGDC metadata
    my $metadata = createDataReference($dataId, $$datalist{$datafilename});

    # Insert the metadata file into Metacat, catching accession # errors
    my $metadataId = insertMetadata($metacat, $metadata );

    # Set access control rules for the metadata and data files
    setAccess($dataId);
    setAccess($metadataId);
}

exit(0);

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
# Login to the metacat server
#
sub loginToMetacat {
    my $metacat = shift;
    my $dn = shift;
    my $password = shift;

    my $response1 = $metacat->login($dn, $password);
    if (! $response1) {
        print $metacat->getMessage();
        print "Failed during login: metacat.\n";
    } else {
        print "Connected to metacat\n";
    }
}

#
# TODO
# Get a hashed list of datafile names and their associated metadata file names
#
sub getDataList {
    my $dataDir = shift;

    my %dataList = ("datafile_a"=>"metadatafile_ma", 
                    "datafile_b"=>"metadatafile_mb");

    return \%dataList;
}

#
# TODO
# Upload a data file to metacat, returning the identifier used
#
sub uploadData {
    my $metacat = shift;
    my $datafilename = shift;

    # TODO: Create a unique ID
    my $identifier;

    # TODO: Read in the data file from disk
    # my $data;

    # TODO: Do the metacat insertion as a MIME insertion
    #my $response = $metacat->upload($identifier, $data);

    # TODO: Check the insertion succeeded, if not possibly try again with new id
	my $message =  $metacat->getMessage();

    return $identifier;

    return $identifier;
}

#
# TODO
# Insert a reference to a data file into the FGDC metadata document
#
sub createDataReference {
    my $metacat = shift;
    my $dataId = shift;
    my $metadatafilename = shift;

    my $metadata;

    return $metadata;
}

#
# TODO
# Insert the metadata document into the Metacat database
#
sub insertMetadata {
    my $metacat = shift;
    my $metadatafilename = shift;

    # TODO: Create a unique ID
    my $identifier;

    # TODO: Read in the metadata file from disk
    # my $metadata;

    # Do the metacat insertion
    # my $response = $metacat->insert($identifier, $metadata);

    # TODO: Check the insertion succeeded, if not possibly try again with new id
	my $message =  $metacat->getMessage();

    return $identifier;
}

#
# TODO
# Set access control permissions on the file identified
#
sub setAccess {
    my $metacat = shift;
    my $identifier;

}
