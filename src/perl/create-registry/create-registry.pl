
#!/usr/bin/perl
#
# This is a CGI application for inserting metadata documents into
# the Metacat database.  It utilizes the Metacat.pm module for most work.
# You can specify two metacats and a list of documents. This script will
# copy documents from one metacat to another.

# read variables from registry.cfg

use File::Copy;
use IO::File;
use strict;

my $scope     = readReqdParam("Enter the scope name:");
my $org       = readReqdParam("Enter the organization name:");
my $orgabbrev = readReqdParam("Enter the organization abbreviation:");
my $orgurl    = readReqdParam("Enter the organization URL:");

my $username  = readParam("Enter the username:", "uid=".$scope."admin,o=".$orgabbrev.",dc=ecoinformatics,dc=org");
my $password  = readParam("Enter the password:", "");
my $mailhost  = readParam("Enter the mailhost name:", "hyperion.nceas.ucsb.edu");
my $adminName  = readParam("Enter the Administrator's name:", "adminName");
my $adminEmail  = readParam("Enter the administrator's email address:", "email\@admin");
my $senderEmail  = readParam("Enter the sender's email address:", "registry\@$org");
my $debugLevel  = readParam("Enter the debug level:", "0");

my $hasKeyword  = readParam("Do you want to add keywords to this skin [Yes/No]:", "Yes");
my $hasMethod  = readParam("Do you want to add methods to this skin [Yes/No]:", "Yes");
my $hasSpatial  = readParam("Do you want to add spatial coverage to this skin [Yes/No]:", "Yes");
my $hasTemporal  = readParam("Do you want to add temporal coverage in this skin [Yes/No]:", "Yes");
my $hasTaxonomic  = readParam("Do you want to see taxonomic coverage in this skin [Yes/No]:", "Yes");


mkdir "../../../lib/style/skins/$scope", 0744;

writeConfigFile();
copyFile('searchform.html');
copyFile('index.html');
copyFile('header.html');
copyAndRenameFile('xml');
copyAndRenameFile('css');
copyAndRenameFile('js');

sub readParam{
    my $printString = shift;
    my $defaultValue = shift;

    print "$printString [$defaultValue]\n";
    my $returnVal = <>;   
    chomp $returnVal;
    
    if($returnVal eq ""){
	$returnVal = $defaultValue;
    }
 
    return $returnVal;
}


sub readReqdParam{
    my $printString = shift;

    print "$printString\n";
    my $returnVal = <>;   
    chomp $returnVal;
    
    while($returnVal eq ""){
	print "This value is required. $printString\n";
	$returnVal = <>;
	chomp $returnVal;
    }
    
    return $returnVal;
}

