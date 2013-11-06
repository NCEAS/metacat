#!/usr/bin/env perl
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
# In this script, we process the form fields passed in from a POST, insert a
# metadata document and an ACL document.

use lib '../WEB-INF/lib';
use Metacat;
use Config::Properties;
use Cwd 'abs_path';
use XML::LibXML;
use XML::LibXSLT;
use Template;
use Net::SMTP;
use CGI qw/:standard :html3/;
use CGI::Session;
use Digest::SHA1;
use File::stat;
use File::Basename;
use File::Temp;
use strict;

#debug("running register-dataset.cgi");

# Global configuration paramters
my $cgiDir           = $ENV{'SCRIPT_FILENAME'};
my $workingDirectory = ".";
if ( $cgiDir ne "" ) {
	my $workingDirectory = dirname($cgiDir);
}
my $metacatProps = "${workingDirectory}/../WEB-INF/metacat.properties";
my $properties   = new Config::Properties();
unless ( open( METACAT_PROPERTIES, $metacatProps ) ) {
	print "Content-type: text/html\n\n";
	print
"Unable to locate Metacat properties. Working directory is set as '$workingDirectory', is this correct?";
	exit();
}

$properties->load(*METACAT_PROPERTIES);

# local directory configuration
my $skinsDir     = "${workingDirectory}/../style/skins";
my $templatesDir = abs_path("${workingDirectory}/../style/common/templates");
my $tempDir      = $properties->getProperty('application.tempDir');
my $dataDir      = $properties->getProperty('application.datafilepath');

# url configuration
my $server = $properties->splitToTree( qr/\./, 'server' );
my $protocol = 'http://';
if ( $properties->getProperty('server.httpPort') eq '443' ) {
	$protocol = 'https://';
}
my $contextUrl = $protocol . $properties->getProperty('server.name');
if ( $properties->getProperty('server.httpPort') ne '80' ) {
	$contextUrl =
	  $contextUrl . ':' . $properties->getProperty('server.httpPort');
}
$contextUrl =
  $contextUrl . '/' . $properties->getProperty('application.context');

my $metacatUrl = $contextUrl . "/metacat";
my $cgiPrefix =
  "/" . $properties->getProperty('application.context') . "/cgi-bin";
my $styleSkinsPath  = $contextUrl . "/style/skins";
my $styleCommonPath = $contextUrl . "/style/common";

my $now = time;

# Import all of the HTML form fields as variables
import_names('FORM');

# Must have a config to use Metacat
my $skinName = "";
if ( hasContent($FORM::cfg) ) {
	$skinName = $FORM::cfg;
}
elsif ( hasContent( $ARGV[0] ) ) {
	$skinName = $ARGV[0];
}
else {
	debug("No configuration set.");
	print "Content-type: text/html\n\n";
	'Registry Error: The registry requires a skin name to continue.';
	exit();
}

# Metacat isn't initialized, the registry will fail in strange ways.
if ( !hasContent($metacatUrl) ) {
	debug("No Metacat.");
	print "Content-type: text/html\n\n";
	'Registry Error: Metacat is not initialized! Make sure'
	  . ' MetacatUrl is set correctly in '
	  . $skinName
	  . '.properties';
	exit();
}

# Set up the hash for returning data to the HTML templates
my $templateVars = { 'status' => 'success' };
my $error = 0;
my @errorMessages;

my $skinProperties = new Config::Properties();
if ( !hasContent($skinName) ) {
	$error = "Application misconfigured.  Please contact the administrator.";
	push( @errorMessages, $error );
}
else {
	my $skinProps = "$skinsDir/$skinName/$skinName.properties";
	unless ( open( SKIN_PROPERTIES, $skinProps ) ) {
		print "Content-type: text/html\n\n";
		print "Unable to locate skin properties at $skinProps.  Is this path correct?";
		exit();
	}
	$skinProperties->load(*SKIN_PROPERTIES);
}

# replacements for appconfig values using properties
my $moderators = $properties->getProperty('auth.moderators');
my $config     = $skinProperties->splitToTree( qr/\./, 'registry.config' );
my $templates  = $skinProperties->splitToTree( qr/\./, 'registry.templates' );
my $modules    = $skinProperties->splitToTree( qr/\./, 'registry.modules' );
my $required   = $skinProperties->splitToTree( qr/\./, 'registry.required' );
my $spatial    = $skinProperties->splitToTree( qr/\./, 'registry.spatial' );
my $show       = $skinProperties->splitToTree( qr/\./, 'registry.show' );

# set stderr printing if configured
my $debug_enabled = $config->{'debug'};
if ($FORM::debug) {
	$debug_enabled = $FORM::debug;
}

# skin owner credentials
my $adminUsername = $config->{'username'};
my $adminPassword = $config->{'password'};

# contains sender, recipient, admin, mailhost
my $skinEmail = $skinProperties->splitToTree( qr/\./, 'email' );
my $email = $properties->splitToTree( qr/\./, 'email' );

# override email properties with skin-based ones
my @emailData = keys(%$email);
foreach my $d (@emailData) {
	if ( %$skinEmail->{$d} ) {
		$email->{$d} = %$skinEmail->{$d};
	}
}

# convert the lat and lon configs into usable data structures
my @sitelist;
my %siteLatDMS;
my %siteLongDMS;

while ( my ( $key, $value ) = each(%$spatial) ) {
	my ( $name, $lon, $lat ) = split( /\|/, $value );
	my ( $latd, $latm, $lats, $latdir ) = split( /\|/, $lat );
	my ( $lond, $lonm, $lons, $londir ) = split( /\|/, $lon );
	push( @sitelist, $name );
	$siteLatDMS{$name}  = [ $latd, $latm, $lats, $latdir ];
	$siteLongDMS{$name} = [ $lond, $lonm, $lons, $londir ];
}

# set some configuration options for the template object
my $ttConfig = {
	INCLUDE_PATH => $templatesDir,
	INTERPOLATE  => 0,
	POST_CHOMP   => 1,
	DEBUG        => 1,
};

# create an instance of the template processor
my $template = Template->new($ttConfig) || die $Template::ERROR, "\n";

#print "Content-type: text/html\n\n";
#print "Is debug enabled? `$debug_enabled`";
#use Data::Dumper;
#print Dumper($config);
#exit;
# Set up the template information that is common to all forms

$$templateVars{'contextUrl'}      = $contextUrl;
$$templateVars{'styleSkinsPath'}  = $styleSkinsPath;
$$templateVars{'styleCommonPath'} = $styleCommonPath;
$$templateVars{'cgiPrefix'}       = $cgiPrefix;
$$templateVars{'metacatUrl'}      = $metacatUrl;
$$templateVars{'cfg'}             = $skinName;
$$templateVars{'email'}           = $email;
$$templateVars{'templates'}       = $templates;
$$templateVars{'required'}        = $required;
$$templateVars{'config'}          = $config;

debug("Initialized -- stage set: $FORM::stage");

# handle pids, set the mapped docid in the FORM params
# see: https://projects.ecoinformatics.org/ecoinfo/issues/5932
debug("PID: $FORM::pid");
if ($FORM::pid ne "" ) {
	my $pid = $FORM::pid;
	my $metacat = Metacat->new($metacatUrl);
	my $docid = $metacat->getDocid($pid);
	$FORM::docid = $docid;
}

# Process the form based on stage parameter.
if ( $FORM::stage =~ "loginform" ) {
	print "Content-type: text/html\n\n";

	# Send back the login form.....
	my $session = CGI::Session->load() or die CGI::Session->errstr();

	if ( $FORM::submission eq 'true' ) {
		$$templateVars{'message'} = 'You must login to view your submissions.';
	}

	if ( !$session->is_empty ) {

		# session found ... delete the session....
		$session->delete();
	}

	$template->process( $templates->{'login'}, $templateVars );
	exit();
}
elsif ( $FORM::stage =~ "logout" ) {
	handleLogoutRequest();
	exit();
}
elsif ( $FORM::stage =~ "login" ) {
	handleLoginRequest();
	exit();
}
elsif ( $FORM::stage =~ "mod_accept" ) {
	handleModAccept();
	exit();
}
elsif ( $FORM::stage =~ "mod_decline" ) {
	handleModDecline();
	exit();
}
elsif ( $FORM::stage =~ "mod_revise" ) {
	handleModRevise();
	exit();
}
elsif ( $FORM::stage =~ "read" ) {
	handleRead();
	exit();
}
elsif ( $FORM::stage =~ "review_frame" ) {
	handleReviewFrame();
	exit();
}

print "Content-type: text/html\n\n";

if ( $FORM::stage =~ "guide" ) {

	# Send back the information on how to fill the form
	$$templateVars{'section'} = "Guide on How to Complete Registry Entries";
	$template->process( $templates->{'guide'}, $templateVars );
	exit();

}
elsif ( $FORM::stage =~ "insert" ) {

	# The user has entered the data. Do data validation and send back data
	# to confirm the data that has been entered.
	toConfirmData();
	exit();

}
elsif ($FORM::dataWrong =~ "No, go back to editing"
	&& $FORM::stage =~ "confirmed" )
{

	# The user wants to correct the data that he has entered.
	# Hence show the data again in entryData form.
	confirmDataToReEntryData();
	exit();

}
elsif ( $FORM::stage =~ "modify" ) {
	#debug("in modify stage");
	# Modification of a file has been requested.
	# check if the user is logged in...
	my $session = CGI::Session->load() or die CGI::Session->errstr();
	if ( $session->is_empty ) {

		# no session found ... redirect to login page template
		$$templateVars{'message'} = 'You must login to modify your dataset.';
		$template->process( $templates->{'login'}, $templateVars );
	}
	else {

		# Show the form will all the values filled in.
		my @sortedSites;
		foreach my $site ( sort @sitelist ) {
			push( @sortedSites, $site );
		}
		$$templateVars{'siteList'} = \@sortedSites;
		$$templateVars{'section'}  = "Modification Form";
		my ( $foundScope, $id, $rev ) = split( /\./, $FORM::docid );
		if ( !$rev ) {
			my $metacat = Metacat->new($metacatUrl);
			my $lastRev = $metacat->getLastRevision($FORM::docid);
			$$templateVars{'docid'} = $FORM::docid . "." . $lastRev;
		}
		else {
			$$templateVars{'docid'} = $FORM::docid;
		}
		modifyData();
	}
	exit();

}
elsif ( $FORM::stage =~ "delete_confirm" ) {

	# Result from deleteData form.
	if ( $FORM::deleteData =~ "Delete document" ) {

		# delete Data
		deleteData(1);
		exit();
	}
	else {
		$$templateVars{'status'}   = "Cancel";
		$$templateVars{'function'} = "cancel";
		$template->process( $templates->{'response'}, $templateVars );
		exit();
	}

}
elsif ( $FORM::stage =~ "delete" ) {

	# Deletion of a file has been requested.
	# Ask for username and password using deleteDataForm
	$$templateVars{'docid'} = $FORM::docid;
	$template->process( $templates->{'deleteData'}, $templateVars );
	exit();

}
elsif ( $FORM::stage !~ "confirmed" ) {

	# None of the stages have been reached and data is not being confirmed.

	# check if the user is logged in...
	my $session = CGI::Session->load() or die CGI::Session->errstr();
	if ( $session->is_empty ) {

		# no session found ... redirect to login page template
		$$templateVars{'showInstructions'} = 'true';
		$$templateVars{'message'} = 'You must login to register your dataset.';
		$template->process( $templates->{'login'}, $templateVars );
	}
	else {

		# Hence, send back entry form for entry of data.
		debug("Sending form");
		my @sortedSites;
		foreach my $site ( sort @sitelist ) {
			push( @sortedSites, $site );
		}

		if ( $skinName eq 'nceas' ) {
			my $projects = getProjectList($properties);
			$$templateVars{'projects'} = $projects;
			$$templateVars{'wg'}       = \@FORM::wg;
		}

		$$templateVars{'modules'}   = $modules;
		$$templateVars{'required'}  = $required;
		$$templateVars{'templates'} = $templates;
		$$templateVars{'show'}      = $show;
		$$templateVars{'site'}      = $config->{'site'};

		$$templateVars{'siteList'} = \@sortedSites;
		$$templateVars{'section'}  = "Entry Form";
		$$templateVars{'docid'}    = "";
		debug("Sending form: ready to process template");
		$template->process( $templates->{'entry'}, $templateVars );
		debug("Sending form: template processed");
	}
	exit();
}

# Confirm stage has been reached. Enter the data into metacat.

# Initialize some global vars
my $latDeg1      = "";
my $latMin1      = "";
my $latSec1      = "";
my $hemisphLat1  = "";
my $longDeg1     = "";
my $longMin1     = "";
my $longSec1     = "";
my $hemisphLong1 = "";
my $latDeg2      = "";
my $latMin2      = "";
my $latSec2      = "";
my $hemisphLat2  = "";
my $longDeg2     = "";
my $longMin2     = "";
my $longSec2     = "";
my $hemisphLong2 = "";
my $modUsername  = "";
my $modPassword  = "";

# validate the input form parameters
my $invalidParams;

if ( !$error ) {
	$invalidParams = validateParameters(1);
	if ( scalar(@$invalidParams) ) {
		$$templateVars{'status'}        = 'failure';
		$$templateVars{'invalidParams'} = $invalidParams;
		$error                          = 1;
	}
}

my $docid;

# Create a metacat object
my $metacat = Metacat->new($metacatUrl);

if ( !$error ) {

	# Login to metacat
	my ( $username, $password ) = getCredentials();
	my $response = $metacat->login( $username, $password );
	my $errorMessage = "";

	# Parameters have been validated and Create the XML document
	my $xmldoc = createXMLDocument();

	my $xmldocWithDocID = $xmldoc;
	my $errorMessage    = "";

	if ( !$response ) {
		debug("No response from Metacat");
		push( @errorMessages, $metacat->getMessage() );
		push( @errorMessages, "Failed during login.\n" );
		$$templateVars{'status'}        = 'login_failure';
		$$templateVars{'errorMessages'} = \@errorMessages;
		$$templateVars{'docid'}         = $docid;
		$$templateVars{'cfg'}           = $skinName;
		$$templateVars{'function'}      = "submitted";
		$$templateVars{'section'}       = "Submission Status";
		$template->process( $templates->{'response'}, $templateVars );
		exit();
	}
	else {

		if ( $config->{'adminIsDocOwner'} eq 'true' ) {
			debug("adminIsDocOwner is set.");
			$response = $metacat->login( $adminUsername, $adminPassword );
			if ( !$response ) {
				push( @errorMessages, $metacat->getMessage() );
				push( @errorMessages, "Failed during login for admin.\n" );
				$$templateVars{'status'}        = 'login_failure';
				$$templateVars{'errorMessages'} = \@errorMessages;
				$$templateVars{'docid'}         = $docid;
				$$templateVars{'cfg'}           = $skinName;
				$$templateVars{'function'}      = "submitted";
				$$templateVars{'section'}       = "Submission Status";
				$template->process( $templates->{'response'}, $templateVars );
				exit();
			}
		}

		debug("A");
		if ( $FORM::docid eq "" ) {
			debug("B1");

			# document is being inserted
			my $docStatus = "INCOMPLETE";
			while ( $docStatus eq "INCOMPLETE" ) {
				$docid = newAccessionNumber( $config->{'scope'}, $metacat );

				$xmldocWithDocID =~ s/docid/$docid/;
				debugDoc($xmldocWithDocID);
				$docStatus = insertMetadata( $xmldocWithDocID, $docid );
			}
			debug("B2");
			if ( $docStatus ne "SUCCESS" ) {
				debug("NO SUCCESS");
				debug("Message is: $docStatus");
				push( @errorMessages, $docStatus );
			}
			else {
				deleteRemovedData();
			}

			debug("B3");
		}
		else {
			debug("M1");

			# document is being modified
			$docid = incrementRevision($FORM::docid);

			$xmldoc =~ s/docid/$docid/;
			debugDoc($xmldoc);

			my $response = $metacat->update( $docid, $xmldoc );

			if ( !$response ) {
				push( @errorMessages, $metacat->getMessage() );
				push( @errorMessages, "Failed while updating.\n" );
			}

			debug("M2, $docid");
			if ( scalar(@errorMessages) ) {
				debug("Errors defined in modify.");

				$$templateVars{'docid'} = $FORM::docid;
				copyFormToTemplateVars();
				$$templateVars{'status'}        = 'failure';
				$$templateVars{'errorMessages'} = \@errorMessages;
				$error                          = 1;
			}
			else {
				deleteRemovedData();
				$$templateVars{'docid'} = $docid;
				$$templateVars{'cfg'}   = $skinName;
			}

			# Create our HTML response and send it back
			$$templateVars{'function'} = "modified";
			$$templateVars{'section'}  = "Modification Status";
			$template->process( $templates->{'response'}, $templateVars );

			# send a notification email to the moderator
			if ( hasContent($FORM::cfg) && $FORM::cfg eq 'esa' ) {
				my $title               = "";
				my $contactEmailAddress = "";
				my $contactName         = "";
				my $parser              = XML::LibXML->new();
				my $parsedDoc           = $parser->parse_string($xmldoc);
				$FORM::function = 'modified';

				my $findNodes = $parsedDoc->findnodes('//dataset/title');
				if ( $findNodes->size() > 0 ) {

					# found title
					my $node = '';
					foreach $node ( $findNodes->get_nodelist ) {
						$title = findValue( $node, '../title' );
					}
				}

				$findNodes = $parsedDoc->findnodes('//dataset/contact');
				if ( $findNodes->size() > 0 ) {

					# found contact email address
					my $node = '';
					foreach $node ( $findNodes->get_nodelist ) {
						my $surName =
						  findValue( $node, 'individualName/surName' );
						my $givenName =
						  findValue( $node, 'individualName/givenName' );
						my $organizationName =
						  findValue( $node, 'organizationName' );

						if ( $surName ne '' ) {
							$contactName = $givenName . ' ' . $surName;
						}
						else {
							$contactName = $organizationName;
						}
					}
				}

				$FORM::docid = $docid;

				modSendNotification( $title, $contactEmailAddress, $contactName,
					"Document $docid modification review pending" );
			}
			exit();
		}
	}

	if ( hasContent($FORM::cfg) && $FORM::cfg eq 'esa' ) {
		my $title               = "";
		my $contactEmailAddress = "";
		my $contactName         = "";
		my $parser              = XML::LibXML->new();
		my $parsedDoc           = $parser->parse_string($xmldoc);

		my $findNodes = $parsedDoc->findnodes('//dataset/title');
		if ( $findNodes->size() > 0 ) {

			# found title
			my $node = '';
			foreach $node ( $findNodes->get_nodelist ) {
				$title = findValue( $node, '../title' );
			}
		}

		$findNodes = $parsedDoc->findnodes('//dataset/contact');
		if ( $findNodes->size() > 0 ) {

			# found contact email address
			my $node = '';
			foreach $node ( $findNodes->get_nodelist ) {
				$contactEmailAddress = findValue( $node, 'electronicMailAddress' );
				my $surName   = findValue( $node, 'individualName/surName' );
				my $givenName = findValue( $node, 'individualName/givenName' );
				my $organizationName = findValue( $node, 'organizationName' );

				if ( $surName ne '' ) {
					$contactName = $givenName . ' ' . $surName;
				}
				else {
					$contactName = $organizationName;
				}
			}
		}
		$FORM::docid = $docid;

		modSendNotification( $title, $contactEmailAddress, $contactName,
			"Document $docid review pending" );
	}
}

debug("C");

if ( scalar(@errorMessages) ) {
	debug("ErrorMessages defined.");
	$$templateVars{'docid'} = $FORM::docid;
	copyFormToTemplateVars();
	$$templateVars{'status'}        = 'failure';
	$$templateVars{'errorMessages'} = \@errorMessages;
	$error                          = 1;
}
else {
	$$templateVars{'docid'} = $docid;
	$$templateVars{'cfg'}   = $skinName;

	# delete the remaining file objects from disk
	for ( my $fileNum = 0 ; $fileNum <= $FORM::upCount ; $fileNum++ ) {
		my $fn = 'uploadname_' . $fileNum;
		if ( hasContent( param($fn) ) ) {
			deleteFile( param($fn) );
		}
	}

}

# Create our HTML response and send it back
$$templateVars{'function'} = "submitted";
$$templateVars{'section'}  = "Submission Status";

$template->process( $templates->{'response'}, $templateVars );

exit();

################################################################################
#
# Subroutine for inserting a document to metacat
#
################################################################################
sub insertMetadata {
	my $xmldoc = shift;
	my $docid  = shift;

	debug("Trying to insert the following document");
	my $docStatus = "SUCCESS";
	debug("Starting insert of $docid (D1)");

	my $response = $metacat->insert( $docid, $xmldoc );
	if ( !$response ) {
		debug("Response gotten (D2)");
		my $errormsg = $metacat->getMessage();
		i debug( "Error is (D3): " . $errormsg );
		if ( $errormsg =~ /is already in use/ ) {
			$docStatus = "INCOMPLETE";
		}
		elsif ( $errormsg =~ /<login>/ ) {
			$docStatus = "SUCCESS";
		}
		else {
			$docStatus = $errormsg;
		}
	}
	debug("Ending insert (D4)");

	return $docStatus;
}

