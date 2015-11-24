#!/usr/bin/perl
#
# Basic LSID resolution client for quick and dirty testing
# Matt Jones  07 December 2005
#
# '$Id$'

use strict;
use SOAP::Lite;
use SOAP::MIME;
use MIME::Entity;

# Default LSID to resolve
my $lsid = "urn:lsid:joneseckert.org:jones-test:1:1";
#my $lsid = "urn:lsid:limnology.wisc.edu:dataset:ntlfi02";

# I have hardcoded the endpoint proxy for this service, but in fact it
# should be first determined using a DNS SRV record lookup on the authority,
# and then parsed out of the WSDL from the getAvailableServices() call
my $endpoint = "http://snow.joneseckert.org:8080/authority/";
#my $endpoint = "http://lsid.limnology.wisc.edu:8080/authority/services/AuthorityWebService";

&parseArgs;

print "\nResolving LSID: $lsid\n";
print "Using Endpoint: $endpoint\n\n";

# Namespace constants
my $AUTH_SERVICE_NS="http://www.omg.org/LSID/2003/AuthorityServiceSOAPBindings";
my $DATA_SERVICE_NS="http://www.omg.org/LSID/2003/DataServiceSOAPBindings";

# First get the WSDL for available services for this LSID
callLsidOperation($AUTH_SERVICE_NS, $endpoint, $lsid, 'getAvailableServices');

# Second call the getMetadata operation for this LSID
callLsidOperation($DATA_SERVICE_NS, $endpoint . 'metadata/', $lsid, 
        'getMetadata');

# Third call the getData operation for this LSID
callLsidOperation($DATA_SERVICE_NS, $endpoint . 'data/', $lsid, 'getData');

#
# Subroutine to make the SOAP call to the LSID resolver
#    TODO: Assumes parameters passed in are valid, need to check
#
sub callLsidOperation {
    my $namespace = shift;
    my $endpoint = shift;
    my $lsid = shift;
    my $method = shift;

    my $service = SOAP::Lite
        -> uri($DATA_SERVICE_NS)
        -> proxy($endpoint);
    my $response = $service->call($method => SOAP::Data->name(lsid => "$lsid"));
    if ($response->fault) { 
        print "DETAIL: ", $response->faultdetail, "\n";
        print "  CODE: ", $response->faultcode, "\n";
        print "STRING: ", $response->faultstring, "\n"; 
        print " ACTOR: ", $response->faultactor, "\n";
    } else {
        print "\n";
        print "************************************************************\n";
        print "*   Results of $method\n";
        print "************************************************************\n";
        print "SOAP Body says type is: ", $response->result, "\n";
        foreach my $part (@{$response->parts}) {
            my $type = $$part->mime_type;
            print "MIME Envelope says type is: ", $type, "\n";
            print "Attachment payload is: ", "\n";
            if (my $io = $$part->open("r")) {
                while (defined($_ = $io->getline)) { 
                    print $_;
                }
                $io->close;
            }
            print "\n\n";
        }
    }
}

# check the commandline for LSIDs to be resolved and the endpoint
# if no arguments are found, the deault lsid is used
sub parseArgs {
    foreach my $arg (@ARGV) {
        if ($arg =~ /^urn:lsid:/i) {
            $lsid = $arg;
        } else {
            $endpoint = $arg;
        }
    }
}