sub writeConfigFile{

    my $configText = "";

    $configText .= "#"."\n";
    $configText .= "# General configuration parameters"."\n";
    $configText .= "#"."\n";
    $configText .= "metacatUrl = http://\@server\@\@servlet-path\@"."\n";
    $configText .= "username = ".$username."\n";
    $configText .= "password = ".$password."\n";
    $configText .= "ldapUrl = \@ldapUrl\@"."\n";
    $configText .= "defaultScope = $scope"."\n";
    $configText .= "organization = $org"."\n";
    $configText .= "orgabbrev = $orgabbrev"."\n";
    $configText .= "orgurl = $orgurl"."\n";
    $configText .= "responseTemplate = \@responseForm\@"."\n";
    $configText .= "entryFormTemplate = \@entryForm\@"."\n";
    $configText .= "guideTemplate = \@guide\@"."\n";
    $configText .= "confirmDataTemplate = \@confirmData\@"."\n";
    $configText .= "deleteDataTemplate = \@deleteData\@"."\n";
    
    if($hasKeyword ne "Yes" && $hasKeyword ne "yes"){
	$configText .= "hasKeyword = false"."\n";
    }

    if($hasMethod ne "Yes" && $hasMethod ne "yes"){
	$configText .= "hasMethod = false"."\n";
    }

    if($hasSpatial ne "Yes" && $hasSpatial ne "yes"){
	$configText .= "hasSpatial = false"."\n";
    } else {
	my $spatialRequired  = readParam("Do you want to make spatial coverage required [Yes/No]:", "Yes");
	if($spatialRequired ne "Yes" && $spatialRequired ne "yes"){
	    $configText .= "spatialRequired = false"."\n";
	}
    }

    if($hasTaxonomic ne "Yes" && $hasTaxonomic ne "yes"){
	$configText .= "hasTaxonomic = false"."\n";
    }

    if($hasTemporal ne "Yes" && $hasTemporal ne "yes"){
	$configText .= "hasTemporal = false"."\n";
    } else {
	my $temporalRequired  = readParam("Do you want to make temporal coverage required [Yes/No]:", "Yes");
	if($temporalRequired ne "Yes" && $temporalRequired ne "yes"){
	    $configText .= "temporalRequired = false"."\n";
	}
    }


    $configText .= "accesspubid = -//ecoinformatics.org//eml-access-2.0.0beta6//EN"."\n";
    $configText .= "accesssysid = eml-access.dtd"."\n";
    $configText .= "datasetpubid = eml://ecoinformatics.org/eml-dataset-2.0.0"."\n";
    $configText .= "datasetsysid = eml-dataset.dtd"."\n";
    $configText .= "mailhost = ".$mailhost."\n";
    $configText .= "sender = ".$senderEmail."\n";
    $configText .= "recipient = ".$adminEmail."\n";
    $configText .= "adminname = ".$adminName."\n";
    $configText .= "debug = ".$debugLevel."\n";
    $configText .= "contactEmailAddressRequired = 'false'\n";
    $configText .= "adminIsDocOwner = 'false'\n";
    $configText .= "#"."\n";
    $configText .= "# These are the sites and their coordinates. Coordinates are in"."\n";
    $configText .= "# degrees:minutes:seconds:direction format"."\n";
    $configText .= "# Make sure there is a lat/lon pair for every site"."\n";
    $configText .= "#"."\n";
    $configText .= "# example format ..."."\n";
    $configText .= "# lat [UK] Harwood forest[Sitka Spruce] = 55:12:46:N"."\n";
    $configText .= "# lon [UK] Harwood forest[Sitka Spruce] = 2:2:15:W"."\n";


    #print $configText;
    
    my $writefilehandle = new IO::File;
    $writefilehandle->open(">../../../lib/style/skins/$scope/$scope.cfg") or die "Could not open ../../../lib/style/skins/$scope/$scope.cfg";
    $writefilehandle->write($configText, length($configText));
    $writefilehandle->close;
}

sub copyFile{
    my $filename = shift;

    my $readfilehandle = new IO::File;
    $readfilehandle->open("<$filename") or die "Could not open $filename";

    my $text = "";
    my $newtext;
    
    while ($readfilehandle->read($newtext, 1)){
	$text .= $newtext;
    }
    
    $text =~ s/<\@scope\@>/$scope/g;
    $text =~ s/<\@organization\@>/$org/g;
    $text =~ s/<\@orgabbrev\@>/$orgabbrev/g;
    $text =~ s/<\@orgurl\@>/$orgurl/g;

    $readfilehandle->close;
    
    my $writefilehandle = new IO::File;
    $writefilehandle->open(">../../../lib/style/skins/$scope/$filename") or die "Could not open $filename";
    $writefilehandle->write($text, length($text));
    $readfilehandle->close;
    $writefilehandle->close;
}


sub copyAndRenameFile{
    my $ext = shift;

    my $readfilehandle = new IO::File;
    $readfilehandle->open("<scope.$ext") or die "Could not open scope.$ext";

    my $text = "";
    my $newtext;
    
    while ($readfilehandle->read($newtext, 1)){
	$text .= $newtext;
    }
    
    $text =~ s/<\@scope\@>/$scope/g;
    $text =~ s/<\@organization\@>/$org/g;
    $text =~ s/<\@orgabbrev\@>/$orgabbrev/g;
    $text =~ s/<\@orgurl\@>/$orgurl/g;

    $readfilehandle->close;
    
    my $writefilehandle = new IO::File;
    $writefilehandle->open(">../../../lib/style/skins/$scope/$scope.$ext") or die "Could not open ../../../lib/style/skins/$scope/$scope.$ext";
    $writefilehandle->write($text, length($text));
    $readfilehandle->close;
    $writefilehandle->close;
}