################################################################################
#
# Subroutine for generating a new accession number
#  Note: this is not threadsafe, assumes only one running process at a time
#  Also: need to check metacat for max id # used in this scope already
################################################################################
sub newAccessionNumber {
	my $scope    = shift;
	my $metacat  = shift;
	my $errormsg = 0;

	my $docid = $metacat->getLastId($scope);
	if ( !$docid ) {
		$docid = "$scope.1.1";
		debug( "Error in newAccessionNumber: " . $metacat->getMessage() );
	}
	else {
		my ( $foundScope, $id, $rev ) = split( /\./, $docid );
		$id++;
		$docid = "$scope.$id.1";
	}
	debug("Metcat handed us a new docid: $docid");
	return $docid;
}

sub incrementRevision {
	my $initDocid = shift;
	my $docid     = '';
	if ( !$initDocid ) {
		debug("No docid entered.");
	}
	else {
		my ( $scope, $id, $rev ) = split( /\./, $initDocid );
		$rev++;
		$docid = "$scope.$id.$rev";
	}
	return $docid;
}

################################################################################
#
# Validate the parameters to make sure that required params are provided
#
################################################################################
sub validateParameters {
	my $chkUser = shift;
	my @invalidParams;

	push( @invalidParams, "Name of the Project is not selected in the form." )
	  if ( scalar(@FORM::wg) == 0 && $required->{'wgList'} eq 'true' );
	push( @invalidParams, "First name of person entering the form is missing." )
	  unless hasContent($FORM::providerGivenName);
	push( @invalidParams, "Last name of person entering the form is missing." )
	  unless hasContent($FORM::providerSurName);
	push( @invalidParams, "Dataset title is missing." )
	  unless hasContent($FORM::title);
	push( @invalidParams, ucfirst( $config->{'site'} ) . " name is missing." )
	  unless ( ( hasContent($FORM::site) && !( $FORM::site =~ /^Select/ ) )
		|| $skinName eq "nceas" );
	push( @invalidParams, "First name of principal data set owner is missing." )
	  unless hasContent($FORM::origNamefirst0);
	push( @invalidParams, "Last name of principal data set owner is missing." )
	  unless hasContent($FORM::origNamelast0);
	push( @invalidParams, "Dataset abstract is missing." )
	  unless hasContent($FORM::abstract);

	if ( $modules->{'temporal'} eq 'true' ) {
		push( @invalidParams, "Year of start date is missing." )
		  unless ( hasContent($FORM::beginningYear)
			|| $required->{'temporal'} ne 'true' );
		push( @invalidParams,
"Year of stop date has been specified but year of start date is missing."
		  )
		  if ( ( !hasContent($FORM::beginningYear) )
			&& hasContent($FORM::endingYear) );
	}
	push( @invalidParams, "Geographic description is missing." )
	  unless ( hasContent($FORM::geogdesc)
		|| $required->{'spatial'} ne 'true' );

	if ( $FORM::beginningMonth eq "MM" ) {
		$FORM::beginningMonth = "";
	}
	if ( $FORM::beginningDay eq "DD" ) {
		$FORM::beginningDay = "";
	}
	if ( $FORM::endingMonth eq "MM" ) {
		$FORM::endingMonth = "";
	}
	if ( $FORM::endingDay eq "DD" ) {
		$FORM::endingDay = "";
	}

	if ( hasContent($FORM::beginningYear)
		&& !( $FORM::beginningYear =~ /[0-9]{4}/ ) )
	{
		push( @invalidParams, "Invalid year of start date specified." );
	}

	if ( hasContent($FORM::endingYear) && !( $FORM::endingYear =~ /[0-9]{4}/ ) )
	{
		push( @invalidParams, "Invalid year of stop date specified." );
	}

	# If the "use site" coord. box is checked and if the site is in
	# the longitude hash ...  && ($siteLatDMS{$FORM::site})

	if ( $modules->{'spatial'} eq 'true' ) {
		if ( ($FORM::useSiteCoord) && ( $siteLatDMS{$FORM::site} ) ) {
			$latDeg1      = $siteLatDMS{$FORM::site}[0];
			$latMin1      = $siteLatDMS{$FORM::site}[1];
			$latSec1      = $siteLatDMS{$FORM::site}[2];
			$hemisphLat1  = $siteLatDMS{$FORM::site}[3];
			$longDeg1     = $siteLongDMS{$FORM::site}[0];
			$longMin1     = $siteLongDMS{$FORM::site}[1];
			$longSec1     = $siteLongDMS{$FORM::site}[2];
			$hemisphLong1 = $siteLongDMS{$FORM::site}[3];
		}
		else {
			$latDeg1      = $FORM::latDeg1;
			$latMin1      = $FORM::latMin1;
			$latSec1      = $FORM::latSec1;
			$hemisphLat1  = $FORM::hemisphLat1;
			$longDeg1     = $FORM::longDeg1;
			$longMin1     = $FORM::longMin1;
			$longSec1     = $FORM::longSec1;
			$hemisphLong1 = $FORM::hemisphLong1;
		}

		if ( $latDeg1 > 90 || $latDeg1 < 0 ) {
			push( @invalidParams, "Invalid first latitude degrees specified." );
		}
		if ( $latMin1 > 59 || $latMin1 < 0 ) {
			push( @invalidParams, "Invalid first latitude minutes specified." );
		}
		if ( $latSec1 > 59 || $latSec1 < 0 ) {
			push( @invalidParams, "Invalid first latitude seconds specified." );
		}
		if ( $longDeg1 > 180 || $longDeg1 < 0 ) {
			push( @invalidParams,
				"Invalid first longitude degrees specified." );
		}
		if ( $longMin1 > 59 || $longMin1 < 0 ) {
			push( @invalidParams,
				"Invalid first longitude minutes specified." );
		}
		if ( $longSec1 > 59 || $longSec1 < 0 ) {
			push( @invalidParams,
				"Invalid first longitude seconds specified." );
		}

		if ( hasContent($FORM::latDeg2)
			&& ( $FORM::latDeg2 > 90 || $FORM::latDeg2 < 0 ) )
		{
			push( @invalidParams,
				"Invalid second latitude degrees specified." );
		}
		if ( hasContent($FORM::latMin2)
			&& ( $FORM::latMin2 > 59 || $FORM::latMin2 < 0 ) )
		{
			push( @invalidParams,
				"Invalid second latitude minutes specified." );
		}
		if ( hasContent($FORM::latSec2)
			&& ( $FORM::latSec2 > 59 || $FORM::latSec2 < 0 ) )
		{
			push( @invalidParams,
				"Invalid second latitude seconds specified." );
		}
		if ( hasContent($FORM::latDeg2)
			&& ( $FORM::longDeg2 > 180 || $FORM::longDeg2 < 0 ) )
		{
			push( @invalidParams,
				"Invalid second longitude degrees specified." );
		}
		if ( hasContent($FORM::latMin2)
			&& ( $FORM::longMin2 > 59 || $FORM::longMin2 < 0 ) )
		{
			push( @invalidParams,
				"Invalid second longitude minutes specified." );
		}
		if ( hasContent($FORM::latSec2)
			&& ( $FORM::longSec2 > 59 || $FORM::longSec2 < 0 ) )
		{
			push( @invalidParams,
				"Invalid second longitude seconds specified." );
		}
	}

	# Check if latDeg1 and longDeg1 has values if useSiteCoord is used.
	# This check is required because some of the sites dont have lat
	# and long mentioned in the config file.

	if ( $modules->{'spatial'} eq 'true' && $required->{'spatial'} eq 'true' ) {
		if ($FORM::useSiteCoord) {
			push( @invalidParams,
"The Data Registry doesn't have latitude and longitude information for the site that you chose. Please go back and enter the spatial information."
			) unless ( hasContent($latDeg1) && hasContent($longDeg1) );
		}
		else {
			push( @invalidParams, "Latitude degrees are missing." )
			  unless ( hasContent($latDeg1)
				|| $required->{'spatial'} ne 'true' );
			push( @invalidParams, "Longitude degrees are missing." )
			  unless ( hasContent($longDeg1)
				|| $required->{'spatial'} ne 'true' );
		}
		push( @invalidParams,
"You must provide a geographic description if you provide latitude and longitude information."
		  )
		  if ( ( hasContent($latDeg1) || ( hasContent($longDeg1) ) )
			&& ( !hasContent($FORM::geogdesc) ) );
	}

	if ( $modules->{'method'} eq 'true' ) {
		push( @invalidParams,
"You must provide a method description if you provide a method title."
		  )
		  if (
			hasContent($FORM::methodTitle)
			&& (  !( scalar(@FORM::methodPara) > 0 )
				|| ( !hasContent( $FORM::methodPara[0] ) ) )
		  );
		push( @invalidParams,
"You must provide a method description if you provide an extent of study description."
		  )
		  if (
			hasContent($FORM::studyExtentDescription)
			&& (  !( scalar(@FORM::methodPara) > 0 )
				|| ( !hasContent( $FORM::methodPara[0] ) ) )
		  );
		push( @invalidParams,
"You must provide both an extent of study description and a sampling description, or neither."
		  )
		  if (
			(
				hasContent($FORM::studyExtentDescription)
				&& !hasContent($FORM::samplingDescription)
			)
			|| (  !hasContent($FORM::studyExtentDescription)
				&& hasContent($FORM::samplingDescription) )
		  );
	}

	if ( $modules->{'upload'} eq 'true' ) {
		for ( my $upNum = 0 ; $upNum <= $FORM::upCount ; $upNum++ ) {
			my $upn = "upload_$upNum";
			if ( hasContent( param($upn) )
				&& !grep { $_ eq ("uploadname_$upNum") } @FORM::deletefile )
			{
				push( @invalidParams,
					"Must select a permission for file "
					  . param("uploadname_$upNum") )
				  if ( !hasContent( param("uploadperm_$upNum") ) );
			}
		}
	}

	push( @invalidParams, "First name of data set contact is missing." )
	  unless ( hasContent($FORM::origNamefirstContact)
		|| $FORM::useOrigAddress );
	push( @invalidParams, "Last name of data set contact is missing." )
	  unless ( hasContent($FORM::origNamelastContact)
		|| $FORM::useOrigAddress );
	if ( $required->{'contactEmailAddress'} eq 'true' ) {
		if ($FORM::useOrigAddress) {
			push( @invalidParams,
"Email address of data set owner is missing. This is required as it will be used as contact email address as specified by you."
			) unless ( hasContent($FORM::origEmail) );
		}
		else {
			push( @invalidParams,
				"Email address of data set contact is missing." )
			  unless ( hasContent($FORM::origEmailContact) );
		}
	}

	# check required distribution elements
	push( @invalidParams, "Data medium is required." )
	  unless ( hasContent($FORM::dataMedium) );
	if ( $FORM::dataMedium eq 'other' ) {
		push( @invalidParams,
			"Must enter custom data medium when 'other' is selected." )
		  unless ( hasContent($FORM::dataMediumOther) );
	}
	push( @invalidParams, "Usage rights are required." )
	  unless ( hasContent($FORM::useConstraints) );
	if ( $FORM::useConstraints eq 'other' ) {
		push( @invalidParams,
			"Must enter custom usage rights when 'other' is selected." )
		  unless ( hasContent($FORM::useConstraintsOther) );
	}

	return \@invalidParams;
}

################################################################################
#
# utility function to determine if a paramter is defined and not an empty string
#
################################################################################
sub hasContent {
	my $param = shift;

	my $paramHasContent;
	if ( !defined($param) || $param eq '' ) {
		$paramHasContent = 0;
	}
	else {
		$paramHasContent = 1;
	}
	return $paramHasContent;
}

