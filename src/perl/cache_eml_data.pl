#!/usr/bin/perl

# This script queries a metacat database to locate EML documents, and for each 
# document determines if there are references to data objects.  If so, those
# references are parsed and a list is generated with metadata about each object,
# such as whether the object was directly accessible or not, how many records 
# were present, and the size of the data files.

use Metacat;
use XML::DOM;
use LWP::UserAgent;
use Cache::FileCache;
use strict;

############################################################################
#
# MAIN program block
#
############################################################################

# check that the correct number or parameters are passed from the commandline
if (($#ARGV +1) != 1) {die "Usage: %./cache_eml_data.pl <metacat_url> \n\n";}
# Get the URL to the metacat server from the command line options
my ($url) = @ARGV; 

# Initialize the data cache
my $cacheDir = "/var/metacat/cache";
my $cache = initializeCache($cacheDir);

# Open a metacat connection
my $metacat = openMetacatConnection($url);

# Get a list of EML documents
#my $queryTerm = "%Jones%";
my $queryTerm = "%";

my $result = executeQuery($metacat, $queryTerm);

# Extract an array of all of the entity URLs for each EML document
my $listRef = extractEntityUrlList($result);

# Retrieve the entities, save them in the cache,  and record metadata
my $entityMetadata = cacheEntities($cache, $listRef);

# Print out the results
#printNestedArray($entityMetadata);

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
# Execute a metacat query and return the XML resultset
#
sub executeQuery {
    my $metacat = shift;
    my $queryTerm = shift;

    my $query = "<?xml version=\"1.0\" ?> <pathquery version=\"1.2\">  <querytitle>Untitled-Search-2</querytitle>  <returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype> <returndoctype>-//NCEAS//eml-dataset-2.0//EN</returndoctype>  <returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>  <returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype><returnfield>dataTable/physical/distribution/online/url</returnfield><returnfield>dataTable/entityName</returnfield><querygroup operator=\"UNION\"><queryterm searchmode=\"contains\" casesensitive=\"false\"><value>$queryTerm</value><pathexpr>surName</pathexpr></queryterm></querygroup></pathquery>";

    my $code = $metacat->squery($query);
    
    my $result =$metacat->getMessage();

    if ($result eq "") {
        print $code, "\n";
        print "Message: ", $result, "\n";
        print ("Error or timeout from metacat...");
        exit();
    }

    return $result;
}

#
# Extract the docid and entity urls for each document in the list
#
sub extractEntityUrlList {
    my $resultset = shift;

    my $parser = new XML::DOM::Parser;
    my $node;
    my $docid;
    my $doc = $parser->parse($resultset);
    my $nodes = $doc->getElementsByTagName("docid");
    my $numberNodes = $nodes->getLength;
    my @urlList;

    # Loop through each of the documents in the resultset
    for (my $i =0; $i < $numberNodes; $i++) {
        my $node = $nodes->item($i);
        $docid =  trimwhitespace($node->getFirstChild()->getNodeValue());
    
        $node = $node->getParentNode(); 
        my $tempnodes = $node->getElementsByTagName("param");
        my $tempnumberNodes = $tempnodes->getLength;
 
        my $disturl = "";
    
        # Loop through each of the "param" elements for this document
        for (my $j =0; $j < $tempnumberNodes; $j++) {
	        my $tempnode = $tempnodes->item($j);	
	        my $paramname = $tempnode->getAttributeNode("name")->getValue();
	        if ($paramname eq "dataTable/physical/distribution/online/url") {
	            $disturl = trimwhitespace(
                        $tempnode->getFirstChild()->getNodeValue());
                push(@urlList, [$docid, $disturl]);
	        }
        }
    }
    return \@urlList;
}


#
# Remove whitespace from the start and end of the string
#
sub trimwhitespace($)
{
  my $string = shift;
  $string =~ s/^\s+//;
  $string =~ s/\s+$//;
  return $string;
}

#
# Print out a nested array of arrays
#
sub printNestedArray {
    my $listRef = shift;

    for (my $i = 0; $i <= $#{$listRef}; $i++) {
        my $innerArray = $$listRef[$i];
        printArray($innerArray);
    }
}

#
# Print an array of scalars of arbitrary length, separating values with commas
#
sub printArray {
    my $innerArray = shift;
    my $innerLength = $#{$innerArray};
    for (my $i=0; $i <= $innerLength; $i++) {
        print $$innerArray[$i];
        my $delim = ($i eq $innerLength) ? "\n" : ",";
        print $delim;
    }
}

#
# For each entity in the list, try to cache the entity after downloading it
# and return information about the size of each entity
#
sub cacheEntities {
    my $cache = shift;
    my $listRef = shift;

    my @entityMetadata;

    # Create a user agent object for downloading from URLs
    my $ua = LWP::UserAgent->new;
    $ua->agent("Metacat Harvester 1.0 ");
    $ua->timeout(600);

    # Loop through all of the entity URLs
    for (my $i = 0; $i <= $#{$listRef}; $i++) {
        my $entity;
        my $entitySize;
        my $packageId = $$listRef[$i][0];
        my $entityUrl = $$listRef[$i][1];
        if ($entityUrl =~ /^ecogrid:/) {
            #print "Need to process Ecogrid uri: ", $entityUrl, "\n";
            my $dataDir = '/var/metacat/data/';
            my $pos = length("ecogrid://knb/");
            my $entityId = substr($entityUrl, $pos);
            #print "Looking for Ecogrid file: ", $dataDir . $entityId, "\n";
            my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,$atime,
                $mtime,$ctime,$blksize,$blocks) = stat($dataDir . $entityId);
            #print "Got Ecogrid size: ", $size, "\n";
            $entity = 1;
            $entitySize = $size;
        } else {
            # For regular URLs, check if its in the cache already, and use
            # it if it is.  If not, download it and save it to the cache, but
            # only if its not an HTML file (test for <html> is simplistic)
            my $entity = $cache->get( $entityUrl );
            if ( defined $entity ) {
                if ($entity =~ /<html>/) {
                    $entity = -2;
                    $entitySize = -2;
                    $cache->remove( $entityUrl );
                } else {
                    $entitySize = length($entity);
                }
            } else {
                $entity = downloadEntity($ua, $entityUrl);
                if ($entity == -1) {
                    $entitySize = -1;
                    #print("Error on download for $entityUrl\n");
                } elsif ($entity =~ /<html>/) {
                    $entity = -2;
                    $entitySize = -2;
                } else {
                    # write the data to cache, using URL as key
                    $cache->set( $entityUrl, $entity, "never" );
                    $entitySize = length($entity);
                }
            }
        }

        # Record metadata about this entity
        my $info = [$packageId, $entityUrl, 
                ($entity < 0) ? $entity : $entitySize];
        printArray($info);
        push(@entityMetadata, $info);
    }
    return \@entityMetadata;
}

#
# Download a single entity from a given URL and return it, or return -1 on error
#
sub downloadEntity {
    my $ua = shift;
    my $url = shift;

    # Create a request
    my $req = HTTP::Request->new(GET => $url);

    # Pass request to the user agent and get a response back
    my $res = $ua->request($req);
    
    # Check the outcome of the response
    if ($res->is_success) {
        return $res->content;
    } else {
        #print $res->status_line, "\n";
        return -1;
    }
}

# 
# Create a new cache to be used for storing downloaded entities
#
sub initializeCache {
    my $cacheDir = shift;

    my $cache = new Cache::FileCache( );
    $cache->set_cache_root($cacheDir);

    return $cache;
}

