#!/usr/bin/perl
#
#  '$RCSfile$'
#  Copyright: 2000 Regents of the University of California 
#
#   '$Author$'
#     '$Date$'
# '$Revision$' 
# 
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
#
# This is a CGI application for inserting metadata documents into
# the Metacat database.  It utilizes the Metacat.pm module for most work.
# You can specify two metacats and a list of documents. This script will
# copy documents from one metacat to another.


use lib qw(/opt/local/lib/perl5/site_perl/5.12.3);
use Metacat;

my $metacat1;
my $metacat2;
my $docid;
my $error = 0;
my $xmldoc;
my $xa;
my $response;

my $listname = "list_of_docids";

my $metacat1_url = "https://mn-stage-ucsb-1.dataone.org/metacat/metacat";
my $metacat2_url = "https://demo2.test.dataone.org/metacat/metacat";
my $username = "uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org";
my $password = "xxx";

$metacat1 = Metacat->new();
$metacat2 = Metacat->new();


if ($metacat1) {
    $metacat1->set_options( metacatUrl => $metacat1_url);
} else {
    #die "failed during metacat creation\n";
    print "Failed during metacat1 creation.";
    $error = 1;
}


# Login to metacat
print "Connecting to metacat1..........\n";
my $response1 = $metacat1->login($username, $password);
if (! $response1) {
    print $metacat1->getMessage();
    print "Failed during login: metacat1.\n";
    $error = 2;
} else {
    print "Connected to metacat1\n";
}

if ($metacat2) {
    $metacat2->set_options( metacatUrl => $metacat2_url );
} else {
    #die "failed during metacat creation\n";
    print "Failed during metacat2 creation.";
    $error = 3;
}

# Login to metacat
print "Connecting to metacat2..........\n";
my $response2 = $metacat2->login($username, $password);
if (! $response2) {
    #print $metacat->getMessage();
    #die "failed during login\n";
    print $metacat2->getMessage();
    print "Failed during login: metacat2.\n";
    $error = 4;
} else {
    print "Connected to metacat2\n";
}

if($error == 0){
    open(file,$listname) || die ("Couldn't open the file");
    while(<file>) {
	chomp();
	$xmldoc = $metacat1->read($_);
	$xa = $xmldoc->content;
	$response = $metacat2->insert($_, $xa);
	print $metacat2->getMessage();
    }
} else {
    print $error;
}