################################################################################
#
# Subroutine for replacing characters not recognizable by XML and otherwise.
#
################################################################################
sub normalize {
	my $val = shift;

	$val =~ s/&/&amp;/g;

	$val =~ s/</&lt;/g;
	$val =~ s/>/&gt;/g;
	$val =~ s/\"/&quot;/g;
	$val =~ s/%/&#37;/g;

	my $returnVal = "";

	foreach ( split( //, $val ) ) {
		my $var = unpack "C*", $_;

		if ( $var < 128 && $var > 31 ) {
			$returnVal = $returnVal . $_;
		}
		elsif ( $var < 32 ) {
			if ( $var == 10 ) {
				$returnVal = $returnVal . $_;
			}
			if ( $var == 13 ) {
				$returnVal = $returnVal . $_;
			}
			if ( $var == 9 ) {
				$returnVal = $returnVal . $_;
			}
		}
		else {
			$returnVal = $returnVal . $_;
		}
	}

	return $returnVal;
}

################################################################################
#
# Subroutine for replacing characters not recognizable by XML and otherwise
# except for ", > amd <.
#
################################################################################
sub delNormalize {
	my $val = shift;

	$val =~ s/&/&amp;/g;

	$val =~ s/%/&#37;/g;

	my $returnVal = "";

	foreach ( split( //, $val ) ) {
		my $var = unpack "C*", $_;

		if ( $var < 128 && $var > 31 ) {
			$returnVal = $returnVal . $_;
		}
		elsif ( $var < 32 ) {
			if ( $var == 10 ) {
				$returnVal = $returnVal . $_;
			}
			if ( $var == 13 ) {
				$returnVal = $returnVal . $_;
			}
			if ( $var == 9 ) {
				$returnVal = $returnVal . $_;
			}
		}
		else {
			$returnVal = $returnVal . "&#" . $var . ";";
		}
	}

	$returnVal =~ s/&/%26/g;
	return $returnVal;
}

################################################################################
#
# Subroutine for replacing characters that might create problem in HTML.
# Specifically written for " being used in any text field. This creates a
# problem in confirmData template, when you specify input name value pair
# with value having a " in it.
#
################################################################################
sub normalizeCD {
	my $val = shift;

	$val =~ s/\"/&quot;/g;

	return $val;
}

################################################################################
#
# Upload new file objects into Metacat, if they're present and valid.
#
################################################################################
sub allFileData {
	my %uploadedFiles = ();
	my $fileInfo;
	my $docid;

	for ( my $fileNum = 0 ; $fileNum <= $FORM::upCount ; $fileNum++ ) {
		my $fn = 'upload_' . $fileNum;
		if ( hasContent( param($fn) ) ) {

			# ignore data which is scheduled for deletion
			if ( grep { $_ eq ("uploadname_$fileNum") } @FORM::deletefile ) {
				debug(
"Not generating metadata for file scheduled for deletion: $fn"
				);
			}
			else {
				debug("Retrieving metadata for file: $fn");
				( $docid, $fileInfo ) = fileMetadata($fileNum);
				$uploadedFiles{$docid} = $fileInfo;
			}
		}
	}

	return %uploadedFiles;
}

sub fileMetadata {
	my $fileNum     = shift;
	my $fileHash    = param("upload_$fileNum");
	my $fileName    = param("uploadname_$fileNum");
	my $contentType = param("uploadtype_$fileNum");
	my $filePerm    = param("uploadperm_$fileNum");
	my $docid;
	my $outFile;
	my $cleanName = $fileName;

	# process an _existing_ data file, which is already within Metacat.
	if ( $fileHash =~ /ondisk/ ) {
		( $docid, $fileHash ) = datafileInfo($fileHash);
		$outFile = $dataDir . "/" . $docid;
	}
	else {

		# normalize input filenames; Windows filenames include full paths
		$cleanName =~ s/.*[\/\\](.*)/$1/;
		$outFile = $tempDir . "/" . $cleanName;
	}
	debug("Reading file from disk: $outFile");

	my $fileSize = stat($outFile)->size;
	if ( $fileSize == 0 ) {
		push( @errorMessages, "file $fileName is zero bytes!" );
		debug("File $fileName is zero bytes!");
	}

	# Now the file is on disk, send the object to Metacat
	my $session = CGI::Session->load();
	if ( $session->is_empty ) {
		push( @errorMessages, "Must be logged in to upload files." );
		debug("Not logged in, cannot upload files.");
		return 0;
	}

	# remove the uniqueness of the filename
	# 'tempXXXXX'
	$cleanName = substr($cleanName, 9);
	
	if ( !$docid ) {
		$docid = newAccessionNumber( $config->{'scope'}, $metacat );
		my $uploadReturn = uploadData( $outFile, $docid, $cleanName );
		if ( !$uploadReturn ) {
			debug("Uploading the data failed.");
		}
	}
	my $entityid  = $fileHash . "001";
	my $distribid = $fileHash . "002";

	my $uploadUrl = 'ecogrid://knb/' . $docid;

	# TODO:  should match the object promotion path, so that an
	#        Excel upload results in 'dataTable' in this field
	my $entityType = 'Other';
	
	my %dataInfo = (
		'docid'       => $docid,
		'entityid'    => $entityid,
		'distribid'   => $distribid,
		'fileName'    => $cleanName,
		'fileSize'    => $fileSize,
		'fileHash'    => $fileHash,
		'filePerm'    => $filePerm,
		'contentType' => $contentType,
		'url'         => $uploadUrl,
		'entityType'  => $entityType,
	);

	return ( $docid, \%dataInfo );
}

sub datafileInfo {
	my $finfo = shift;
	$finfo =~ s/ondisk://g;
	return my ( $docid, $fileHash ) = split( ":", $finfo );
}

sub processFile {
	my $fileName = shift;

	# test that we actually got a file
	if ( !$fileName || cgi_error() ) {
		debug( "Error receiving file " . cgi_error() );
	}

	# write file to disk, get SHA1 hash and size
	my ( $outFile, $fileHash ) = writeFile($fileName);
	debug( "processed file to temp directory:  $outFile" );

	my $fileSize = stat($outFile)->size;
	if ( $fileSize == 0 ) {
		push( @errorMessages, "file $fileName is zero bytes!" );
		debug("File $fileName is zero bytes!");
	}

	# file is in Metacat, generate the pertinent EML elements
	my $contentType = uploadInfo($fileName)->{'Content-Type'};

	# occasionally CGI.pm doesn't get the file info.  In this case,
	# use a default MIME type of text/plain.  Seems fixed in the newer CGI.pm:
	# http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=313141
	if ( !$contentType ) {
		$contentType = 'text/plain';
	}

	my %dataInfo = (
		'fileName'    => $outFile,
		'fileHash'    => $fileHash,
		'contentType' => $contentType,
	);

	return \%dataInfo;
}

sub writeFile {
	my $fileName = shift;
	my $fileData;
	my $length = 0;
	my $buffer;

	my $cleanName = $fileName;

	# normalize input filenames; Windows filenames include full paths
	$cleanName =~ s/.*[\/\\](.*)/$1/;

	while ( my $bytesRead = read( $fileName, $buffer, 4096 ) ) {
		$fileData .= $buffer;
		$length += $bytesRead;
	}

	# create SHA1 sum to store file hash
	my $ctx = Digest::SHA1->new;
	$ctx->add($fileData);
	my $digest = $ctx->hexdigest;

	# use tempfile for writing
	my $tmp = File::Temp->new( 
						TEMPLATE => 'tempXXXXX',
                        DIR => $tempDir,
                        SUFFIX => $cleanName, 
                        UNLINK => 0);
	my $outputName = $tmp->filename();
	#open( OUT, ">$outputName" ) or die "Could not open: $!";
	print $tmp $fileData;
	close($tmp);
	debug("Writing output, result is: $outputName");

	return ( $outputName, $digest );
}

sub deleteRemovedData {

# if we have any associated datafiles which are scheduled for deletion, remove them now
	for ( my $delNum = 0 ; $delNum <= $FORM::delCount ; $delNum++ ) {

	  # need to look up the actual upload number, which is contained in the name
		my $upNum = param("deletefile_$delNum");
		$upNum =~ s/uploadname_//;
		my $upn = param("upload_$upNum");
		if ( hasContent($upn) ) {
			debug("Deleting upload_$upNum, $upn");
			if ( grep { $_ eq ("uploadname_$upNum") } @FORM::deletefile ) {
				if ( param("upload_$upNum") =~ /ondisk/ ) {
					debug(
						"got a file which is ondisk, proceeding with deletion");
					deleteFileData( param("upload_$upNum") );
				}
				else {
					debug(
"got an old reference, not yet in EML, remove from tempdir"
					);
					deleteFile( param("uploadname_$upNum") );
				}
			}
			else {
				debug("Name didn't match in deletefile list");
			}
		}
	}
}

sub deleteFile {
	my $input    = shift;
	#my $fileName = $tempDir . "/" . $input;
	my $fileName = $input;

	if ( -e $fileName ) {
		unlink $fileName
		  or debug("Failed to delete file $fileName.");
	}
	else {
		debug("Unable to find file $fileName");
	}
	if ( !-e $fileName ) {
		debug("Successfully deleted $fileName");
	}
}

sub deleteFileData {
	my $input = shift;
	my ( $docid, $fileHash ) = datafileInfo($input);
	my $metacat = Metacat->new($metacatUrl);

	my ( $username, $password ) = getCredentials();
	my $response = $metacat->login( $username, $password );
	if ( !$response ) {
		my $msg = $metacat->getMessage();
		push( @errorMessages,
			"Failed to login with credentials for `$username`. Error was $msg"
		);
		debug(
"Failed to login with given credentials for username $username, Error is: $msg"
		);
	}
	else {
		$response = $metacat->delete($docid);
		if ( !$response ) {
			my $msg = $metacat->getMessage();
			push( @errorMessages,
				"Failed to delete existing file. Error was: $msg" );
			debug("Delete -- Error is: $msg");
		}
		else {
			debug("Delete -- Success! Removed docid $docid");
		}
	}
}

sub uploadData {
	my $data  = shift;
	my $docid = shift;
	my $filename = shift;

	debug("Upload -- Starting upload of $docid");
	my $response = $metacat->upload( $docid, $data, $filename );
	if ( !$response ) {
		my $uploadMsg = $metacat->getMessage();
		push( @errorMessages,
			"Failed to upload file. Error was: $uploadMsg\n" );
		debug("Upload -- Error is: $uploadMsg");
	}
	else {
		debug("Upload -- Success! New docid $docid");
	}
}

################################################################################
#
# Create the XML document from the HTML form input
# returns the XML document as a string
#
################################################################################
sub createXMLDocument {

	#FIXME placeholder for $FORM element, should be determined by config

	if ( $skinName eq "ebm" ) {
		return createProjectDocument();
	}
	else {
		return createDatasetDocument();
	}
}

sub createProjectDocument {
	my $doc = EMLStart();
	$doc .= accessElement();
	$doc .= datasetStart();
	$doc .= titleElement();
	$doc .= creatorNameElement();
	$doc .= creatorElement();

	$doc .= pubElement();
	$doc .= setDisplayType('project');
	$doc .= keywordElement();
	$doc .= contactElement();

	# putting everything else under project
	$doc .= "<project>";
	$doc .= titleElement();
	my %originators = personnelCreate('personnel');
	$doc .= personnelList( \%originators );
	$doc .= abstractElement();
	$doc .= "<studyAreaDescription>\n";
	$doc .= coverageElement();
	$doc .= "</studyAreaDescription>\n";

	$doc .= "</project>";
	$doc .= datasetEnd();
	$doc .= EMLEnd();
	return $doc;
}

sub createDatasetDocument {
	my $doc = EMLStart();
	$doc .= accessElement();
	$doc .= datasetStart();
	$doc .= titleElement();
	$doc .= creatorElement();
	$doc .= creatorContactElement();
	my %originators = personnelCreate('associatedParty');
	$doc .= personnelList( \%originators );

	$doc .= pubElement();
	$doc .= abstractElement();

	#    $doc .= setDisplayType('dataset');
	$doc .= keywordElement();
	$doc .= distributionElement();
	$doc .= coverageElement();
	$doc .= contactElement();
	$doc .= methodsElement();
	my %fileData = allFileData();
	$doc .= entityElement( \%fileData );
	$doc .= datasetEnd();
	$doc .= EMLEnd();
}

# EML document creation functions

sub personnelCreate {

	# passed parameter defines default role for individuals
	my $defaultRole = shift;

	# element name => objects of that type
	my %orig = (
		'creator'          => [],
		'metadataProvider' => [],
		'publisher'        => [],
		$defaultRole       => [],
	);

	# form name => EML element name
	my %roles = (
		'Originator'        => 'creator',
		'Metadata Provider' => 'metadataProvider',
		'Publisher'         => 'publisher',
	);

	push(
		@{ $orig{'metadataProvider'} },
		[ $FORM::providerGivenName, $FORM::providerSurName ]
	);

	# Additional originators
	foreach my $origName ( param() ) {
		my $origNum = $origName;
		$origNum =~ s/origNamelast//;   # get the index of the parameter 0 to 10
		if ( $origNum =~ /^([0-9]+)$/ ) {

			# do not generate EML for empty originator fields
			if ( hasContent( param( "origNamefirst" . $origNum ) ) ) {
				my $first    = normalize( param( "origNamefirst" . $origNum ) );
				my $last     = normalize( param( "origNamelast" . $origNum ) );
				my $origRole = param( "origRole" . $origNum );
				my $roleName = $roles{$origRole};
				if ( !hasContent($roleName) ) {
					$roleName = $defaultRole;
				}

				push( @{ $orig{$roleName} }, [ $first, $last, $origRole ] );
			}
		}
	}
	return %orig;
}

sub personnelList {
	my ( $orig, $type ) = @_;
	my %orig = %$orig;

	my $elemList = "";
	foreach my $role ( keys %orig ) {
		foreach my $v ( @{ $orig->{$role} } ) {
			my ( $first, $last, $origRole ) = @$v;
			my $elem = "<individualName>\n";
			$elem .= "<givenName>" . normalize($first) . "</givenName>\n";
			$elem .= "<surName>" . normalize($last) . "</surName>\n";
			$elem .= "</individualName>\n";

			if ( ( $role eq 'personnel' ) && ($FORM::origNameOrgContact) ) {
				$elem .=
"<organizationName>$FORM::origNameOrgContact</organizationName>\n";
			}

			if ( ( $role eq 'personnel' ) || ( $role eq 'associatedParty' ) ) {
				my $roleElem = $role;
				if ( hasContent($origRole) ) {
					$roleElem = $origRole;
				}
				$elem .= "<role>" . normalize($roleElem) . "</role>\n";
			}
			$elemList .= "<$role>$elem</$role>\n";
		}
	}
	return $elemList;
}

sub entityElement() {
	my $entityObjects = shift;
	my %entityObjects = %$entityObjects;
	my $entityList    = "";

	my $access = fileAccessElement( \%entityObjects );
	while ( my ( $docid, $data ) = each(%entityObjects) ) {
		my $entityStub =
		  qq|<otherEntity id="$data->{'entityid'}" scope="document">
            <entityName>$data->{'fileName'}</entityName>
            <physical scope="document">
                <objectName>$data->{'fileName'}</objectName>
                <size>$data->{'fileSize'}</size>
                <authentication method="SHA1">$data->{'fileHash'}</authentication>
                <dataFormat>
                    <externallyDefinedFormat>
                        <formatName>$data->{'contentType'}</formatName>
                    </externallyDefinedFormat>
                </dataFormat>
                <distribution id="$data->{'distribid'}" scope="document">
                    <online>
                        <url function="download">$data->{'url'}</url>
                    </online>
                    $access
                </distribution>
            </physical>
            <entityType>$data->{'entityType'}</entityType>
    </otherEntity>
|;
		$entityList .= $entityStub;
	}

	return $entityList;
}

sub fileAccessElement() {
	my $entityObjects = shift;
	my %entityObjects = %$entityObjects;
	my $userAccess    = allowElement( getUsername(), 'all' );
	my $skinAccess    = allowElement( $adminUsername, 'all' );
	my $accessList    = "";

	# form name => EML permission; roles akin to Apache model
	my %accessRoles = (
		'public'  => 'allow',
		'private' => 'deny',
	);

	while ( my ( $docid, $data ) = each(%entityObjects) ) {
		my $defaultAccess = $accessRoles{ $data->{'filePerm'} };
		$accessList = qq|
                <access authSystem="knb" order="allowFirst">
                    $skinAccess
                    $userAccess 
                    <$defaultAccess>
                        <principal>public</principal>
                        <permission>read</permission>
                    </$defaultAccess>
                </access>
    |;
	}

	return $accessList;
}

sub keywordElement() {
	my %kwSet, my $kwList = (), "";

	# process the standard keywords
	foreach my $kwName ( param() ) {
		my $kwNum = $kwName;
		$kwNum =~ s/keyword//;    # get the index of the parameter 0, ..., 10
		if ( $kwNum =~ /^([0-9]+)$/ ) {

	   # don't generate xml for empty keyword fields
	   # don't generate taxonomic keyword fields, those go in taxonomic coverage
			if ( hasContent( param($kwName) ) ) {
				my $kwType = param( "kwType" . $kwNum );
				my $kwTh   = param( "kwTh" . $kwNum );
				my $kw     = param( "keyword" . $kwNum );

				push( @{ $kwSet{$kwTh} }, [ $kw, $kwType ] );
			}
		}
	}

	# output keyword lists
	while ( ( my $thesaurus, my $a_ref ) = each %kwSet ) {
		my $kwElem .= "<keywordSet>\n";

		foreach my $x (@$a_ref) {
			my ( $kw, $kwType ) = @$x;
			$kwElem .= "<keyword ";
			if ( hasContent($kwType) && $kwType != "None" ) {
				$kwElem .= "keywordType=\"" . lc($kwType) . "\"";
			}
			$kwElem .= ">$kw</keyword>\n";
		}

		$kwElem .=
		    "<keywordThesaurus>"
		  . normalize($thesaurus)
		  . "</keywordThesaurus>\n";
		$kwElem .= "</keywordSet>\n";
		$kwList .= $kwElem;
	}
	return $kwList;
}

sub EMLStart() {
	my $gmt = gmtime($now);
	my $doc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

	$doc .= qq|<eml:eml
      packageId="docid" system="knb"
      xmlns:eml="eml://ecoinformatics.org/eml-2.1.1"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:ds="eml://ecoinformatics.org/dataset-2.1.1"
      xmlns:stmml="http://www.xml-cml.org/schema/stmml-1.1"
      xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.1 eml.xsd">
|;

	$doc .= "<!-- Person who filled in the catalog entry form: ";
	$doc .=
	    normalize($FORM::providerGivenName) . " "
	  . normalize($FORM::providerSurName)
	  . " -->\n";
	$doc .= "<!-- Form filled out at $gmt GMT -->\n";
	return $doc;
}

sub EMLEnd() {
	return "</eml:eml>\n";
}

sub datasetStart() {
	my $dataset = "<dataset>\n";

	if ( hasContent($FORM::identifier) ) {
		$dataset .= "<alternateIdentifier system=\"$FORM::site\">";
		$dataset .= normalize($FORM::identifier) . "</alternateIdentifier>\n";
	}
	return $dataset;
}

sub datasetEnd() {
	return "</dataset>\n";
}

sub setDisplayType {
	my $kwDisp = shift;
	if ( !$kwDisp ) {
		$kwDisp = 'dataset';
	}
	my $kw = "<keywordSet>\n";
	$kw .= "  <keyword>$kwDisp</keyword>\n";
	$kw .= "  <keywordThesaurus>EMLDisplayType</keywordThesaurus>\n";
	$kw .= "</keywordSet>\n";
	return $kw;
}

sub pubElement() {

	#add the publication Date to the eml document.
	my ( $Day, $Month, $Year ) = (localtime)[ 3, 4, 5 ];

	return
	    "<pubDate>"
	  . sprintf( "%03d-%02d-%02d", $Year + 1900, $Month + 1, $Day )
	  . "</pubDate>\n";
}

sub titleElement() {
	if ( hasContent($FORM::title) ) {
		return "<title>" . normalize($FORM::title) . "</title>\n";
	}
}

sub abstractElement() {
	return
	    "<abstract>\n<para>"
	  . normalize($FORM::abstract)
	  . "</para>\n</abstract>\n";
}

sub creatorElement() {
	my $creators;
	if ( $skinName eq 'nceas' ) {
		for ( my $i = 0 ; $i < scalar(@FORM::wg) ; $i++ ) {
			$creators .= creatorNode( $FORM::wg[$i] );
		}
	}
	else {
		$creators .= creatorNode($FORM::site);
	}
	# only use configured organization for certain skins
	if ( $skinName ne 'knb' && $skinName ne 'metacatui' ) {
		$creators .= creatorNode();
	}

	return $creators;
}

sub creatorNode {
	my $org     = shift;
	my $content = "";

	if ( !hasContent($org) ) {
		$content = $config->{'organization'};
	}
	else {
		$content = $org;
	}

	return
	    "<creator>\n<organizationName>"
	  . normalize($content)
	  . "</organizationName>\n</creator>\n";
}

sub creatorNameElement {
	my $role = shift;
	my $creator;
	if ( !hasContent($role) ) {
		$role = "creator";
	}

	$creator .= "<individualName>\n";
	$creator .=
	  "<givenName>" . normalize($FORM::origNamefirst0) . "</givenName>\n";
	$creator .= "<surName>" . normalize($FORM::origNamelast0) . "</surName>\n";
	$creator .= "</individualName>\n";

	return "<$role>\n$creator</$role>\n";
}

sub methodsElement() {
	my $methods = "";
	if (   ( hasContent($FORM::methodTitle) )
		|| scalar(@FORM::methodsPara) > 0
		|| ( $FORM::methodPara[0] ne "" ) )
	{
		$methods = "<methods><methodStep><description><section>\n";
		if ( hasContent($FORM::methodTitle) ) {
			$methods .=
			  "<title>" . normalize($FORM::methodTitle) . "</title>\n";
		}
		for ( my $i = 0 ; $i < scalar(@FORM::methodPara) ; $i++ ) {
			$methods .=
			  "<para>" . normalize( $FORM::methodPara[$i] ) . "</para>\n";
		}
		$methods .= "</section></description></methodStep>\n";
		if ( hasContent($FORM::studyExtentDescription) ) {
			$methods .= "<sampling><studyExtent><description>\n";
			$methods .=
			  "<para>" . normalize($FORM::studyExtentDescription) . "</para>\n";
			$methods .= "</description></studyExtent>\n";
			$methods .= "<samplingDescription>\n";
			$methods .=
			  "<para>" . normalize($FORM::samplingDescription) . "</para>\n";
			$methods .= "</samplingDescription>\n";
			$methods .= "</sampling>\n";
		}
		$methods .= "</methods>\n";
	}
	return $methods;
}

sub creatorContactElement() {
	my $cont = "";

	$cont .= "<individualName>\n";
	$cont .=
	  "<givenName>" . normalize($FORM::origNamefirst0) . "</givenName>\n";
	$cont .=
	  "<surName>" . normalize($FORM::origNamelast0) . "</surName>\n";
	$cont .= "</individualName>\n";

	if ( hasContent($FORM::origNameOrg) ) {
		$cont .=
		    "<organizationName>"
		  . normalize($FORM::origNameOrg)
		  . "</organizationName>\n";
	}

	if (   hasContent($FORM::origDelivery)
		|| hasContent($FORM::origCity)
		|| hasContent($FORM::origState)
		|| hasContent($FORM::origStateOther)
		|| hasContent($FORM::origZIP)
		|| hasContent($FORM::origCountry) )
	{
		$cont .= "<address>\n";
		if ( hasContent($FORM::origDelivery) ) {
			$cont .= "<deliveryPoint>" . normalize($FORM::origDelivery);
			$cont .= "</deliveryPoint>\n";
		}
		if ( hasContent($FORM::origCity) ) {
			$cont .= "<city>" . normalize($FORM::origCity) . "</city>\n";
		}
		if ( hasContent($FORM::origState)
			&& ( $FORM::origState !~ /select state/i ) )
		{
			$cont .=
			  "<administrativeArea>" . normalize($FORM::origState);
			$cont .= "</administrativeArea>\n";
		}
		elsif ( hasContent($FORM::origStateOther) ) {
			$cont .=
			  "<administrativeArea>" . normalize($FORM::origStateOther);
			$cont .= "</administrativeArea>\n";
		}
		if ( hasContent($FORM::origZIP) ) {
			$cont .=
			    "<postalCode>"
			  . normalize($FORM::origZIP)
			  . "</postalCode>\n";
		}
		if ( hasContent($FORM::origCountry) ) {
			$cont .=
			    "<country>"
			  . normalize($FORM::origCountry)
			  . "</country>\n";
		}
		$cont .= "</address>\n";
	}
	if ( hasContent($FORM::origPhone) ) {
		$cont .= "<phone>" . normalize($FORM::origPhone) . "</phone>\n";
	}
	if ( hasContent($FORM::origFAXContact) ) {
		$cont .=
		    "<phone phonetype=\"Fax\">"
		  . normalize($FORM::origFAXContact)
		  . "</phone>\n";
	}
	if ( hasContent($FORM::origEmail) ) {
		$cont .= "<electronicMailAddress>" . normalize($FORM::origEmail);
		$cont .= "</electronicMailAddress>\n";
	}

	return "<creator>\n$cont</creator>\n";
}

sub contactElement() {
	my $role = shift;
	my $cont = "";

	if ( !hasContent($role) ) {
		$role = 'contact';
	}

	$cont .= "<individualName>\n";
	$cont .=
	  "<givenName>" . normalize($FORM::origNamefirstContact) . "</givenName>\n";
	$cont .=
	  "<surName>" . normalize($FORM::origNamelastContact) . "</surName>\n";
	$cont .= "</individualName>\n";

	if ( hasContent($FORM::origNameOrgContact) ) {
		$cont .=
		    "<organizationName>"
		  . normalize($FORM::origNameOrgContact)
		  . "</organizationName>\n";
	}

	if (   hasContent($FORM::origDeliveryContact)
		|| hasContent($FORM::origCityContact)
		|| hasContent($FORM::origStateContact)
		|| hasContent($FORM::origStateOtherContact)
		|| hasContent($FORM::origZIPContact)
		|| hasContent($FORM::origCountryContact) )
	{
		$cont .= "<address>\n";
		if ( hasContent($FORM::origDeliveryContact) ) {
			$cont .= "<deliveryPoint>" . normalize($FORM::origDeliveryContact);
			$cont .= "</deliveryPoint>\n";
		}
		if ( hasContent($FORM::origCityContact) ) {
			$cont .= "<city>" . normalize($FORM::origCityContact) . "</city>\n";
		}
		if ( hasContent($FORM::origStateContact)
			&& ( $FORM::origStateContact !~ /select state/i ) )
		{
			$cont .=
			  "<administrativeArea>" . normalize($FORM::origStateContact);
			$cont .= "</administrativeArea>\n";
		}
		elsif ( hasContent($FORM::origStateOtherContact) ) {
			$cont .=
			  "<administrativeArea>" . normalize($FORM::origStateOtherContact);
			$cont .= "</administrativeArea>\n";
		}
		if ( hasContent($FORM::origZIPContact) ) {
			$cont .=
			    "<postalCode>"
			  . normalize($FORM::origZIPContact)
			  . "</postalCode>\n";
		}
		if ( hasContent($FORM::origCountryContact) ) {
			$cont .=
			    "<country>"
			  . normalize($FORM::origCountryContact)
			  . "</country>\n";
		}
		$cont .= "</address>\n";
	}
	if ( hasContent($FORM::origPhoneContact) ) {
		$cont .= "<phone>" . normalize($FORM::origPhoneContact) . "</phone>\n";
	}
	if ( hasContent($FORM::origFAXContact) ) {
		$cont .=
		    "<phone phonetype=\"Fax\">"
		  . normalize($FORM::origFAXContact)
		  . "</phone>\n";
	}
	if ( hasContent($FORM::origEmailContact) ) {
		$cont .= "<electronicMailAddress>" . normalize($FORM::origEmailContact);
		$cont .= "</electronicMailAddress>\n";
	}

	return "<$role>\n$cont</$role>\n";
}

sub coverageElement() {
	my $cov = "";

	# temporal coverage
	if ( hasContent($FORM::endingYear) ) {
		$cov .= "<temporalCoverage>\n";
		$cov .= "<rangeOfDates>\n";
		if ( hasContent($FORM::beginningMonth) ) {
			my $month = (
				"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
				"JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
			)[ $FORM::beginningMonth - 1 ];
			if ( hasContent($FORM::beginningDay) ) {
				$cov .= "<beginDate>\n";
				$cov .= "<calendarDate>";
				$cov .=
				    normalize($FORM::beginningYear) . "-"
				  . normalize($FORM::beginningMonth) . "-"
				  . normalize($FORM::beginningDay);
				$cov .= "</calendarDate>\n";
				$cov .= "</beginDate>\n";
			}
			else {
				$cov .= "<beginDate>\n";
				$cov .= "<calendarDate>";
				$cov .=
				    normalize($FORM::beginningYear) . "-"
				  . normalize($FORM::beginningMonth) . "-01";
				$cov .= "</calendarDate>\n";
				$cov .= "</beginDate>\n";
			}
		}
		else {
			$cov .= "<beginDate>\n";
			$cov .= "<calendarDate>";
			$cov .= normalize($FORM::beginningYear);
			$cov .= "</calendarDate>\n";
			$cov .= "</beginDate>\n";
		}

		if ( hasContent($FORM::endingMonth) ) {
			my $month = (
				"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
				"JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
			)[ $FORM::endingMonth - 1 ];

			if ( hasContent($FORM::endingDay) ) {
				$cov .= "<endDate>\n";
				$cov .= "<calendarDate>";
				$cov .=
				    normalize($FORM::endingYear) . "-"
				  . normalize($FORM::endingMonth) . "-"
				  . normalize($FORM::endingDay);
				$cov .= "</calendarDate>\n";
				$cov .= "</endDate>\n";
			}
			else {
				$cov .= "<endDate>\n";
				$cov .= "<calendarDate>";
				$cov .=
				    normalize($FORM::endingYear) . "-"
				  . normalize($FORM::endingMonth) . "-01";
				$cov .= "</calendarDate>\n";
				$cov .= "</endDate>\n";
			}
		}
		else {
			$cov .= "<endDate>\n";
			$cov .= "<calendarDate>";
			$cov .= normalize($FORM::endingYear);
			$cov .= "</calendarDate>\n";
			$cov .= "</endDate>\n";
		}
		$cov .= "</rangeOfDates>\n";
		$cov .= "</temporalCoverage>\n";
	}
	else {
		if ( hasContent($FORM::beginningYear) ) {
			$cov .= "<temporalCoverage>\n";
			$cov .= "<singleDateTime>\n";
			if ( hasContent($FORM::beginningMonth) ) {
				my $month = (
					"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
					"JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
				)[ $FORM::beginningMonth - 1 ];

				if ( hasContent($FORM::beginningDay) ) {
					$cov .= "<calendarDate>";
					$cov .=
					    normalize($FORM::beginningYear) . "-"
					  . normalize($FORM::beginningMonth) . "-"
					  . normalize($FORM::beginningDay);
					$cov .= "</calendarDate>\n";
				}
				else {
					$cov .= "<calendarDate>";
					$cov .=
					    normalize($FORM::beginningYear) . "-"
					  . normalize($FORM::beginningMonth) . "-01";
					$cov .= "</calendarDate>\n";
				}
			}
			else {
				$cov .= "<calendarDate>";
				$cov .= normalize($FORM::beginningYear);
				$cov .= "</calendarDate>\n";
			}
			$cov .= "</singleDateTime>\n";
			$cov .= "</temporalCoverage>\n";
		}
	}

	# geographic coverage
	if (
		hasContent($FORM::geogdesc)
		|| (   hasContent($FORM::latDeg1)
			&& $FORM::latDeg1 < 91
			&& $FORM::latDeg1 > -1
			&& hasContent($FORM::longDeg1)
			&& $FORM::longDeg1 < 181
			&& $FORM::longDeg1 > -1 )
	  )
	{
		$cov .= "<geographicCoverage>\n";

		if ( hasContent($FORM::geogdesc) ) {
			$cov .=
			    "<geographicDescription>"
			  . normalize($FORM::geogdesc)
			  . "</geographicDescription>\n";
		}

		if (   $latDeg1 < 91
			&& $latDeg1 > -1
			&& $longDeg1 < 181
			&& $longDeg1 > -1 )
		{
			$cov .= "<boundingCoordinates>\n";

		  # if the second latitude is missing, then set the second lat/long pair
		  # equal to the first this makes a point appear like a rectangle
			if (
				$FORM::useSiteCoord
				|| (   $FORM::latDeg2 eq ""
					&& $FORM::latMin2 eq ""
					&& $FORM::latSec2 eq "" )
			  )
			{

				$latDeg2      = $latDeg1;
				$latMin2      = $latMin1;
				$latSec2      = $latSec1;
				$hemisphLat2  = $hemisphLat1;
				$longDeg2     = $longDeg1;
				$longMin2     = $longMin1;
				$longSec2     = $longSec1;
				$hemisphLong2 = $hemisphLong1;
			}
			else {
				$latDeg2      = $FORM::latDeg2;
				$latMin2      = $FORM::latMin2;
				$latSec2      = $FORM::latSec2;
				$hemisphLat2  = $FORM::hemisphLat2;
				$longDeg2     = $FORM::longDeg2;
				$longMin2     = $FORM::longMin2;
				$longSec2     = $FORM::longSec2;
				$hemisphLong2 = $FORM::hemisphLong2;
			}

			my $hemisph;
			$hemisph = ( $hemisphLong1 eq "W" ) ? -1 : 1;
			$cov .= "<westBoundingCoordinate>";
			my $var =
			  $hemisph * ( $longDeg1 + ( 60 * $longMin1 + $longSec1 ) / 3600 );
			$cov .= sprintf( "%.4f", $var );
			$cov .= "</westBoundingCoordinate>\n";

			$hemisph = ( $hemisphLong2 eq "W" ) ? -1 : 1;
			$cov .= "<eastBoundingCoordinate>";
			$var =
			  $hemisph * ( $longDeg2 + ( 60 * $longMin2 + $longSec2 ) / 3600 );
			$cov .= sprintf( "%.4f", $var );
			$cov .= "</eastBoundingCoordinate>\n";

			$hemisph = ( $hemisphLat1 eq "S" ) ? -1 : 1;
			$cov .= "<northBoundingCoordinate>";
			$var =
			  $hemisph * ( $latDeg1 + ( 60 * $latMin1 + $latSec1 ) / 3600 );
			$cov .= sprintf( "%.4f", $var );
			$cov .= "</northBoundingCoordinate>\n";

			$hemisph = ( $hemisphLat2 eq "S" ) ? -1 : 1;
			$cov .= "<southBoundingCoordinate>";
			$var =
			  $hemisph * ( $latDeg2 + ( 60 * $latMin2 + $latSec2 ) / 3600 );
			$cov .= sprintf( "%.4f", $var );
			$cov .= "</southBoundingCoordinate>\n";

			$cov .= "</boundingCoordinates>\n";
		}
		$cov .= "</geographicCoverage>\n";
	}

	# taxonomic coverage
	my $foundFirstTaxon = 0;
	foreach my $trn ( param() ) {
		if ( $trn =~ /taxonRankName/ ) {
			my $taxIndex = $trn;
			$taxIndex =~
			  s/taxonRankName//;    # get the index of the parameter 0, ..., 10
			my $trv = "taxonRankValue" . $taxIndex;
			if ( $taxIndex =~ /[0-9]+/ ) {
				if ( hasContent( param($trn) ) && hasContent( param($trv) ) ) {
					if ( !$foundFirstTaxon ) {
						$cov .= "<taxonomicCoverage>\n";
						$foundFirstTaxon = 1;
						if ( hasContent($FORM::taxaAuth) ) {
							$cov .=
							    "<generalTaxonomicCoverage>"
							  . normalize($FORM::taxaAuth)
							  . "</generalTaxonomicCoverage>\n";
						}
					}
					$cov .= "<taxonomicClassification>\n";
					$cov .=
					    "  <taxonRankName>"
					  . normalize( param($trn) )
					  . "</taxonRankName>\n";
					$cov .=
					    "  <taxonRankValue>"
					  . normalize( param($trv) )
					  . "</taxonRankValue>\n";
					$cov .= "</taxonomicClassification>\n";
				}
			}
		}
	}

	if ($foundFirstTaxon) {
		$cov .= "</taxonomicCoverage>\n";
	}

	return "<coverage>\n$cov</coverage>\n";
}

sub distributionElement() {
	my $dist = "";
	if ( hasContent($FORM::addComments) ) {
		$dist .= "<additionalInfo>\n";
		$dist .= "<para>" . normalize($FORM::addComments) . "</para>\n";
		$dist .= "</additionalInfo>\n";
	}

	if (   hasContent($FORM::useConstraints)
		|| hasContent($FORM::useConstraintsOther) )
	{
		$dist .= "<intellectualRights>\n";
		if ( hasContent($FORM::useConstraints) ) {
			$dist .= "<para>" . normalize($FORM::useConstraints) . "</para>\n";
		}
		if ( hasContent($FORM::useConstraintsOther) ) {
			$dist .=
			  "<para>" . normalize($FORM::useConstraintsOther) . "</para>\n";
		}
		$dist .= "</intellectualRights>\n";
	}

	if ( hasContent($FORM::url) ) {
		$dist .= "<distribution>\n";
		$dist .= "<online>\n";
		$dist .= "<url>" . normalize($FORM::url) . "</url>\n";
		$dist .= "</online>\n";
		$dist .= "</distribution>\n";
	}

	$dist .= "<distribution>\n";
	$dist .= "<offline>\n";
	$dist .=
	    "<mediumName>"
	  . normalize($FORM::dataMedium) . " "
	  . normalize($FORM::dataMediumOther);
	$dist .= "</mediumName>\n";
	$dist .= "</offline>\n";
	$dist .= "</distribution>\n";
	return $dist;
}

sub accessElement {
	my $public = shift;
	if ( !$public ) {
		$public = $config->{'publicReadable'};
	}

	my $access = "";

	$access .= "<access authSystem=\"knb\" order=\"allowFirst\">\n";
	$access .= allowElement( $adminUsername, 'all' );

	if ( $moderators eq '' ) {
		$access .= allowElement( getUsername(), 'all' );
	}
	else {
		foreach ( split( ":", $moderators ) ) {
			$access .= allowElement( $_, 'all' );
		}

		$access .= allowElement( getUsername(), 'read', 'write' );
	}

	if ( $public eq "true" ) {
		$access .= allowElement( 'public', 'read' );
	}
	$access .= "</access>\n";
	return $access;
}

sub allowElement {
	my $principal   = shift;
	my @permissions = @_;

	my $allowElem = "<allow>\n" . "  <principal>$principal</principal>";
	foreach my $perm (@permissions) {
		$allowElem .= "<permission>$perm</permission>\n";
	}
	$allowElem .= "</allow>\n";
	return $allowElem;
}

sub getUsername() {
	my $username = '';

	if ( $FORM::username ne '' ) {
		$username =
		  "uid=$FORM::username,o=$FORM::organization,dc=ecoinformatics,dc=org";
	}
	else {
		my $session = CGI::Session->load();
		if ( !$session->is_empty ) {
			$username = $session->param("username");
		}
	}

	return $username;
}

sub readDocumentFromMetacat() {

	#debug("read the document from metacat");
	my $docid = $FORM::docid;

	# create metacat instance
	my $metacat = Metacat->new($metacatUrl);
	my $httpMessage;
	my $doc;
	my $xmldoc;
	my $findType;
	my $parser = XML::LibXML->new();
	my @fileArray;
	my $pushDoc;
	my $alreadyInArray;
	my $node;
	my $response;
	my $element;
	my $tempfile;

	my ( $username, $password ) = getCredentials();
	$metacat->login( $username, $password );

	$httpMessage = $metacat->read($docid);
	$doc         = $httpMessage->content();
	$xmldoc      = $parser->parse_string($doc);

	if ( $xmldoc eq "" ) {
		$error = "Error in parsing the eml document";
		push( @errorMessages, $error );
	}
	elsif ( $doc =~ /<error/ ) {
		if ( $doc =~ /public/ ) {
			$error = "Error in reading the eml document. Please check if you are logged in.";
			push( @errorMessages, $error );
		}
		else {
			$error = "Error in reading the eml document. Please check if you have access to read the document";
			push( @errorMessages, $error );
		}
	}
	else {
		$findType = $xmldoc->findnodes('//dataset/identifier');
		if ( $findType->size() > 0 ) {

			# This is a eml beta6 document
			# Read the documents mentioned in triples also
			push( @errorMessages, "EML2 beta6 support deprecated." );
		}
	}
	return $xmldoc;
}

################################################################################
#
# read the eml document and send back a form with values filled in.
#
################################################################################
sub modifyData {
	#debug("in modifyData");

	my $xmldoc = readDocumentFromMetacat();
	if ( !scalar(@errorMessages) ) {
		getFormValuesFromEml2($xmldoc);
	}

	if ( scalar(@errorMessages) ) {

		# if any errors, print them in the response template
		$$templateVars{'status'}        = 'failure_no_resubmit';
		$$templateVars{'errorMessages'} = \@errorMessages;
		$error                          = 1;
		$$templateVars{'function'}      = "modification";
		$$templateVars{'section'}       = "Modification Status";
		$template->process( $templates->{'response'}, $templateVars );
	}
	else {
		$$templateVars{'form'} = 're_entry';
		$template->process( $templates->{'entry'}, $templateVars );
	}
}

################################################################################
#
# Convert EML 2.0.x documents into 2.1.0 using an XSLT transform
#
################################################################################
sub transformEmlTo210 {
	my $xmldoc = shift;
	my $xslt   = XML::LibXSLT->new();
	my $results;
	my $stylesheet;
	my $resultsheet;

	my $transform = "$styleCommonPath/conversions/eml201to210.xsl";
	$stylesheet = $xslt->parse_stylesheet_file($transform);
	$results    = $stylesheet->transform($xmldoc);

	# if debugging is enabled, dump the transformed document to disk
	debugDoc( $stylesheet->output_string($results) );

	return $results;
}

################################################################################
#
# transform the EML document if necessary, otherwise leave unadultered
#
################################################################################
sub transformEml {
	my $doc = shift;

	# get the document namespace
	my $root  = $doc->getDocumentElement();
	my $emlns = $root->lookupNamespaceURI("eml");

	if ( $emlns =~ 'eml-2.0' ) {
		debug("Translation: Upgrading a 2.0.x doc to 2.1.0");
		$doc = transformEmlTo210($doc);
	}
	elsif ( $emlns =~ 'eml-2.1' ) {
		debug("Translation: Found a 2.1.x doc.");
	}
	else {
		$error = "Unrecognized document type!";
		debug("Translation: $error");
		push( @errorMessages, $error . "\n" );
	}
	return $doc;
}

################################################################################
#
# Parse an EML 2.x file and extract the metadata into perl variables for
# processing and returning to the template processor
#
################################################################################
sub getFormValuesFromEml2 {
	#debug("getting form values from eml 2");

	my $doc = shift;
	my $results;
	my $error;
	my $node;
	my $tempResult;
	my $tempNode;
	my $aoCount = 0;
	my $foundDSO;

	# set variable values
	$$templateVars{'modules'}  = $modules;
	$$templateVars{'required'} = $required;
	$$templateVars{'show'}     = $show;
	$$templateVars{'site'}     = $config->{'site'};

	# perform any required transformation
	$doc = transformEml($doc);

	# find out the tag <alternateIdentifier>.
	$results = $doc->findnodes('//dataset/alternateIdentifier');
	if ( $results->size() > 1 ) {
		errMoreThanOne("alternateIdentifier");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			$$templateVars{'identifier'} =
			  findValue( $node, '../alternateIdentifier' );
		}
	}

	# find out the tag <title>.
	$results = $doc->findnodes('//dataset/title');
	if ( $results->size() > 1 ) {
		errMoreThanOne("title");
	}
	elsif ( $results->size() < 1 ) {
		$error = "Following tag not found: title. Please use Morpho to edit this document";
		push( @errorMessages, $error . "\n" );
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			$$templateVars{'title'} = findValue( $node, '../title' );
		}
	}

	# find out the tag <creator>.
	$results = $doc->findnodes('//dataset/creator/individualName');
	debug( "Creators: " . $results->size() );
	foreach $node ( $results->get_nodelist ) {
		dontOccur(
			$node,
			"../positionName|../onlineURL|../userId",
			"positionName, onlineURL, userId"
		);

		dontOccur( $node, "./salutation", "salutation" );

		debug("Checking a creator in loop 1...");
		$tempResult = $node->findnodes(
			'../address|../phone|../electronicmailAddress|../organizationName');
		if ( $tempResult->size > 0 ) {
			if ( $foundDSO == 0 ) {
				$foundDSO = 1;

				debug("Recording a creator in loop 1...");
				$$templateVars{'origNamefirst0'} =
				  findValue( $node, 'givenName' );
				$$templateVars{'origNamelast0'} = findValue( $node, 'surName' );

				my $tempResult2 = $node->findnodes('../address');
				if ( $tempResult2->size > 1 ) {
					errMoreThanOne("address");
				}
				else {
					foreach my $tempNode2 ( $tempResult2->get_nodelist ) {
						$$templateVars{'origDelivery'} =
						  findValue( $tempNode2, 'deliveryPoint' );
						$$templateVars{'origCity'} =
						  findValue( $tempNode2, 'city' );
						$$templateVars{'origState'} =
						  findValue( $tempNode2, 'administrativeArea' );
						$$templateVars{'origZIP'} =
						  findValue( $tempNode2, 'postalCode' );
						$$templateVars{'origCountry'} =
						  findValue( $tempNode2, 'country' );
					}
				}

				my $tempResult3 = $node->findnodes('../phone');
				if ( $tempResult3->size > 2 ) {
					errMoreThanN("phone");
				}
				else {
					foreach my $tempNode2 ( $tempResult3->get_nodelist ) {
						if ( $tempNode2->hasAttributes() ) {
							my @attlist = $tempNode2->attributes();
							if ( $attlist[0]->value eq "Fax" ) {
								$$templateVars{'origFAX'} =
								  $tempNode2->textContent();
							}
							else {
								$$templateVars{'origPhone'} =
								  $tempNode2->textContent();
							}
						}
						else {
							$$templateVars{'origPhone'} =
							  $tempNode2->textContent();
						}
					}
				}
				$$templateVars{'origEmail'} =
				  findValue( $node, '../electronicMailAddress' );
				$$templateVars{'origNameOrg'} =
				  findValue( $node, '../organizationName' );
			}
			else {
				errMoreThanN("address, phone and electronicMailAddress");
			}
		}
	}
	foreach $node ( $results->get_nodelist ) {
		debug("Checking a creator in loop 2...");
		$tempResult = $node->findnodes(
			'../address|../phone|../electronicmailAddress|../organizationName');
		if ( $tempResult->size == 0 ) {
			if ( $foundDSO == 0 ) {
				debug("Recording a creator in loop 2 block A...");
				$foundDSO = 1;
				$$templateVars{'origNamefirst0'} =
				  findValue( $node, 'givenName' );
				$$templateVars{'origNamelast0'} = findValue( $node, 'surName' );
				$$templateVars{'origNameOrg'} =
				  findValue( $node, '../organizationName' );
			}
			else {
				debug("Recording a creator in loop 2 block B...");
				$$templateVars{"origNamefirst$aoCount"} =
				  findValue( $node, './givenName' );
				$$templateVars{"origNamelast$aoCount"} =
				  findValue( $node, './surName' );
				$$templateVars{"origRole$aoCount"} = "Originator";
				$aoCount++;
			}
		}
	}

	$results = $doc->findnodes('//dataset/creator/organizationName');
	my $wgroups = $doc->findnodes(
		"//dataset/creator/organizationName[contains(text(),'(NCEAS ')]");
	debug( "Number Org: " . $results->size() );
	debug( " Number WG: " . $wgroups->size() );
	if ( $results->size() - $wgroups->size() > 3 ) {
		errMoreThanN("creator/organizationName");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			my $tempValue = findValue( $node, '../organizationName' );
			$tempResult = $node->findnodes('../individualName');
			if (   $tempResult->size == 0
				&& $tempValue ne $config->{'organization'} )
			{
				$$templateVars{'site'} = $tempValue;
			}
		}
		if ( $skinName eq 'nceas' ) {
			my @wg;
			foreach $node ( $results->get_nodelist ) {
				my $tempValue = findValue( $node, '../organizationName' );
				$wg[ scalar(@wg) ] = $tempValue;
			}
			my $projects = getProjectList($properties);
			$$templateVars{'projects'} = $projects;
			$$templateVars{'wg'}       = \@wg;
		}
	}

	$results = $doc->findnodes('//dataset/metadataProvider');
	foreach $node ( $results->get_nodelist ) {
		dontOccur(
			$node,
"./organizationName|./positionName|./onlineURL|./userId|./electronicMailAddress|./phone|./address",
"organizationName, positionName, onlineURL, userId, electronicMailAddress, phone, address in metadataProvider"
		);

		$tempResult = $node->findnodes('./individualName');
		if ( $tempResult->size > 1 ) {
			errMoreThanOne("metadataProvider/indvidualName");
		}
		else {
			foreach $tempNode ( $tempResult->get_nodelist ) {
				if ( $$templateVars{'providerGivenName'} ne "" ) {
					$$templateVars{"origNamefirst$aoCount"} =
					  findValue( $tempNode, './givenName' );
					$$templateVars{"origNamelast$aoCount"} =
					  findValue( $tempNode, './surName' );
					$$templateVars{"origRole$aoCount"} = "Metadata Provider";
					$aoCount++;
				}
				else {
					$$templateVars{'providerGivenName'} =
					  findValue( $tempNode, './givenName' );
					$$templateVars{'providerSurName'} =
					  findValue( $tempNode, './surName' );
				}
			}
		}
	}

	$results = $doc->findnodes('//dataset/associatedParty');
	foreach $node ( $results->get_nodelist ) {
		dontOccur(
			$node,
"./organizationName|./positionName|./onlineURL|./userId|./electronicMailAddress|./phone|./address",
"organizationName, positionName, onlineURL, userId, electronicMailAddress, phone, address in associatedParty"
		);

		$tempResult = $node->findnodes('./individualName');
		if ( $tempResult->size > 1 ) {
			errMoreThanOne("associatedParty/indvidualName");
		}
		else {
			foreach $tempNode ( $tempResult->get_nodelist ) {
				$$templateVars{"origNamefirst$aoCount"} =
				  findValue( $tempNode, './givenName' );
				$$templateVars{"origNamelast$aoCount"} =
				  findValue( $tempNode, './surName' );
				$$templateVars{"origRole$aoCount"} =
				  findValue( $tempNode, '../role' );
				$aoCount++;
			}
		}
	}

	$results = $doc->findnodes('//dataset/publisher');

	#    if ($results->size() > 10) {
	#       errMoreThanN("publisher");
	#   } else {
	foreach $node ( $results->get_nodelist ) {
		dontOccur(
			$node,
			"./organizationName|./positionName|./onlineURL|./userId|./electronicMailAddress|./phone|./address",
			"organizationName, positionName, onlineURL, userId, electronicMailAddress, phone, address in associatedParty"
		);

		$tempResult = $node->findnodes('./individualName');
		if ( $tempResult->size > 1 ) {
			errMoreThanOne("publisher/indvidualName");
		}
		else {
			foreach $tempNode ( $tempResult->get_nodelist ) {
				$$templateVars{"origNamefirst$aoCount"} =
				  findValue( $tempNode, './givenName' );
				$$templateVars{"origNamelast$aoCount"} =
				  findValue( $tempNode, './surName' );
				$$templateVars{"origRole$aoCount"} = "Publisher";
				$aoCount++;
			}
		}
	}

	#  }

	#  if ($aoCount > 11) {
	#      errMoreThanN("Additional Originators");
	#   }

	$$templateVars{'aoCount'} = $aoCount;

	dontOccur( $doc, "./pubDate",  "pubDate" );
	dontOccur( $doc, "./language", "language" );
	dontOccur( $doc, "./series",   "series" );

	$results = $doc->findnodes('//dataset/abstract');
	if ( $results->size() > 1 ) {
		errMoreThanOne("abstract");
	}
	else {
		foreach my $node ( $results->get_nodelist ) {
			dontOccur( $node, "./section", "section" );
			$$templateVars{'abstract'} = findValueNoChild( $node, "para" );
		}
	}

	$results = $doc->findnodes('//dataset/keywordSet');

	my $count = 1;
	foreach $node ( $results->get_nodelist ) {
		my $thesaurus = findValue( $node, "keywordThesaurus" );
		$tempResult = $node->findnodes('./keyword');
		foreach $tempNode ( $tempResult->get_nodelist ) {
			$$templateVars{"keyword$count"} = $tempNode->textContent();
			if ( $tempNode->hasAttributes() ) {
				my @attlist = $tempNode->attributes();
				my $tmp     = $attlist[0]->value;
				$tmp =~
				  s/\b(\w)/\U$1/g;    # convert the first letter to upper case
				$$templateVars{"kwType$count"} = $tmp;
			}
			$$templateVars{"kwTh$count"} = $thesaurus;

 			#debug("Keyword Found: $count `" . $tempNode->textContent() . "`, $thesaurus");
			$count++;
		}
	}

	$$templateVars{'keyCount'} = $count;
	if ( $count > 1 ) {
		$$templateVars{'hasKeyword'} = "true";
	}

	$results = $doc->findnodes('//dataset/additionalInfo');
	if ( $results->size() > 1 ) {
		errMoreThanOne("additionalInfo");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			dontOccur( $node, "./section", "section" );
			$$templateVars{'addComments'} = findValueNoChild( $node, "para" );
		}
	}

	$$templateVars{'useConstraints'} = "";
	$results = $doc->findnodes('//dataset/intellectualRights');
	if ( $results->size() > 1 ) {
		errMoreThanOne("intellectualRights");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			dontOccur( $node, "./section", "section in intellectualRights" );

			$tempResult = $node->findnodes("para");
			if ( $tempResult->size > 2 ) {
				errMoreThanN("para");
			}
			else {
				foreach $tempNode ( $tempResult->get_nodelist ) {
					my $childNodes = $tempNode->childNodes;
					if ( $childNodes->size() > 1 ) {
						$error = "The tag para in intellectualRights has children which cannot be shown using the form. Please use Morpho to edit this document";
						push( @errorMessages, $error );
					}
					else {

					  #print $tempNode->nodeName().":".$tempNode->textContent();
					  #print "\n";
						if ( $$templateVars{'useConstraints'} eq "" ) {
							$$templateVars{'useConstraints'} =
							  $tempNode->textContent();
						}
						else {
							$$templateVars{'useConstraintsOther'} =
							  $tempNode->textContent();
						}
					}
				}
			}
		}
	}

	$results = $doc->findnodes('//dataset/distribution/online');
	if ( $results->size() > 1 ) {
		errMoreThanOne("distribution/online");
	}
	else {
		foreach my $tempNode ( $results->get_nodelist ) {
			$$templateVars{'url'} = findValue( $tempNode, "url" );
			dontOccur( $tempNode, "./connection",
				"/distribution/online/connection" );
			dontOccur( $tempNode, "./connectionDefinition",
				"/distribution/online/connectionDefinition" );
		}
	}

	$results = $doc->findnodes('//dataset/distribution/offline');
	if ( $results->size() > 1 ) {
		errMoreThanOne("distribution/online");
	}
	else {
		foreach my $tempNode ( $results->get_nodelist ) {
			my $temp = findValue( $tempNode, "mediumName" );
			if ( substr( $temp, 0, 5 ) eq "other" ) {
				$$templateVars{'dataMedium'} = substr( $temp, 0, 5 );
				$$templateVars{'dataMediumOther'} = substr( $temp, 6 );
			}
			else {
				$$templateVars{'dataMedium'} = $temp;
			}
			dontOccur( $tempNode, "./mediumDensity",
				"/distribution/offline/mediumDensity" );
			dontOccur( $tempNode, "./mediumDensityUnits",
				"/distribution/offline/mediumDensityUnits" );
			dontOccur( $tempNode, "./mediumVolume",
				"/distribution/offline/mediumVolume" );
			dontOccur( $tempNode, "./mediumFormat",
				"/distribution/offline/mediumFormat" );
			dontOccur( $tempNode, "./mediumNote",
				"/distribution/offline/mediumNote" );
		}
	}

	dontOccur( $doc, "./inline", "//dataset/distribution/inline" );

	$results = $doc->findnodes('//dataset/coverage');
	if ( $results->size() > 1 ) {
		errMoreThanOne("coverage");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			dontOccur(
				$node,
				"./temporalCoverage/rangeOfDates/beginDate/time|./temporalCoverage/rangeOfDates/beginDate/alternativeTimeScale|./temporalCoverage/rangeOfDates/endDate/time|./temporalCoverage/rangeOfDates/endDate/alternativeTimeScale|./taxonomicCoverage/taxonomicSystem|./taxonomicCoverage/taxonomicClassification/commonName|./taxonomicCoverage/taxonomicClassification/taxonomicClassification|./geographicCoverage/datasetGPolygon|./geographicCoverage/boundingCoordinates/boundingAltitudes",
				"temporalCoverage/rangeOfDates/beginDate/time, /temporalCoverage/rangeOfDates/beginDate/alternativeTimeScale, /temporalCoverage/rangeOfDates/endDate/time, /temporalCoverage/rangeOfDates/endDate/alternativeTimeScale, /taxonomicCoverage/taxonomicSystem, /taxonomicCoverage/taxonomicClassification/commonName, /taxonomicCoverage/taxonomicClassification/taxonomicClassification, /geographicCoverage/datasetGPolygon, /geographicCoverage/boundingCoordinates/boundingAltitudes"
			);

			$tempResult = $node->findnodes('./temporalCoverage');
			if ( $tempResult->size > 1 ) {
				errMoreThanOne("temporalCoverage");
			}
			else {
				foreach $tempNode ( $tempResult->get_nodelist ) {
					my $x;
					my $y;
					my $z;
					my $tempdate = findValue( $tempNode,
						"rangeOfDates/beginDate/calendarDate" );
					( $x, $y, $z ) = split( "-", $tempdate );
					$$templateVars{'beginningYear'}  = $x;
					$$templateVars{'beginningMonth'} = $y;
					$$templateVars{'beginningDay'}   = $z;

					$tempdate = findValue( $tempNode,
						"rangeOfDates/endDate/calendarDate" );
					( $x, $y, $z ) = split( "-", $tempdate );
					$$templateVars{'endingYear'}  = $x;
					$$templateVars{'endingMonth'} = $y;
					$$templateVars{'endingDay'}   = $z;

					$tempdate = "";
					$tempdate =
					  findValue( $tempNode, "singleDateTime/calendarDate" );
					if ( $tempdate ne "" ) {
						( $x, $y, $z ) = split( "-", $tempdate );
						$$templateVars{'beginningYear'}  = $x;
						$$templateVars{'beginningMonth'} = $y;
						$$templateVars{'beginningDay'}   = $z;
					}

					$$templateVars{'hasTemporal'} = "true";
				}
			}

			$tempResult = $node->findnodes('./geographicCoverage');
			if ( $tempResult->size > 1 ) {
				errMoreThanOne("geographicCoverage");
			}
			else {
				foreach $tempNode ( $tempResult->get_nodelist ) {
					my $geogdesc =
					  findValue( $tempNode, "geographicDescription" );
					debug("Geogdesc from xml is: $geogdesc");
					$$templateVars{'geogdesc'} = $geogdesc;
					my $coord = findValue( $tempNode,
						"boundingCoordinates/westBoundingCoordinate" );
					if ( $coord > 0 ) {

						#print "+";
						$$templateVars{'hemisphLong1'} = "E";
					}
					else {

						#print "-";
						eval( $coord = $coord * -1 );
						$$templateVars{'hemisphLong1'} = "W";
					}
					eval( $$templateVars{'longDeg1'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'longMin1'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'longSec1'} = int($coord) );

					$coord = findValue( $tempNode,
						"boundingCoordinates/southBoundingCoordinate" );
					if ( $coord > 0 ) {

						#print "+";
						$$templateVars{'hemisphLat2'} = "N";
					}
					else {

						#print "-";
						eval( $coord = $coord * -1 );
						$$templateVars{'hemisphLat2'} = "S";
					}
					eval( $$templateVars{'latDeg2'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'latMin2'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'latSec2'} = int($coord) );

					$coord = findValue( $tempNode,
						"boundingCoordinates/northBoundingCoordinate" );
					if ( $coord > 0 ) {

						#print "+";
						$$templateVars{'hemisphLat1'} = "N";
					}
					else {

						#print "-";
						eval( $coord = $coord * -1 );
						$$templateVars{'hemisphLat1'} = "S";
					}
					eval( $$templateVars{'latDeg1'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'latMin1'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'latSec1'} = int($coord) );

					$coord = findValue( $tempNode,
						"boundingCoordinates/eastBoundingCoordinate" );
					if ( $coord > 0 ) {

						#print "+";
						$$templateVars{'hemisphLong2'} = "E";
					}
					else {

						#print "-";
						eval( $coord = $coord * -1 );
						$$templateVars{'hemisphLong2'} = "W";
					}
					eval( $$templateVars{'longDeg2'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'longMin2'} = int($coord) );
					eval( $coord = ( $coord - int($coord) ) * 60 );
					eval( $$templateVars{'longSec2'} = int($coord) );

					$$templateVars{'hasSpatial'} = "true";
				}
			}

			$tempResult =
			  $node->findnodes('./taxonomicCoverage/taxonomicClassification');
			my $taxonIndex = 0;
			foreach $tempNode ( $tempResult->get_nodelist ) {
				$taxonIndex++;
				my $taxonRankName  = findValue( $tempNode, "taxonRankName" );
				my $taxonRankValue = findValue( $tempNode, "taxonRankValue" );
				$$templateVars{ "taxonRankName" . $taxonIndex } =
				  $taxonRankName;
				$$templateVars{ "taxonRankValue" . $taxonIndex } =
				  $taxonRankValue;
				$$templateVars{'hasTaxonomic'} = "true";
			}
			$$templateVars{'taxaCount'} = $taxonIndex;
			my $taxaAuth = findValue( $node,
				"./taxonomicCoverage/generalTaxonomicCoverage" );
			$$templateVars{'taxaAuth'} = $taxaAuth;
		}
	}
	dontOccur( $doc, "./purpose",     "purpose" );
	dontOccur( $doc, "./maintenance", "maintnance" );

	$results = $doc->findnodes('//dataset/contact/individualName');
	if ( $results->size() > 1 ) {
		errMoreThanOne("contact/individualName");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			dontOccur(
				$node,
				"../positionName|../onlineURL|../userId",
				"positionName, onlineURL, userId in contact tag"
			);
			dontOccur( $node, "./saluation", "saluation in contact tag" );

			$tempResult = $node->findnodes(
'../address|../phone|../electronicmailAddress|../organizationName'
			);
			if ( $tempResult->size > 0 ) {
				$$templateVars{'origNamefirstContact'} =
				  findValue( $node, 'givenName' );
				$$templateVars{'origNamelastContact'} =
				  findValue( $node, 'surName' );

				my $tempResult2 = $node->findnodes('../address');
				if ( $tempResult2->size > 1 ) {
					errMoreThanOne("address");
				}
				else {
					foreach my $tempNode2 ( $tempResult2->get_nodelist ) {
						$$templateVars{'origDeliveryContact'} =
						  findValue( $tempNode2, 'deliveryPoint' );
						$$templateVars{'origCityContact'} =
						  findValue( $tempNode2, 'city' );
						$$templateVars{'origStateContact'} =
						  findValue( $tempNode2, 'administrativeArea' );
						$$templateVars{'origZIPContact'} =
						  findValue( $tempNode2, 'postalCode' );
						$$templateVars{'origCountryContact'} =
						  findValue( $tempNode2, 'country' );
					}
				}

				my $tempResult3 = $node->findnodes('../phone');
				if ( $tempResult3->size > 2 ) {
					errMoreThanN("phone");
				}
				else {
					foreach my $tempNode2 ( $tempResult3->get_nodelist ) {
						if ( $tempNode2->hasAttributes() ) {
							my @attlist = $tempNode2->attributes();
							if ( $attlist[0]->value eq "Fax" ) {
								$$templateVars{'origFAXContact'} =
								  $tempNode2->textContent();
							}
							else {
								$$templateVars{'origPhoneContact'} =
								  $tempNode2->textContent();
							}
						}
						else {
							$$templateVars{'origPhoneContact'} =
							  $tempNode2->textContent();
						}
					}
				}
				$$templateVars{'origEmailContact'} =
				  findValue( $node, '../electronicMailAddress' );
				$$templateVars{'origNameOrgContact'} =
				  findValue( $node, '../organizationName' );
			}
			else {
				$$templateVars{'origNamefirstContact'} =
				  findValue( $node, 'givenName' );
				$$templateVars{'origNamelastContact'} =
				  findValue( $node, 'surName' );
				$$templateVars{'origNameOrgContact'} =
				  findValue( $node, '../organizationName' );
			}
		}
	}

	$results =
	  $doc->findnodes('//dataset/methods/methodStep/description/section');
	debug( "Number methods: " . $results->size() );
	if ( $results->size() > 1 ) {
		errMoreThanN("methods/methodStep/description/section");
	}
	else {
		my @methodPara;
		foreach $node ( $results->get_nodelist ) {
			my @children = $node->childNodes;
			for ( my $i = 0 ; $i < scalar(@children) ; $i++ ) {
				debug("Method child loop ($i)");
				my $child = $children[$i];
				if ( $child->nodeName eq 'title' ) {
					my $title = $child->textContent();
					debug("Method title ($title)");
					$$templateVars{'methodTitle'} = $title;
				}
				elsif ( $child->nodeName eq 'para' ) {
					my $para = $child->textContent();
					debug("Method para ($para)");
					$methodPara[ scalar(@methodPara) ] = $para;
				}
			}
			$$templateVars{'hasMethod'} = "true";
		}
		if ( scalar(@methodPara) > 0 ) {
			$$templateVars{'methodPara'} = \@methodPara;
		}
	}

	$results = $doc->findnodes(
		'//dataset/methods/sampling/studyExtent/description/para');
	if ( $results->size() > 1 ) {
		errMoreThanN("methods/sampling/studyExtent/description/para");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			my $studyExtentDescription = $node->textContent();
			$$templateVars{'studyExtentDescription'} = $studyExtentDescription;
			$$templateVars{'hasMethod'}              = "true";
		}
	}

	$results =
	  $doc->findnodes('//dataset/methods/sampling/samplingDescription/para');
	if ( $results->size() > 1 ) {
		errMoreThanN("methods/sampling/samplingDescription/para");
	}
	else {
		foreach $node ( $results->get_nodelist ) {
			my $samplingDescription = $node->textContent();
			$$templateVars{'samplingDescription'} = $samplingDescription;
			$$templateVars{'hasMethod'}           = "true";
		}
	}

	dontOccur( $doc, "//methodStep/citation", "methodStep/citation" );
	dontOccur( $doc, "//methodStep/protocol", "methodStep/protocol" );
	dontOccur( $doc, "//methodStep/instrumentation",
		"methodStep/instrumentation" );
	dontOccur( $doc, "//methodStep/software",    "methodStep/software" );
	dontOccur( $doc, "//methodStep/subStep",     "methodStep/subStep" );
	dontOccur( $doc, "//methodStep/dataSource",  "methodStep/dataSource" );
	dontOccur( $doc, "//methods/qualityControl", "methods/qualityControl" );

	dontOccur(
		$doc,
		"//methods/sampling/spatialSamplingUnits",
		"methods/sampling/spatialSamplingUnits"
	);
	dontOccur( $doc, "//methods/sampling/citation",
		"methods/sampling/citation" );
	dontOccur( $doc, "./pubPlace", "pubPlace" );
	dontOccur( $doc, "./project",  "project" );

	# Code for checking ACL: with EML 2.1, we should only look within the top-level elements
	#debug("checking user access");
	dontOccur( $doc, "/eml:eml/access/deny", "access/deny" );

	$results = $doc->findnodes('/eml:eml/access/allow');
	my $accessError = 0;
	my $accessGranted = 0;
	my $docOwner;
	my $errorMessage;

	my @admins;
	foreach ( split( ":", $moderators ) ) {
		push( @admins, $_ );
		#debug("getting moderator: $_");
	}
	push( @admins, $adminUsername );

	#debug("getting user groups for current user");
	
	my @userGroups = getUserGroups();

	foreach $node ( $results->get_nodelist ) {
		my @children   = $node->childNodes;
		my $permission = "";
		my $principal  = "";
		for ( my $i = 0 ; $i < scalar(@children) ; $i++ ) {
			my $child = $children[$i];
			if ( $child->nodeName eq 'principal' ) {
				$principal = $child->textContent();
			}
			elsif ( $child->nodeName eq 'permission' ) {
				$permission = $child->textContent();
			}
		}
		
		if (($principal eq 'public') && ($permission ne 'read')) {
			# If the principal is 'public' and the permission is not 'read' then this document
	    	# could not have been created in the registry. 
			$errorMessage = "The ACL for this document has been changed outside the registry. Please use Morpho to edit this document (Access Error: public principal cannot have $permission permission).\n";
			$accessError = 1;
			debug($errorMessage);
		} 
		if (($principal eq $adminUsername) && ($permission ne 'all')) {
			# If the principal is the admin and permission is not 'all' then this document
	    	# could not have been created in the registry. 
			$errorMessage = "The ACL for this document has been changed outside the registry. Please use Morpho to edit this document (Access Error: admin principal cannot have $permission permission).\n";
			$accessError = 1;
			debug($errorMessage);
		} 

		# no access error in doc, if principal is not equal to public and permission is 
		# 'all' (requirements in registry) then try and determine if user has access
		if (!$accessError && ($principal ne 'public') && ($permission eq 'all' || $permission eq 'write')) {
			my ($username, $password) = getCredentials();
						
			# 1) check if user matches principal
			#debug("does user $username match principal $principal?");
			if ($principal eq $username) {
				$accessGranted = 1;	
				#debug("Access granted: user $username matches principal");
			}
						
			# 2) if access not granted, check if user group matches principal			
			if (!$accessGranted) {
				#debug("is one of the user groups @userGroups the principal $principal?");
				for my $userGroup (@userGroups) {
      				if ($userGroup == $principal) {
           				$accessGranted = 1;
           				#debug("Access granted: user group $userGroup matches principal");
           				last;
       				}
				}
			}	
		}		
				
		# if there was an access error, we know this is not a valid registry doc.  No need to
		# continue looking at access sections in doc.  Same it true if we were granted access
		# already.
		if ( $accessError || $accessGranted ) {
			last;
		}
	}
	
	if (!$accessError) {	
		my ($username, $password) = getCredentials();
		
		# 3) if access not granted, check if the user is a moderator or admin
		if (!$accessGranted) {
			#debug("is user $username in admins @admins?");
			if (grep { $_ eq $username } @admins) {
				$accessGranted = 1;
				#debug("Access granted: user $username is an admin or moderator");
			}
		}
		
		# 4) if access not granted, check if user group in moderator/admin list
		if (!$accessGranted) {
			#debug("is one of the user groups @userGroups in admins @admins?");
			foreach my $userGroup (split(":", @userGroups)) {
				if (grep {$_ eq $userGroup} @admins) {
					$accessGranted = 1;
					#debug("Access granted: user group $userGroup is an admin or moderator");
					last;
				}
			}
		}	
	
		# 5) if access not granted, and there was no other error, the user is not authorized. 
		# Set accessError to true and set the error string
		if (!$accessError && !$accessGranted) {
			$errorMessage = "User $username is not authorized to access document\n";
			$accessError = 1;
		}
	}

	# push the error message, if any
	if ( $accessError ) {
		#debug($errorMessage);
		push( @errorMessages, $errorMessage );
	}

	# handle otherEntity objects, by populating the relevant file form elements
	$results = $doc->findnodes('//otherEntity/physical');
	my $upCount = 0;
	foreach $node ( $results->get_nodelist ) {
		my $distUrl = findValue( $node, 'distribution/online/url' );
		debug("Found distUrl of value $distUrl.");
		if ( $distUrl !~ /^ecogrid/ ) {
			my $error = "The file URL referenced is not a local resource and has been changed outside the registry. Please use Morpho to edit this document.";
			push( @errorMessages, $error . "\n" );
		}
		else {

			# have a file with a ecogrid distUrl, use this to set up the file parameters
			$distUrl =~ s/ecogrid:\/\/knb\///g;
			my $accessResults = $doc->findnodes('distribution/access/allow');
			my $accessRule    = 'private';

			foreach $node ( $accessResults->get_nodelist ) {
				my @children   = $node->childNodes;
				my $permission = "";
				my $principal  = "";
				for ( my $i = 0 ; $i < scalar(@children) ; $i++ ) {
					my $child = $children[$i];
					if ( $child->nodeName eq 'principal' ) {
						$principal = $child->textContent();
					}
					elsif ( $child->nodeName eq 'permission' ) {
						$permission = $child->textContent();
					}
				}

				if ( $principal eq 'public' && $permission eq 'read' ) {
					$accessRule = 'public';
				}
			}

		  # overload the name with the ondisk status, the docid and the SHA1 sum
			$$templateVars{"upload_$upCount"} =
			  "ondisk:$distUrl:" . findValue( $node, 'authentication' );
			$$templateVars{"uploadname_$upCount"} =
			  findValue( $node, 'objectName' );
			$$templateVars{"uploadtype_$upCount"} = findValue( $node,
				'dataFormat/externallyDefinedFormat/formatName' );
			$$templateVars{"uploadperm_$upCount"} = $accessRule;
			debug(  "Setting upload data: "
				  . $$templateVars{"upload_$upCount"} . ", "
				  . $$templateVars{"uploadname_$upCount"} . ", "
				  . $$templateVars{"uploadtype_$upCount"} . ", "
				  . $$templateVars{"uploadperm_$upCount"} );
			$upCount++;
		}
	}
	if ( $upCount > 0 ) {
		$$templateVars{"upCount"} = $upCount;
	}

	dontOccur( $doc, "./dataTable",       "dataTable" );
	dontOccur( $doc, "./spatialRaster",   "spatialRaster" );
	dontOccur( $doc, "./spatialVector",   "spatialVector" );
	dontOccur( $doc, "./storedProcedure", "storedProcedure" );
	dontOccur( $doc, "./view",            "view" );
	dontOccur( $doc, "./references",      "references" );

	dontOccur( $doc, "//citation", "citation" );
	dontOccur( $doc, "//software", "software" );
	dontOccur( $doc, "//protocol", "protocol" );
	$results =
	  $doc->findnodes('//additionalMetadata/metadata/moderatorComment');
	if ( $results->size == 0 ) {
		dontOccur( $doc, "//additionalMetadata", "additionalMetadata" );
	}
}

