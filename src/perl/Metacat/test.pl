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

# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl test.pl'

######################### We start with some black magic to print on failure.

# Change 1..1 below to 1..last_test_to_print .
# (It may become useful if the test is moved to ./t subdirectory.)

BEGIN { $| = 1; print "1..1\n"; }
END {print "not ok 1\n" unless $loaded;}
use Metacat;
$loaded = 1;
print "ok 1\n";

######################### End of black magic.

# Insert your test code below (better if it prints "ok 13"
# (correspondingly "not ok 13") depending on the success of chunk 13
# of the test code):
my $metacatUrl = "http://snow.joneseckert.org:8080/metacat/metacat";
my $username = 'uid=jones,o=NCEAS,dc=ecoinformatics,dc=org';
my $password = 'your-pw-goes-here';

# Set up a date stamp
my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
if ($min < 10) {
   $minstr = "0".$min;
} else {
   $minstr = $min;
}
if ($sec < 10) {
   $secstr = "0".$sec;
} else {
   $secstr = $sec;
}
my $thismon=(Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec)[$mon];
my $timenow="$mday-$thismon-$year $hour:$minstr:$secstr";

# Set up a document accession number to use
my $scope = "$thismon$mday";
my $docroot = "$scope.$minstr.";
my $docrev = 1;

# Chunk 2: Test metacat object creation
my $metacat = Metacat->new();
if ($metacat) {
  $metacat->set_options( metacatUrl => $metacatUrl );
  print "ok 2 metacat creation\n";
} else {
  print "not ok 2 metacat creation\n";
}

# Chunk 3: Test metacat login
my $response = $metacat->login($username, $password);
if ($response) {
  print "ok 3 login\n";
} else {
  print $metacat->getMessage();
  print "not ok 3 login\n";
}

# Chunk 4: Test metacat insert
my $xmldoc = "<?xml version=\"1.0\"?><moviedb><movie>Scream</movie></moviedb>";
my $response = $metacat->insert($docroot . $docrev, $xmldoc);
if ($response) {
  print "ok 4 insert\n";
} else {
  print $metacat->getMessage();
  print "not ok 4 insert\n";
}

# Chunk 5: Test metacat update
$xmldoc = "<?xml version=\"1.0\"?><moviedb><movie>Scream 2</movie></moviedb>";
$docrev++;
my $response = $metacat->update("$docroot" . "$docrev", $xmldoc);
if ($response) {
  print "ok 5 update\n";
} else {
  print $metacat->getMessage();
  print "not ok 5 update\n";
}

# Chunk 6: Test metacat read
my $response = $metacat->read("$docroot" . "$docrev");
if ($response) {
  print "ok 6 read\n";
} else {
  print $metacat->getMessage();
  print "not ok 6 read\n";
}

# Chunk 7: Test metacat squery
my $pathquery = "<?xml version=\"1.0\"?><pathquery version=\"1.1\"><querygroup operator=\"UNION\"><queryterm searchmode=\"contains\" casesensitive=\"false\"><value>Scream</value></queryterm></querygroup></pathquery>";
my $response = $metacat->squery($pathquery);
if ($response) {
  print "ok 7 squery\n";
} else {
  print $metacat->getMessage();
  print "not ok 7 squery\n";
}

# Chunk 8: Test metacat delete
my $response = $metacat->delete("$docroot" . "$docrev");
if ($response) {
  print "ok 8 delete\n";
} else {
  print $metacat->getMessage();
  print "not ok 8 delete\n";
}

# Chunk 9: Test metacat getlastid
my $response = $metacat->getLastId("$scope");
if ($response) {
  print "ok 9 getlastid ($response)\n";
} else {
  print $metacat->getMessage();
  print "not ok 9 getlastid\n";
}