################################################################################
#
# Delete the eml file that has been requested for deletion.
#
################################################################################
sub deleteData {
	my $deleteAll = shift;

	# create metacat instance
	my $metacat = Metacat->new($metacatUrl);
	my $docid   = $FORM::docid;

	# Login to metacat
	my $errorMessage = "";
	my ( $username, $password ) = getCredentials();
	my $response = $metacat->login( $username, $password );

	if ( !$response ) {

		# Could not login
		push( @errorMessages, $metacat->getMessage() );
		push( @errorMessages, "Failed during login.\n" );

	}
	else {

		#Able to login - try to delete the file

		my $parser;
		my @fileArray;
		my $httpMessage;
		my $xmldoc;
		my $doc;
		my $pushDoc;
		my $alreadyInArray;
		my $findType;
		my $node;
		my $element;

		push( @fileArray, $docid );
		$parser = XML::LibXML->new();

		$httpMessage = $metacat->read($docid);
		$doc         = $httpMessage->content();
		$doc         = delNormalize($doc);
		$xmldoc      = $parser->parse_string($doc);

		if ( $xmldoc eq "" ) {
			$error = "Error in parsing the eml document";
			push( @errorMessages, $error );
		}
		else {

			$findType = $xmldoc->findnodes('//dataset/identifier');
			if ( $findType->size() > 0 ) {

				# This is a eml beta6 document
				# Delete the documents mentioned in triples also

				$findType = $xmldoc->findnodes('//dataset/triple');
				if ( $findType->size() > 0 ) {
					foreach $node ( $findType->get_nodelist ) {
						$pushDoc = findValue( $node, 'subject' );

				   # If the file is already in the @fileArray then do not add it
						$alreadyInArray = 0;
						foreach $element (@fileArray) {
							if ( $element eq $pushDoc ) {
								$alreadyInArray = 1;
							}
						}

						if ( !$alreadyInArray ) {

							# If not already in array then delete the file.
							push( @fileArray, $pushDoc );
							$response = $metacat->delete($pushDoc);

							if ( !$response ) {

								# Could not delete
								push( @errorMessages, $metacat->getMessage() );
								push( @errorMessages,
"Failed during deleting $pushDoc. Please check if you are authorized to delete this document.\n"
								);
							}
						}
					}
				}
			}
		}

		# Delete the main document.
		if ($deleteAll) {
			$response = $metacat->delete($docid);
			if ( !$response ) {

				# Could not delete
				push( @errorMessages, $metacat->getMessage() );
				push( @errorMessages,
"Failed during deleting $docid. Please check if you are authorized to delete this document.\n"
				);
			}
		}
	}

	if ( scalar(@errorMessages) ) {

		# If any errors, print them in the response template
		$$templateVars{'status'}        = 'failure';
		$$templateVars{'errorMessages'} = \@errorMessages;
		$error                          = 1;
	}

	# Process the response template
	if ($deleteAll) {
		$$templateVars{'function'} = "deleted";
		$$templateVars{'section'}  = "Deletion Status";
		$template->process( $templates->{'response'}, $templateVars );
	}
}

################################################################################
#
# function to handle login request
#
################################################################################
sub handleLoginRequest() {

	# Check if a session already exists
	my $session = CGI::Session->load() or die CGI::Session->errstr();
	if ( $session->is_empty ) {

		# no session found ... check if the login is correct
		my $username = $FORM::username;
		my $password = $FORM::password;

		my $metacat = Metacat->new($metacatUrl);
		my $returnVal = $metacat->login( $username, $password );
		debug(
"Login was $returnVal for login attempt to $metacatUrl, with $username"
		);
		if ( $returnVal > 0 ) {

			# valid username and passwd
			# create a new session and store username and passswd
			$session = new CGI::Session();

			$session->param( 'username', $username );
			$session->param( 'password', $password );

			if ( $returnVal == 2 || $returnVal == 4 ) {

				# is a moderator. store this information in the session
				$session->param( 'moderator', 'true' );
			}

			# send redirect to metacat and action = login
			my $html = "<html><head>";
			$html .= "</head><body onload=\"document.loginForm.submit()\">";
			$html .= "<form name=\"loginForm\" method=\"post\" action=\""
			  . $metacatUrl . "\">";
			$html .=
			  "<input type=\"hidden\" name=\"action\" value=\"login\" />";
			$html .= "<input type=\"hidden\" name=\"username\" value=\""
			  . $FORM::username . "\" />";
			$html .= "<input type=\"hidden\" name=\"password\" value=\""
			  . $FORM::password . "\" />";
			$html .= "<input type=\"hidden\" name=\"qformat\" value=\""
			  . $skinName . "\" />";
			$html .= "</form></body></html>";
			print $session->header();
			print $html;
		}
		else {

	# send back the error template with error message as wrong username password
	# push(@errorMessages, $metacat->getMessage());
			print "Content-type: text/html\n\n";
			push( @errorMessages, "Failed during login.\n" );
			$$templateVars{'status'}        = 'login_failure';
			$$templateVars{'errorMessages'} = \@errorMessages;
			$$templateVars{'cfg'}           = $skinName;
			$$templateVars{'section'}       = "Login Status";
			$template->process( $templates->{'response'}, $templateVars );
		}

		exit();
	}
	else {

		# session found ... redirect to index page
		my $url = "/";
		redirect($url);
		exit();
	}
}

################################################################################
#
# function to handle logout request
#
################################################################################
sub handleLogoutRequest() {
	print "Content-type: text/html\n\n";

	# Check if the session exists
	debug("Stage is logout");
	my $uname;
	my $session = CGI::Session->load();
	if ( $session->is_empty || $session->is_expired ) {

		# no session found ... send back to index.html page ...
		debug("Session empty or not found");
		# just show the logout form without other info
		#my $url = "/";
		#redirect($url);

	}
	else {

		# get the user name and delete the session
		debug("Session found");
		$uname = $session->param("username");
		$session->delete();
	}
	
	# send redirect form to metacat and action = logout
	my $html = "<html><head>";
	$html .= "</head><body onload=\"document.loginForm.submit()\">";
	$html .= "<form name=\"loginForm\" method=\"post\" action=\""
	  . $metacatUrl . "\">";
	$html .= "<input type=\"hidden\" name=\"action\" value=\"logout\" />";
	$html .= "<input type=\"hidden\" name=\"username\" value=\"" 
	  . $uname . "\" />";
	$html .= "<input type=\"hidden\" name=\"qformat\" value=\""
	  . $skinName . "\" />";
	$html .= "</form></body></html>";
	print($html);
}

################################################################################
#
# get current user credentials from session
#
################################################################################
sub getCredentials {
	my $userDN   = $FORM::username;
	my $userOrg  = $FORM::organization;
	my $userPass = $FORM::password;
	my $dname    = "uid=$userDN,o=$userOrg,dc=ecoinformatics,dc=org";

	my $session = CGI::Session->load();
	if ( !( $session->is_empty || $session->is_expired ) ) {
		$dname    = $session->param("username");
		$userPass = $session->param("password");
	}

	return ( $dname, $userPass );
}

################################################################################
#
# get user groups from metacat for the currently logged in user
#
################################################################################
sub getUserGroups {
	my $sessionId = shift;
	
	#debug("getting user info for session id: $sessionId");
	my $metacat = Metacat->new($metacatUrl);
	
	my ( $username, $password ) = getCredentials();
	$metacat->login( $username, $password );
	
	my $userInfo = $metacat->getUserInfo($sessionId);
	
	debug("user info xml: $userInfo");
	
	my $parser = XML::LibXML->new();
	my $parsedDoc = $parser->parse_string($userInfo);
	
	my $groupString = $parsedDoc->findvalue('//user/groupNames');
	
	my @groupArray;
	foreach (split(":", $groupString)) {
		$_ =~ s/^\s+//;
		$_ =~ s/\s+$//;
		debug("pushing: $_");
		push(@groupArray, $_);
	}

	return @groupArray;
}

################################################################################
#
# function to check if the moderator is logged in - if yes, then username and
# password are set
#
################################################################################
sub isModerator {
	my $stage = shift;

	my $returnValue = 1;

	# Check if the session exists
	my $session = CGI::Session->load();
	if ( $session->is_empty || $session->is_expired ) {

		# no session found ... send back to index.html page ...
		processResultTemplate( $stage, 'failure',
			"Moderator is not logged in.\n" );
		$returnValue = -1;
	}

	# check if logged in user is moderator
	my $moderator = $session->param("moderator");
	if ( $moderator ne 'true' ) {

		# no session found ... send back to index.html page ...
		debug("Logged in user is not moderator");
		processResultTemplate( $stage, 'failure',
			"Logged in user is not moderator.\n" );
		$returnValue = -1;
	}

	if ( $returnValue eq 1 ) {
		( $modUsername, $modPassword ) = getCredentials();
	}

	return $returnValue;
}

sub handleRead {
	my $docid = $FORM::docid;
	my $sessionId = $FORM::sessionid;
	my $errorMessage;

	#debug("\n\n\n\n$docid");
	print "Content-type: text/html\n\n";

	# Check if the session exists
	my $session = CGI::Session->load();
	if ( $session->is_empty || $session->is_expired ) {

		# no session found ... send back the regular read page ...
		my $url = "$skinsDir/$skinName/index.html";
		redirect($url);
	}
	else {
		my $htmldoc =
qq|<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
        <html><head><title>Dataset Description: $docid</title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
          <frameset rows="150,*" cols="*" frameborder="NO" border="0" framespacing="0">
            <frame src="$skinsDir/$skinName/header.jsp" marginwidth="40" name="topFrame" scrolling="NO" noresize>
            <frameset cols="200,*" frameborder="NO" border="0" framespacing="0">
              <body></body><frame src="$cgiPrefix/register-dataset.cgi?cfg=$skinName&stage=review_frame&docid=$docid" name="rightFrame" scrolling="NO" noresize></frame>
              <frame src="$metacatUrl?action=read&qformat=$skinName&docid=$docid&insertTemplate=0&sessionid=$sessionId" name="mainFrame">
            </frameset>
          </frameset>
        </html>
|;
		print $htmldoc;
	}
}

sub handleReviewFrame {
	print "Content-type: text/html\n\n";
	my $session = CGI::Session->load();
	if ( $session->is_empty || $session->is_expired ) {
		return;
	}

	my $moderator = $session->param("moderator");
	$$templateVars{'reviewdocid'} = getReviewHistoryHTML();
	$$templateVars{'docid'}       = $FORM::docid;
	if ( $moderator eq 'true' ) {
		$$templateVars{'isModerator'} = "true";
	}
	$template->process( $templates->{'getReviews'}, $templateVars );
}

sub getReviewHistoryHTML {
	my $metacat = Metacat->new($metacatUrl);
	my ( $username, $password ) = getCredentials();
	$metacat->login( $username, $password );
	my $parser = XML::LibXML->new();
	my $docid  = $FORM::docid;
	my ( $x, $y, $z ) = split( /\./, $docid );
	if ( $x eq "" ) {
		return "Error: Unable to find review for invalid docid.";
	}
	my $docidWithoutRev  = $x . "." . $y;
	my $reviewDocumentId = '';
	#debug("find review history for docid $docidWithoutRev");
	my $reviewQuery = "<pathquery><querytitle>Moderator-Search</querytitle><querygroup operator='INTERSECT'><queryterm searchmode='contains' casesensitive='false'><value>$docidWithoutRev</value><pathexpr>/reviewHistory/review/packageId</pathexpr></queryterm></querygroup></pathquery>";
	my $response   = $metacat->squery($reviewQuery);
	my $doc        = $response->content();
	my $xmldoc     = $parser->parse_string($doc);
	my $reviewHTML = '';

	if ( $xmldoc eq "" || $doc =~ /<error/ ) {

		# not able to parse
		return "Error: Unable to search for review for the docid:" . $docid;
	}
	else {
		my $findNodes = $xmldoc->findnodes('//resultset/document');
		if ( $findNodes->size() > 0 ) {
			#debug("//resultset/document section found");
			# found contact email address
			my $node = '';
			foreach $node ( $findNodes->get_nodelist ) {
				$reviewDocumentId = findValue( $node, 'docid' );
			}
		}
	}
	return $reviewDocumentId;
}

################################################################################
#
# function to handle accept request for moderator
#
################################################################################
sub handleModAccept() {

	my $errorMessage = '';
	my $metacat      = Metacat->new($metacatUrl);

	print "Content-type: text/html\n\n";

	debug("Stage is mod_accept");

	my $isMod = isModerator("accept");
	if ( $isMod < 0 ) {
		return;
	}

	# read the document from
	my $parser              = XML::LibXML->new();
	my $title               = '';
	my $contactEmailAddress = '';
	my $contactName         = '';
	my $userDN              = '';

	# Log into metacat
	my $response = $metacat->login( $modUsername, $modPassword );
	my $docid = $FORM::docid;

	if ( !$response ) {

		# Could not login
		$errorMessage = $errorMessage . " Failed during moderator login.";
	}
	else {

		# read the document and get the contact name and address
		$response = $metacat->read($docid);
		my $docFromMetacat = $response->content();
		my $xmldoc         = $parser->parse_string($docFromMetacat);
		my $dataElem       = '';
		my $results;

		if ( $xmldoc eq "" || $docFromMetacat =~ /<error/ ) {

			# not able to parse
			$errorMessage =
			  $errorMessage . " Error in reading the following docid:" . $docid;
		}
		else {
			my $emldoc = '';
			$xmldoc = transformEml($xmldoc);

			# pull out existing dataset entry
			$results = $xmldoc->findnodes('//dataset');
			foreach my $r ( $results->get_nodelist ) {
				$dataElem .= $r->toString();
			}

			$emldoc .= EMLStart();
			$emldoc .= accessElement("true");
			$emldoc .= $dataElem;
			$emldoc .= EMLEnd();

			#debug("Document created by handleModAccept is " . $emldoc);
			# update the document
			my $docid = incrementRevision($docid);

			$emldoc =~ s/packageId="docid"/packageId=\"$docid\"/;
			debugDoc($emldoc);
			$response = $metacat->update( $docid, $emldoc );

			if ( !$response ) {
				debug("Error while updating in handleModAccept.");

				#push(@errorMessages, $metacat->getMessage());
				$errorMessage = $errorMessage
				  . " Failed while updating the document with new access rules.";
			}

			# get the contact email address from the
			$xmldoc = $parser->parse_string($emldoc);

			if ( $xmldoc eq "" || $xmldoc =~ /<error/ ) {

				# not able to parse
				$errorMessage =
				  $errorMessage . " Error in reading the docid:" . $docid;
			}
			else {
				my $findNodes = $xmldoc->findnodes('//dataset/contact');
				if ( $findNodes->size() > 0 ) {

					# found contact email address
					my $node = '';
					foreach $node ( $findNodes->get_nodelist ) {
						$contactEmailAddress =
						  findValue( $node, 'electronicMailAddress' );
						my $surName =
						  findValue( $node, 'individualName/surName' );
						my $givenName =
						  findValue( $node, 'individualName/givenName' );
						my $organizationName =
						  findValue( $node, 'organizaitionName' );

						if ( $surName ne '' ) {
							$contactName = $givenName . ' ' . $surName;
						}
						else {
							$contactName = $organizationName;
						}
					}
				}
				else {
					$contactEmailAddress = '';
				}
				$findNodes = $xmldoc->findnodes('//dataset/title');
				if ( $findNodes->size() > 0 ) {

					# found title
					my $node = '';
					foreach $node ( $findNodes->get_nodelist ) {
						$title = findValue( $node, '../title' );
					}
				}
				else {
					$title = '';
				}

				$findNodes = $xmldoc->findnodes('//access/allow');
				if ( $findNodes->size() > 0 ) {

					# found title
					my $node = '';
					foreach $node ( $findNodes->get_nodelist ) {
						my $perm = findValue( $node, 'permission' );
						if ( $perm ne 'all' ) {
							$userDN = findValue( $node, 'principal' );
						}
					}
				}
				else {
					$userDN = '';
				}
			}
		}
	}

	# give the system a chance to finish databasing and indexing
	sleep(5);

	# send notification to the user and the moderator
	if ( $errorMessage eq '' ) {
		modSendNotification(
			$title,       $contactEmailAddress,
			$contactName, "Document $docid Accepted"
		);
		if ( $FORM::review ne '' ) {
			$errorMessage = modStoreReview( 'accept', $metacat, $userDN );
			if ( $errorMessage ne '' ) {
				processResultTemplate( 'accept', 'failure', $errorMessage );
				return;
			}
		}

		# send notifications
		processResultTemplate( 'accept', 'success', $errorMessage );
	}
	else {
		processResultTemplate( 'accept', 'failure', $errorMessage );
	}
}

################################################################################
#
# function to handle decline request for moderator
#
################################################################################
sub handleModDecline() {

	my $errorMessage = '';
	my $userDN       = '';
	my $metacat      = Metacat->new($metacatUrl);
	my $docid        = $FORM::docid;

	print "Content-type: text/html\n\n";

	debug("Stage is mod_decline");

	if ( isModerator('decline') < 0 ) {
		return;
	}

	# variables for contact information
	my $contactEmailAddress;
	my $contactName;
	my $title;

	# Log into metacat
	my $response = $metacat->login( $modUsername, $modPassword );

	if ( !$response ) {

		# Could not login
		#push(@errorMessages, $metacat->getMessage());
		$errorMessage = $errorMessage . " Failed during moderator login.";
	}
	else {

		# read the document and get the contact name and address
		my $parser = XML::LibXML->new();

		$response = $metacat->read($docid);
		my $doc    = $response->content();
		my $xmldoc = $parser->parse_string($doc);

		if ( $xmldoc eq "" || $doc =~ /<error/ ) {

			# not able to parse
			$errorMessage =
			  $errorMessage . " Error in reading the docid:" . $docid;
		}
		else {
			my $findNodes = $xmldoc->findnodes('//dataset/contact');
			if ( $findNodes->size() > 0 ) {

				# found contact email address
				my $node = '';
				foreach $node ( $findNodes->get_nodelist ) {
					$contactEmailAddress =
					  findValue( $node, 'electronicMailAddress' );
					my $surName = findValue( $node, 'individualName/surName' );
					my $givenName =
					  findValue( $node, 'individualName/givenName' );
					my $organizationName =
					  findValue( $node, 'organizaitionName' );

					if ( $surName ne '' ) {
						$contactName = $givenName . ' ' . $surName;
					}
					else {
						$contactName = $organizationName;
					}
				}
			}
			else {
				$contactEmailAddress = '';
			}
			$findNodes = $xmldoc->findnodes('//dataset/title');
			if ( $findNodes->size() > 0 ) {

				# found title
				my $node = '';
				foreach $node ( $findNodes->get_nodelist ) {
					$title = findValue( $node, '../title' );
				}
			}
			else {
				$title = '';
			}

			$findNodes = $xmldoc->findnodes('//access/allow');
			if ( $findNodes->size() > 0 ) {

				# found allow
				my $node = '';
				foreach $node ( $findNodes->get_nodelist ) {
					my $perm = findValue( $node, 'permission' );
					if ( $perm ne 'all' ) {
						$userDN = findValue( $node, 'principal' );
					}
				}
			}
			else {
				$userDN = '';
			}
		}

		$response = $metacat->delete($docid);
		if ( !$response ) {
			debug("Error while deleting document in handleModDecline.");

			#push(@errorMessages, $metacat->getMessage());
			$errorMessage = $errorMessage
			  . " Failed during deleting $docid. Please check if you are authorized to delete this document or if the document is not already deleted.";
		}
		else {
			debug( "Document deleted by handleModDecline is " . $docid );
		}
	}

	if ( $errorMessage eq '' ) {
		modSendNotification(
			$title,       $contactEmailAddress,
			$contactName, "Document $docid Rejected"
		);
		if ( $FORM::review ne '' ) {
			$errorMessage = modStoreReview( 'decline', $metacat, $userDN );
			if ( $errorMessage ne '' ) {
				processResultTemplate( 'decline', 'failure', $errorMessage );
				return;
			}
		}

		# send notifications
		processResultTemplate( 'decline', 'success', $errorMessage );
	}
	else {
		processResultTemplate( 'decline', 'failure', $errorMessage );
	}
}

################################################################################
#
# function to handle revise request for moderator
#
################################################################################
sub handleModRevise() {
	my $errorMessage = '';
	my $metacat      = Metacat->new($metacatUrl);
	my $docid        = $FORM::docid;

	print "Content-type: text/html\n\n";

	debug("Stage is mod_revise");

	if ( isModerator('revise') < 0 ) {
		return;
	}

	# variables for contact information
	my $contactEmailAddress;
	my $contactName;
	my $title;
	my $userDN = '';

	# Log into metacat
	my $response = $metacat->login( $modUsername, $modPassword );

	if ( !$response ) {

		# Could not login
		#push(@errorMessages, $metacat->getMessage());
		$errorMessage = $errorMessage . " Failed during moderator login.";

	}
	else {

		# read the document and get the contact name and address
		my $parser = XML::LibXML->new();

		$response = $metacat->read($docid);
		my $doc    = $response->content();
		my $xmldoc = $parser->parse_string($doc);
		$xmldoc = transformEml($xmldoc);
		if ( $xmldoc eq "" || $doc =~ /<error/ ) {

			# not able to parse
			$errorMessage =
			  $errorMessage . " Error in reading the docid:" . $docid;
		}
		else {
			$xmldoc = transformEml($xmldoc);
			my $findNodes = $xmldoc->findnodes('//dataset/contact');
			if ( $findNodes->size() > 0 ) {

				# found contact email address
				my $node = '';
				foreach $node ( $findNodes->get_nodelist ) {
					$contactEmailAddress =
					  findValue( $node, 'electronicMailAddress' );

					my $surName = findValue( $node, 'individualName/surName' );
					my $givenName =
					  findValue( $node, 'individualName/givenName' );
					my $organizationName =
					  findValue( $node, 'organizaitionName' );

					if ( $surName ne '' ) {
						$contactName = $givenName . ' ' . $surName;
					}
					else {
						$contactName = $organizationName;
					}
				}
			}
			else {
				$contactEmailAddress = '';
			}

			$findNodes = $xmldoc->findnodes('//dataset/title');
			if ( $findNodes->size() > 0 ) {

				# found title
				my $node = '';
				foreach $node ( $findNodes->get_nodelist ) {
					$title = findValue( $node, '../title' );
				}
			}
			else {
				$title = '';
			}

			$findNodes = $xmldoc->findnodes('//access/allow');
			if ( $findNodes->size() > 0 ) {

				# found title
				my $node = '';
				foreach $node ( $findNodes->get_nodelist ) {
					my $perm = findValue( $node, 'permission' );
					if ( $perm ne 'all' ) {
						$userDN = findValue( $node, 'principal' );
					}
				}
			}
			else {
				$userDN = '';
			}
		}

		my $emldoc     = '';
		my $dataElem   = '';
		my $accessElem = '';

		# pull out existing dataset entry
		my $results = $xmldoc->findnodes('/eml:eml/dataset');
		foreach my $r ( $results->get_nodelist ) {
			$dataElem .= $r->toString();
		}

		# also grab the access element
		$results = $xmldoc->findnodes('/eml:eml/access');
		foreach my $r ( $results->get_nodelist ) {
			$accessElem .= $r->toString();
		}

		my $addlMetadata = qq|
        <additionalMetadata>
            <metadata>
                <moderatorComment>Revision Requested</moderatorComment>
            </metadata>
        </additionalMetadata>|;

		$emldoc .= EMLStart();
		$emldoc .= $accessElem;
		$emldoc .= $dataElem;
		$emldoc .= $addlMetadata;
		$emldoc .= EMLEnd();

		# update the document
		my $docid = incrementRevision($docid);

		$emldoc =~ s/packageId="docid"/packageId="$docid"/;
		debugDoc($emldoc);
		$response = $metacat->update( $docid, $emldoc );

		if ( !$response ) {
			debug( "Error while updating in handleModAccept."
				  . $metacat->getMessage() );
			$errorMessage = $errorMessage
			  . " Failed while updating the document with additional metadata.";
		}

	}
	if ( $errorMessage eq '' ) {
		modSendNotification(
			$title,       $contactEmailAddress,
			$contactName, "Revise document: $docid"
		);
		if ( $FORM::review ne '' ) {
			$errorMessage = modStoreReview( 'revise', $metacat, $userDN );
			if ( $errorMessage ne '' ) {
				processResultTemplate( 'revise', 'failure', $errorMessage );
				return;
			}
		}

		# send notifications
		processResultTemplate( 'revise', 'success', $errorMessage );
	}
	else {
		processResultTemplate( 'revise', 'failure', $errorMessage );
	}
}

sub modStoreReview {
	my $action  = shift;
	my $metacat = shift;
	my $userDN  = shift;

	my $gmt    = gmtime($now);
	my $docid  = $FORM::docid;
	my $parser = XML::LibXML->new();

	# create the xml part that has to be inserted
	my $review = qq|
    <review>
      <packageId>$docid</packageId>
      <action>$action</action>
      <datetime>$gmt</datetime>
      <text>$FORM::review</text>
    </review>|;

	# find out if a docid already exists...
	my ( $x, $y, $z ) = split( /\./, $docid );
	my $docidWithoutRev = $x . "." . $y;
	debug("docid without rev: $docidWithoutRev");
	my $reviewDocumentId = '';
	my $response         = $metacat->squery(
"<pathquery><querytitle>Moderator-Search</querytitle><querygroup operator='INTERSECT'><queryterm searchmode='contains' casesensitive='false'><value>$docidWithoutRev</value><pathexpr>/reviewHistory/review/packageId</pathexpr></queryterm></querygroup></pathquery>"
	);

	my $doc    = $response->content();
	my $xmldoc = $parser->parse_string($doc);

	if ( $xmldoc eq "" || $doc =~ /<error/ ) {

		# not able to parse
		debug("Unable to parse results for $docidWithoutRev query.");
		return "Error: Unable to store review for the docid:" . $docid;
	}
	else {
		my $findNodes = $xmldoc->findnodes('//resultset/document');
		if ( $findNodes->size() > 0 ) {

			# found contact email address
			my $node = '';
			foreach $node ( $findNodes->get_nodelist ) {
				$reviewDocumentId = findValue( $node, 'docid' );
			}

			# update the old document
			$response = $metacat->read($reviewDocumentId);
			$doc      = $response->content();
			$xmldoc   = $parser->parse_string($doc);

			if ( $xmldoc eq "" || $doc =~ /<error/ ) {
				return
" Unable to read the document from Metacat which has the old reviews for the docid:"
				  . $docid;
			}
			else {
				my ( $reviewdoc, $temp ) = split( '</reviewHistory>', $doc );
				$reviewdoc .= $review . "\n</reviewHistory>\n";
				$reviewDocumentId = incrementRevision($reviewDocumentId);
				debug(
					"Generating review history document for $reviewDocumentId");
				debugDoc($reviewdoc);
				$response = $metacat->update( $reviewDocumentId, $reviewdoc );
				if ( $response != '1' ) {
					return
					  " Unable to update the review on Metacat for the docid:"
					  . $docid;
				}
				else {
					$response =
					  $metacat->setaccess( $reviewDocumentId, $userDN, "read",
						"allow", "allowFirst" );
					if ( $response != '1' ) {
						return
" Unable to set access for the review document in Metacat for the docid:"
						  . $docid;
					}
					foreach ( split( ":", $moderators ) ) {
						$response =
						  $metacat->setaccess( $reviewDocumentId, $_, "all",
							"allow", "allowFirst" );
						if ( $response != '1' ) {
							return
" Unable to set access for the review document in Metacat for the docid:"
							  . $docid;
						}
					}
				}
			}
		}
		else {

			#insert a new document
			debug("no review history document found, generating a new one.");

			my $id = newAccessionNumber( 'esa_reviews', $metacat );
			my $reviewDoc = '';
			my $failMessage =
			  " Unable to insert the review on Metacat for the docid:" . $docid;

			$reviewDoc .= "<?xml version=\"1.0\"?>";
			$reviewDoc .= "<reviewHistory registryName=\"" . $skinName . "\">";
			$reviewDoc .= $review;
			$reviewDoc .= "</reviewHistory>";
			$response = $metacat->insert( $id, $reviewDoc );

			if ( $response != '1' ) {
				return $failMessage;
			}
			else {

				# set access
				$response = $metacat->setaccess( $id, $userDN, "read", "allow",
					"allowFirst" );
				if ( $response != '1' ) {
					return $failMessage;
				}
				foreach ( split( ":", $moderators ) ) {
					$response = $metacat->setaccess( $id, $_, "all", "allow",
						"allowFirst" );
					if ( $response != '1' ) {
						return $failMessage;
					}
				}

				# DENY all public access to these documents
				$response = $metacat->setaccess( $id, 'public', "all", "deny",
					"allowFirst" );
				if ( $response != '1' ) {
					return $failMessage;
				}

			}
		}
	}

	return '';

# find out if a document already exist. if not then insert a new one. otherwise update the old one
# the owner of the document is the moderator
}

################################################################################
#
# send an email message to the moderator and the contact
#
################################################################################
sub modSendNotification {
	my $title               = shift;
	my $contactEmailAddress = shift;
	my $contactName         = shift;
	my $subject             = shift;

	#debug($subject);
	# send notification to the user and the moderator
	my $templateVars = { 'stage' => $FORM::stage };
	my ( $x, $y, $z ) = split( /\./, $FORM::docid );
	my $docidWithoutRev = $x . "." . $y;
	$$templateVars{'docid'}       = $docidWithoutRev;
	$$templateVars{'fullDocid'}   = $FORM::docid;
	if (hasContent($FORM::function)) {
		$$templateVars{'function'}    = $FORM::function;
	} else {
		$$templateVars{'function'}    = 'unknown';
	}
	$$templateVars{'comment'}     = $FORM::review;
	$$templateVars{'contactName'} = $contactName;
	$$templateVars{'dpTitle'}     = $title;
	$$templateVars{'registryUrl'} =
	  $contextUrl . "/cgi-bin/register-dataset.cgi";
	$$templateVars{'metacatUrl'}       = $metacatUrl;
	$$templateVars{'contextUrl'}       = $contextUrl;
	$$templateVars{'recipient_status'} = 'moderator';
	debug(
"modSendNotification - sending moderator notification to: $email->{'recipient'} with subject: $subject"
	);
	sendNotification( $email->{'sender'}, $email->{'recipient'}, $subject,
		$templates->{'modEmailNotification'},
		$templateVars );

	if ( $contactEmailAddress ne '' ) {

		# send notification to contact email address specified in cfg
		$$templateVars{'recipient_status'} = 'user';
		debug(
"modSendNotification - sending user notification to: $contactEmailAddress with subject: $subject"
		);
		sendNotification( $email->{'sender'}, $contactEmailAddress, $subject,
			$templates->{'modEmailNotification'},
			$templateVars );
	}
}

################################################################################
#
# send an email message notifying the moderator of a new submission
#
################################################################################
sub sendNotification {
	my $sender       = shift;
	my $recipient    = shift;
	my $subject      = shift;
	my $templateForm = shift;
	my $templateVars = shift;

	# When testing, set recipient to your email address
	# $recipient = 'walbridge@nceas.ucsb.edu';
	my $smtp = Net::SMTP->new( $email->{'mailhost'} );
	$smtp->mail($sender);
	$smtp->to($recipient);

	$smtp->data;
	$smtp->datasend("From: $sender\n");
	$smtp->datasend("To: <$recipient>\n");
	$smtp->datasend("Subject: $subject\n");
	$smtp->datasend("\n");

	my $message;
	$template->process( $templateForm, $templateVars, \$message );

	$smtp->datasend($message);
	$smtp->dataend();
	$smtp->quit;
}

################################################################################
#
# sends back the result to the use
#
################################################################################
sub processResultTemplate() {
	my $stage   = shift;
	my $result  = shift;
	my $message = shift;

	if ( $result eq 'success' ) {
		if ( $stage eq 'accept' ) {
			$message = "Dataset (docid:" . $FORM::docid . ") accepted ";
		}
		elsif ( $stage eq 'decline' ) {
			$message = "Dataset (docid:" . $FORM::docid . ") declined ";
		}
		else {
			$message =
			  "Revision requested for dataset (docid:" . $FORM::docid . ") ";
		}
	}
	else {
		if ( $stage eq 'accept' ) {
			$message =
			    "Failure in accepting docid: "
			  . $FORM::docid . " ("
			  . $message . ")";
		}
		elsif ( $stage eq 'decline' ) {
			$message =
			    "Failure in declining docid: "
			  . $FORM::docid . " ("
			  . $message . ")";
		}
		else {
			$message =
			    "Failure in requesting revision of docid: "
			  . $FORM::docid . " ("
			  . $message . ")";
		}
	}
	$$templateVars{'message'} = $message;
	$template->process( $templates->{'modResult'}, $templateVars );
}

################################################################################
#
# Do data validation and send the data to confirm data template.
#
################################################################################
sub toConfirmData {

	# Check if any invalid parameters

	my $invalidParams;
	if ( !$error ) {
		$invalidParams = validateParameters(0);
		if ( scalar(@$invalidParams) ) {
			$$templateVars{'status'}        = 'failure';
			$$templateVars{'invalidParams'} = $invalidParams;
			$error                          = 1;
		}
	}

	$$templateVars{'providerGivenName'} = normalizeCD($FORM::providerGivenName);
	$$templateVars{'providerSurName'}   = normalizeCD($FORM::providerSurName);
	if ( $FORM::site eq "Select your station here." ) {
		$$templateVars{'site'} = "";
	}
	else {
		$$templateVars{'site'} = $FORM::site;
	}
	if ( $skinName eq "nceas" ) {
		$$templateVars{'wg'} = \@FORM::wg;
	}
	$$templateVars{'identifier'}     = normalizeCD($FORM::identifier);
	$$templateVars{'title'}          = normalizeCD($FORM::title);
	$$templateVars{'origNamefirst0'} = normalizeCD($FORM::origNamefirst0);
	$$templateVars{'origNamelast0'}  = normalizeCD($FORM::origNamelast0);
	$$templateVars{'origNameOrg'}    = normalizeCD($FORM::origNameOrg);
	$$templateVars{'origDelivery'}   = normalizeCD($FORM::origDelivery);
	$$templateVars{'origCity'}       = normalizeCD($FORM::origCity);

	if ( $FORM::origState =~ /select state/i ) {
		$$templateVars{'origState'} = "";
	}
	else {
		$$templateVars{'origState'} = $FORM::origState;
	}
	$$templateVars{'origStateOther'} = normalizeCD($FORM::origStateOther);
	$$templateVars{'origZIP'}        = normalizeCD($FORM::origZIP);
	$$templateVars{'origCountry'}    = normalizeCD($FORM::origCountry);
	$$templateVars{'origPhone'}      = normalizeCD($FORM::origPhone);
	$$templateVars{'origFAX'}        = normalizeCD($FORM::origFAX);
	$$templateVars{'origEmail'}      = normalizeCD($FORM::origEmail);
	$$templateVars{'useOrigAddress'} = normalizeCD($FORM::useOrigAddress);
	if ( $FORM::useOrigAddress eq "on" ) {
		$$templateVars{'origNamefirstContact'} =
		  normalizeCD($FORM::origNamefirst0);
		$$templateVars{'origNamelastContact'} =
		  normalizeCD($FORM::origNamelast0);
		$$templateVars{'origNameOrgContact'} = normalizeCD($FORM::origNameOrg);
		$$templateVars{'origDeliveryContact'} =
		  normalizeCD($FORM::origDelivery);
		$$templateVars{'origCityContact'} = normalizeCD($FORM::origCity);
		if ( $FORM::origState =~ /select state/i ) {
			$$templateVars{'origStateContact'} = "";
		}
		else {
			$$templateVars{'origStateContact'} = $FORM::origState;
		}
		$$templateVars{'origStateOtherContact'} =
		  normalizeCD($FORM::origStateOther);
		$$templateVars{'origZIPContact'}     = normalizeCD($FORM::origZIP);
		$$templateVars{'origCountryContact'} = normalizeCD($FORM::origCountry);
		$$templateVars{'origPhoneContact'}   = normalizeCD($FORM::origPhone);
		$$templateVars{'origFAXContact'}     = normalizeCD($FORM::origFAX);
		$$templateVars{'origEmailContact'}   = normalizeCD($FORM::origEmail);
	}
	else {
		$$templateVars{'origNamefirstContact'} =
		  normalizeCD($FORM::origNamefirstContact);
		$$templateVars{'origNamelastContact'} =
		  normalizeCD($FORM::origNamelastContact);
		$$templateVars{'origNameOrgContact'} =
		  normalizeCD($FORM::origNameOrgContact);
		$$templateVars{'origDeliveryContact'} =
		  normalizeCD($FORM::origDeliveryContact);
		$$templateVars{'origCityContact'} = normalizeCD($FORM::origCityContact);
		if ( $FORM::origStateContact =~ /select state/i ) {
			$$templateVars{'origStateContact'} = "";
		}
		else {
			$$templateVars{'origStateContact'} = $FORM::origStateContact;
		}
		$$templateVars{'origStateOtherContact'} =
		  normalizeCD($FORM::origStateOtherContact);
		$$templateVars{'origZIPContact'} = normalizeCD($FORM::origZIPContact);
		$$templateVars{'origCountryContact'} =
		  normalizeCD($FORM::origCountryContact);
		$$templateVars{'origPhoneContact'} =
		  normalizeCD($FORM::origPhoneContact);
		$$templateVars{'origFAXContact'} = normalizeCD($FORM::origFAXContact);
		$$templateVars{'origEmailContact'} =
		  normalizeCD($FORM::origEmailContact);
	}

	my $aoFNArray   = \@FORM::aoFirstName;
	my $aoLNArray   = \@FORM::aoLastName;
	my $aoRoleArray = \@FORM::aoRole;
	my $aoCount     = 1;

	for ( my $i = 0 ; $i <= $#$aoRoleArray ; $i++ ) {
		if ( hasContent( $aoFNArray->[$i] ) && hasContent( $aoLNArray->[$i] ) )
		{
			debug(  "Processing Associated Party: origName = "
				  . $aoFNArray->[$i]
				  . " origNamelast = "
				  . $aoLNArray->[$i]
				  . " origRole = "
				  . $aoRoleArray->[$i] );
			$$templateVars{ "origNamefirst" . $aoCount } =
			  normalizeCD( $aoFNArray->[$i] );
			$$templateVars{ "origNamelast" . $aoCount } =
			  normalizeCD( $aoLNArray->[$i] );
			$$templateVars{ "origRole" . $aoCount } =
			  normalizeCD( $aoRoleArray->[$i] );
			$aoCount++;
		}
	}

	$$templateVars{'aoCount'}  = $aoCount;
	$$templateVars{'abstract'} = normalizeCD($FORM::abstract);

	my $keywordArray     = \@FORM::keyword;
	my $keywordTypeArray = \@FORM::keywordType;
	my $keywordThArray   = \@FORM::keywordTh;
	my $keyCount         = 1;

	for ( my $i = 0 ; $i <= $#$keywordArray ; $i++ ) {
		if ( hasContent( $keywordArray->[$i] ) ) {
			debug(
				"Processing keyword: keyword = " . $keywordArray->[$i] . "
                  keywordType = " . $keywordTypeArray->[$i] . "
                  keywordTh = " . $keywordThArray->[$i]
			);
			$$templateVars{ "keyword" . $keyCount } =
			  normalizeCD( $keywordArray->[$i] );
			$$templateVars{ "kwType" . $keyCount } =
			  normalizeCD( $keywordTypeArray->[$i] );
			$$templateVars{ "kwTh" . $keyCount } =
			  normalizeCD( $keywordThArray->[$i] );
			$keyCount++;
		}
	}
	$$templateVars{'keyCount'} = $keyCount;

	$$templateVars{'addComments'}    = normalizeCD($FORM::addComments);
	$$templateVars{'useConstraints'} = $FORM::useConstraints;
	if ( $FORM::useConstraints eq "other" ) {
		$$templateVars{'useConstraintsOther'} = $FORM::useConstraintsOther;
	}
	$$templateVars{'url'}        = $FORM::url;
	$$templateVars{'dataMedium'} = $FORM::dataMedium;
	if ( $FORM::dataMedium eq "other" ) {
		$$templateVars{'dataMediumOther'} = normalizeCD($FORM::dataMediumOther);
	}
	$$templateVars{'beginningYear'}  = $FORM::beginningYear;
	$$templateVars{'beginningMonth'} = $FORM::beginningMonth;
	$$templateVars{'beginningDay'}   = $FORM::beginningDay;
	$$templateVars{'endingYear'}     = $FORM::endingYear;
	$$templateVars{'endingMonth'}    = $FORM::endingMonth;
	$$templateVars{'endingDay'}      = $FORM::endingDay;
	$$templateVars{'geogdesc'}       = normalizeCD($FORM::geogdesc);
	$$templateVars{'useSiteCoord'}   = $FORM::useSiteCoord;
	$$templateVars{'latDeg1'}        = $FORM::latDeg1;
	$$templateVars{'latMin1'}        = $FORM::latMin1;
	$$templateVars{'latSec1'}        = $FORM::latSec1;
	$$templateVars{'hemisphLat1'}    = $FORM::hemisphLat1;
	$$templateVars{'longDeg1'}       = $FORM::longDeg1;
	$$templateVars{'longMin1'}       = $FORM::longMin1;
	$$templateVars{'longSec1'}       = $FORM::longSec1;
	$$templateVars{'hemisphLong1'}   = $FORM::hemisphLong1;
	$$templateVars{'latDeg2'}        = $FORM::latDeg2;
	$$templateVars{'latMin2'}        = $FORM::latMin2;
	$$templateVars{'latSec2'}        = $FORM::latSec2;
	$$templateVars{'hemisphLat2'}    = $FORM::hemisphLat2;
	$$templateVars{'longDeg2'}       = $FORM::longDeg2;
	$$templateVars{'longMin2'}       = $FORM::longMin2;
	$$templateVars{'longSec2'}       = $FORM::longSec2;
	$$templateVars{'hemisphLong2'}   = $FORM::hemisphLong2;

	my $taxonRankArray = \@FORM::taxonRank;
	my $taxonNameArray = \@FORM::taxonName;
	my $taxonCount     = 1;

	for ( my $i = 0 ; $i <= $#$taxonNameArray ; $i++ ) {
		if (   hasContent( $taxonRankArray->[$i] )
			&& hasContent( $taxonNameArray->[$i] ) )
		{
			debug(  "Processing keyword: trv = "
				  . $taxonRankArray->[$i]
				  . " trn = "
				  . $taxonNameArray->[$i] );
			$$templateVars{ "taxonRankName" . $taxonCount } =
			  normalizeCD( $taxonRankArray->[$i] );
			$$templateVars{ "taxonRankValue" . $taxonCount } =
			  normalizeCD( $taxonNameArray->[$i] );
			$taxonCount++;
		}
	}

	$$templateVars{'taxaCount'} = $taxonCount - 1;
	$$templateVars{'taxaAuth'}  = normalizeCD($FORM::taxaAuth);
	my $deleteCount = 0;

	for ( my $i = 0 ; $i <= scalar(@FORM::deletefile) ; $i++ ) {
		my $delfile = pop(@FORM::deletefile);
		$$templateVars{"deletefile_$deleteCount"} = $delfile;
		debug(" creating deletefile_$deleteCount = $delfile");
		$deleteCount++;
	}
	$$templateVars{"delCount"} = $deleteCount;
	for ( my $upNum = 0 ; $upNum <= $FORM::upCount ; $upNum++ ) {
		my $upn = "upload_$upNum";
		if ( hasContent( param($upn) ) ) {
			debug("Processing existing file: $upn");
			$$templateVars{"upload_$upNum"}     = param("upload_$upNum");
			$$templateVars{"uploadname_$upNum"} = param("uploadname_$upNum");
			$$templateVars{"uploadtype_$upNum"} = param("uploadtype_$upNum");
			$$templateVars{"uploadperm_$upNum"} = param("uploadperm_$upNum");
		}
	}

	my $uploadCount = 0;
	for ( my $fileNum = 0 ; $fileNum <= $FORM::fileCount ; $fileNum++ ) {
		my $fn = 'file_' . $fileNum;
		if ( hasContent( param($fn) ) ) {
			my $fileName = eval "\$FORM::file_$fileNum";
			debug("Processing file: $fn");

 # Upload the file object itself to a temporary file, copy file metadata to form
			my $fileInfo = processFile($fileName);
			$$templateVars{"upload_$fileNum"}     = $fileInfo->{'fileHash'};
			$$templateVars{"uploadname_$fileNum"} = $fileInfo->{'fileName'};
			$$templateVars{"uploadtype_$fileNum"} = $fileInfo->{'contentType'};
			$$templateVars{"uploadperm_$fileNum"} =
			  param("uploadperm_$fileNum");
			$uploadCount++;
		}
	}

	# total uploads are: new uploads - deleted files + original uploads
	$$templateVars{'upCount'}     = $uploadCount + $FORM::upCount;
	$$templateVars{'methodTitle'} = normalizeCD($FORM::methodTitle);

	my @tempMethodPara;
	for ( my $i = 0 ; $i < scalar(@FORM::methodPara) ; $i++ ) {
		$tempMethodPara[$i] = normalizeCD( $FORM::methodPara[$i] );
	}
	$$templateVars{'methodPara'} = \@tempMethodPara;
	$$templateVars{'studyExtentDescription'} =
	  normalizeCD($FORM::studyExtentDescription);
	$$templateVars{'samplingDescription'} =
	  normalizeCD($FORM::samplingDescription);
#	$$templateVars{'origStateContact'} = $FORM::origState;
	$$templateVars{'modules'}          = $modules;
	$$templateVars{'required'}         = $required;
	$$templateVars{'show'}             = $show;
	$$templateVars{'site'}             = $FORM::site;
	$$templateVars{'docid'}            = $FORM::docid;

	# Check if the session exists
	my $session = CGI::Session->load();
	if ( !( $session->is_empty || $session->is_expired ) ) {
		$$templateVars{'userLoggedIn'} = 'true';
	}

# Errors from validation function. print the errors out using the response template
	if ( scalar(@errorMessages) ) {
		debug("Error messages found when confirming data.");
		$$templateVars{'status'}        = 'failure';
		$$templateVars{'errorMessages'} = \@errorMessages;
		$error                          = 1;
	}

	if ( !$error ) {

		# If no errors, then print out data in confirm Data template
		$$templateVars{'section'} = "Confirm Data";
		$template->process( $templates->{'confirmData'}, $templateVars );

	}
	else {

		# Create our HTML response and send it back
		$$templateVars{'function'} = "submitted";
		$$templateVars{'section'}  = "Submission Status";
		$template->process( $templates->{'response'}, $templateVars );
	}
}

################################################################################
#
# From confirm Data template - user wants to make some changes.
#
################################################################################
sub confirmDataToReEntryData {
	my @sortedSites;
	foreach my $site ( sort @sitelist ) {
		push( @sortedSites, $site );
	}

	$$templateVars{'siteList'} = \@sortedSites;
	$$templateVars{'section'}  = "Re-Entry Form";
	copyFormToTemplateVars();
	$$templateVars{'docid'} = $FORM::docid;

	$$templateVars{'form'} = 're_entry';
	$template->process( $templates->{'entry'}, $templateVars );
}

################################################################################
#
# Copy form data to templateVars.....
#
################################################################################
sub copyFormToTemplateVars {
	$$templateVars{'providerGivenName'} = $FORM::providerGivenName;
	$$templateVars{'providerSurName'}   = $FORM::providerSurName;
	$$templateVars{'site'}              = $FORM::site;
	if ( $skinName eq "nceas" ) {
		my $projects = getProjectList($properties);
		$$templateVars{'projects'} = $projects;
		$$templateVars{'wg'}       = \@FORM::wg;
	}
	$$templateVars{'identifier'}     = $FORM::identifier;
	$$templateVars{'title'}          = $FORM::title;
	$$templateVars{'origNamefirst0'} = $FORM::origNamefirst0;
	$$templateVars{'origNamelast0'}  = $FORM::origNamelast0;
	$$templateVars{'origNameOrg'}    = $FORM::origNameOrg;
	$$templateVars{'origDelivery'}   = $FORM::origDelivery;
	$$templateVars{'origCity'}       = $FORM::origCity;
	$$templateVars{'origState'}      = $FORM::origState;
	$$templateVars{'origStateOther'} = $FORM::origStateOther;
	$$templateVars{'origZIP'}        = $FORM::origZIP;
	$$templateVars{'origCountry'}    = $FORM::origCountry;
	$$templateVars{'origPhone'}      = $FORM::origPhone;
	$$templateVars{'origFAX'}        = $FORM::origFAX;
	$$templateVars{'origEmail'}      = $FORM::origEmail;

	if ( $FORM::useSiteCoord ne "" ) {
		$$templateVars{'useOrigAddress'} = "CHECKED";
	}
	else {
		$$templateVars{'useOrigAddress'} = $FORM::useOrigAddress;
	}
	$$templateVars{'origNamefirstContact'}  = $FORM::origNamefirstContact;
	$$templateVars{'origNamelastContact'}   = $FORM::origNamelastContact;
	$$templateVars{'origNameOrgContact'}    = $FORM::origNameOrgContact;
	$$templateVars{'origDeliveryContact'}   = $FORM::origDeliveryContact;
	$$templateVars{'origCityContact'}       = $FORM::origCityContact;
	$$templateVars{'origStateContact'}      = $FORM::origStateContact;
	$$templateVars{'origStateOtherContact'} = $FORM::origStateOtherContact;
	$$templateVars{'origZIPContact'}        = $FORM::origZIPContact;
	$$templateVars{'origCountryContact'}    = $FORM::origCountryContact;
	$$templateVars{'origPhoneContact'}      = $FORM::origPhoneContact;
	$$templateVars{'origFAXContact'}        = $FORM::origFAXContact;
	$$templateVars{'origEmailContact'}      = $FORM::origEmailContact;

	$$templateVars{'aoCount'} = $FORM::aoCount;
	foreach my $origName ( param() ) {
		if ( $origName =~ /origNamefirst/ ) {
			my $origNameIndex = $origName;
			$origNameIndex =~
			  s/origNamefirst//;    # get the index of the parameter 0, ..., 10
			my $origNamelast = "origNamelast" . $origNameIndex;
			my $origRole     = "origRole" . $origNameIndex;
			if ( $origNameIndex =~ /[0-9]+/ && $origNameIndex > 0 ) {
				if (   hasContent( param($origName) )
					&& hasContent( param($origNamelast) )
					&& hasContent( param($origRole) ) )
				{
					debug(  "Processing keyword: $origName = "
						  . param($origName)
						  . " $origNamelast = "
						  . param($origNamelast)
						  . " $origRole = "
						  . param($origRole) );
					$$templateVars{$origName} = normalizeCD( param($origName) );
					$$templateVars{$origNamelast} =
					  normalizeCD( param($origNamelast) );
					$$templateVars{$origRole} = normalizeCD( param($origRole) );
				}
			}
		}
	}

	$$templateVars{'abstract'} = $FORM::abstract;
	$$templateVars{'keyCount'} = $FORM::keyCount;
	foreach my $kyd ( param() ) {
		if ( $kyd =~ /keyword/ ) {
			my $keyIndex = $kyd;
			$keyIndex =~
			  s/keyword//;    # get the index of the parameter 0, ..., 10
			my $keyType = "kwType" . $keyIndex;
			my $keyTh   = "kwTh" . $keyIndex;
			if ( $keyIndex =~ /[0-9]+/ ) {
				if (   hasContent( param($kyd) )
					&& hasContent( param($keyType) )
					&& hasContent( param($keyTh) ) )
				{
					debug(  "Processing keyword: $kyd = "
						  . param($kyd)
						  . " $keyType = "
						  . param($keyType)
						  . " $keyTh = "
						  . param($keyTh) );
					$$templateVars{$kyd} = param($kyd);
					my $tmp =
					  param($keyType);   #convert the first letter to upper case
					$tmp =~ s/\b(\w)/\U$1/g;
					$$templateVars{$keyType} = $tmp;
					$$templateVars{$keyTh}   = param($keyTh);
				}
			}
		}
	}

	$$templateVars{'addComments'}         = $FORM::addComments;
	$$templateVars{'useConstraints'}      = $FORM::useConstraints;
	$$templateVars{'useConstraintsOther'} = $FORM::useConstraintsOther;
	$$templateVars{'url'}                 = $FORM::url;
	$$templateVars{'dataMedium'}          = $FORM::dataMedium;
	$$templateVars{'dataMediumOther'}     = $FORM::dataMediumOther;
	$$templateVars{'beginningYear'}       = $FORM::beginningYear;
	$$templateVars{'beginningMonth'}      = $FORM::beginningMonth;
	$$templateVars{'beginningDay'}        = $FORM::beginningDay;
	$$templateVars{'endingYear'}          = $FORM::endingYear;
	$$templateVars{'endingMonth'}         = $FORM::endingMonth;
	$$templateVars{'endingDay'}           = $FORM::endingDay;
	$$templateVars{'geogdesc'}            = $FORM::geogdesc;
	if ( $FORM::useSiteCoord ne "" ) {
		$$templateVars{'useSiteCoord'} = "CHECKED";
	}
	else {
		$$templateVars{'useSiteCoord'} = "";
	}
	$$templateVars{'latDeg1'}      = $FORM::latDeg1;
	$$templateVars{'latMin1'}      = $FORM::latMin1;
	$$templateVars{'latSec1'}      = $FORM::latSec1;
	$$templateVars{'hemisphLat1'}  = $FORM::hemisphLat1;
	$$templateVars{'longDeg1'}     = $FORM::longDeg1;
	$$templateVars{'longMin1'}     = $FORM::longMin1;
	$$templateVars{'longSec1'}     = $FORM::longSec1;
	$$templateVars{'hemisphLong1'} = $FORM::hemisphLong1;
	$$templateVars{'latDeg2'}      = $FORM::latDeg2;
	$$templateVars{'latMin2'}      = $FORM::latMin2;
	$$templateVars{'latSec2'}      = $FORM::latSec2;
	$$templateVars{'hemisphLat2'}  = $FORM::hemisphLat2;
	$$templateVars{'longDeg2'}     = $FORM::longDeg2;
	$$templateVars{'longMin2'}     = $FORM::longMin2;
	$$templateVars{'longSec2'}     = $FORM::longSec2;
	$$templateVars{'hemisphLong2'} = $FORM::hemisphLong2;
	$$templateVars{'taxaCount'}    = $FORM::taxaCount;

	foreach my $trn ( param() ) {
		if ( $trn =~ /taxonRankName/ ) {
			my $taxIndex = $trn;
			$taxIndex =~
			  s/taxonRankName//;    # get the index of the parameter 0, ..., 10
			my $trv = "taxonRankValue" . $taxIndex;
			if ( $taxIndex =~ /[0-9]+/ ) {
				if ( hasContent( param($trn) ) && hasContent( param($trv) ) ) {
					debug(  "Processing taxon: $trn = "
						  . param($trn)
						  . " $trv = "
						  . param($trv) );
					$$templateVars{$trn} = param($trn);
					$$templateVars{$trv} = param($trv);
				}
			}
		}
	}
	$$templateVars{'taxaAuth'}               = $FORM::taxaAuth;
	$$templateVars{'methodTitle'}            = $FORM::methodTitle;
	$$templateVars{'methodPara'}             = \@FORM::methodPara;
	$$templateVars{'studyExtentDescription'} = $FORM::studyExtentDescription;
	$$templateVars{'samplingDescription'}    = $FORM::samplingDescription;

	$$templateVars{'modules'}   = $modules;
	$$templateVars{'required'}  = $required;
	$$templateVars{'show'}      = $show;
	$$templateVars{'site'}      = $FORM::site;
	$$templateVars{'fileCount'} = $FORM::fileCount;

	my $uploadCount = 0;
	foreach my $upload ( param() ) {
		if ( $upload =~ /upload_/ ) {
			my $fileIndex = $upload;
			$fileIndex =~ s/upload_//;
			if ( $fileIndex =~ /[0-9]+/ ) {
				if ( hasContent( param($upload) ) ) {
					debug( "Returning filename: " . param($upload) );
					$$templateVars{"upload_$fileIndex"} = param($upload);
					$$templateVars{"uploadname_$fileIndex"} =
					  param("uploadname_$fileIndex");
					$$templateVars{"uploadtype_$fileIndex"} =
					  param("uploadtype_$fileIndex");
					$$templateVars{"uploadperm_$fileIndex"} =
					  param("uploadperm_$fileIndex");
					$uploadCount++;
				}
			}
		}
	}

	$$templateVars{'upCount'} = $uploadCount;
}

################################################################################
#
# check if there is multiple occurence of the given tag and find its value.
#
################################################################################

sub findValue {
	my $node  = shift;
	my $value = shift;
	my $result;
	my $tempNode;

	$result = $node->findnodes("./$value");
	if ( $result->size > 1 ) {
		errMoreThanOne("$value");
	}
	else {
		foreach $tempNode ( $result->get_nodelist ) {

			#print $tempNode->nodeName().":".$tempNode->textContent();
			#print "\n";
			return $tempNode->textContent();
		}
	}
}

################################################################################
#
# check if given tags has any children. if not return the value
#
################################################################################
sub findValueNoChild {
	my $node  = shift;
	my $value = shift;
	my $tempNode;
	my $childNodes;
	my $result;
	my $error;

	$result = $node->findnodes("./$value");
	if ( $result->size > 1 ) {
		errMoreThanOne("$value");
	}
	else {
		foreach $tempNode ( $result->get_nodelist ) {
			$childNodes = $tempNode->childNodes;
			if ( $childNodes->size() > 1 ) {
				$error =
"The tag $value has children which cannot be shown using the form. Please use Morpho to edit this document";
				push( @errorMessages, $error );
			}
			else {
				return $tempNode->textContent();
			}
		}
	}
}

################################################################################
#
# check if given tags are children of given node.
#
################################################################################
sub dontOccur {
	my $node   = shift;
	my $value  = shift;
	my $errVal = shift;

	my $result = $node->findnodes("$value");
	if ( $result->size > 0 ) {
		debug("Error trying to find $value, $errVal.");
		$error = "One of the following tags found: $errVal. Please use Morpho to edit this document";
		push( @errorMessages, $error . "\n" );
	}
}

################################################################################
#
# print out error for more than one occurence of a given tag
#
################################################################################
sub errMoreThanOne {
	my $value = shift;
	my $error =
"More than one occurence of the tag $value found. Please use Morpho to edit this document";
	push( @errorMessages, $error . "\n" );
}

################################################################################
#
# print out error for more than given number of occurences of a given tag
#
################################################################################
sub errMoreThanN {
	my $value = shift;
	my $error =
"More occurences of the tag $value found than that can be shown in the form. Please use Morpho to edit this document";
	push( @errorMessages, $error );
}

################################################################################
#
# print redirect html code
#
################################################################################
sub redirect() {
	my $url = shift;

	print "<head>";
	print "<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=" . $url . "\">";
	print "</head><body></body></html>";
}

################################################################################
#
# print debugging messages to stderr
#
################################################################################
sub debug {
	my $msg = shift;

	if ($debug_enabled) {
		my $date = localtime();
		print STDERR "Registry $date: $msg\n";
	}
	else {

		# This empty print statement is to prevent a bizzare error of the NCEAS
		# skin failing when: debug = 0, and a file upload is attached. Maybe
		# related to function return values?
		print "";
	}
}

################################################################################
#
# dump the EML document to disk
#
################################################################################

sub debugDoc {
	my $doc = shift;

	if ($debug_enabled) {

		# Write out the XML file for debugging purposes
		my $testFile = $tempDir . "/registry-eml-upload.xml";

		if ( open( TFILE, ">$testFile" ) ) {
			print TFILE $doc;
			close(TFILE);
		}
		else {
			debug("WARNING: Cant open xml file: $testFile");
		}
	}
}

################################################################################
#
# get the list of projects
#
################################################################################
sub getProjectList {
	my $properties = shift;
	my $projects;

	# Check for availability AdminDB.pm, for accessing NCEAS projects
	eval { require NCEAS::AdminDB };
	if ($@) {
		$projects = getTestProjectList();
	}
	else {
		my $admindb = NCEAS::AdminDB->new();

		# populate db parameters for NCEAS AdminDB
		my $db = $skinProperties->splitToTree( qr/\./, 'registry.db' );

		# check all neccessary parameters exist
		if ( !$db->{'connection'} || !$db->{'user'} || !$db->{'password'} ) {
			debug(
"NCEAS AdminDB misconfigured. Check database configuration parameters, in the registry.db section of nceas.properties."
			);
			$projects = getTestProjectList();
		}
		else {
			$admindb->connect( $db->{'connection'}, $db->{'user'},
				$db->{'password'} );
			if ( $admindb->{'connected'} ) {
				$projects = $admindb->getProjects();
			}
			else {
				$projects = getTestProjectList();
			}
		}
	}
	return $projects;
}

################################################################################
#
# get a test list of projects for use only in testing where the NCEAS
# admin db is not available.
#
################################################################################
sub getTestProjectList {

	# This block is for testing only!  Remove for production use
	my @row1;
	$row1[0] = 6000;
	$row1[1] = 'Andelman';
	$row1[2] = 'Sandy';
	$row1[3] =
'The very long and windy path to an apparent ecological conclusion: statistics lie';
	my @row2;
	$row2[0] = 7000;
	$row2[1] = 'Bascompte';
	$row2[2] = 'Jordi';
	$row2[3] = 'Postdoctoral Fellow';
	my @row3;
	$row3[0] = 7001;
	$row3[1] = 'Hackett';
	$row3[2] = 'Edward';
	$row3[3] = 'Sociology rules the world';
	my @row4;
	$row4[0] = 7002;
	$row4[1] = 'Jones';
	$row4[2] = 'Matthew';
	$row4[3] = 'Informatics rules the world';
	my @row5;
	$row5[0] = 7003;
	$row5[1] = 'Schildhauer';
	$row5[2] = 'Mark';
	$row5[3] = 'Excel rocks my world, assuming a, b, and c';
	my @row6;
	$row6[0] = 7004;
	$row6[1] = 'Rogers';
	$row6[2] = 'Bill';
	$row6[3] = 'Graduate Intern';
	my @row7;
	$row7[0] = 7005;
	$row7[1] = 'Zedfried';
	$row7[2] = 'Karl';
	$row7[3] = 'A multivariate analysis of thing that go bump in the night';
	my @projects;
	$projects[0] = \@row1;
	$projects[1] = \@row2;
	$projects[2] = \@row3;
	$projects[3] = \@row4;
	$projects[4] = \@row5;
	$projects[5] = \@row6;
	$projects[6] = \@row7;
	return \@projects;
}
