#!/usr/bin/perl -w
#
# This is a web-based application for allowing users to register a new
# account for Metacat access.  We currently only support LDAP even
# though metacat could potentially support other types of directories.

use lib '../WEB-INF/lib';
use strict;             # turn on strict syntax checking
use Template;           # load the template-toolkit module
use CGI qw/:standard :html3/; # load the CGI module 
use Net::LDAP;          # load the LDAP net libraries
use Net::SMTP;          # load the SMTP net libraries
use Digest::SHA1;       # for creating the password hash
use MIME::Base64;       # for creating the password hash
use URI;                # for parsing URL syntax
use Config::Properties; # for parsing Java .properties files
use File::Basename;     # for path name parsing
use DateTime;           # for parsing dates
use DateTime::Duration; # for substracting
use Captcha::reCAPTCHA; # for protection against spams
use Cwd 'abs_path';
use Scalar::Util qw(looks_like_number);

# Global configuration paramters
# This entire block (including skin parsing) could be pushed out to a separate .pm file
my $cgiUrl = $ENV{'SCRIPT_FILENAME'};
my $workingDirectory = dirname($cgiUrl);
my $metacatProps = "${workingDirectory}/../WEB-INF/metacat.properties";
my $properties = new Config::Properties();
unless (open (METACAT_PROPERTIES, $metacatProps)) {
    print "Content-type: text/html\n\n";
    print "Unable to locate Metacat properties. Working directory is set as " . 
        $workingDirectory .", is this correct?";
    exit(0);
}

$properties->load(*METACAT_PROPERTIES);

# local directory configuration
my $skinsDir = "${workingDirectory}/../style/skins";
my $templatesDir = abs_path("${workingDirectory}/../style/common/templates");
my $tempDir = $properties->getProperty('application.tempDir');

# url configuration
my $server = $properties->splitToTree(qr/\./, 'server');
my $protocol = 'http://';
if ($properties->getProperty('server.https') eq 'true') {
    $protocol = 'https://';
}
my $serverUrl = $protocol . $properties->getProperty('server.name');
my $port = $properties->getProperty('server.port');
if ($port ne '80' && $port ne '443') {
        $serverUrl = $serverUrl . ':' . $port;
}
my $context = $properties->getProperty('application.context');
my $contextUrl = $serverUrl . '/' .  $context;

debug("The context url is: " . $contextUrl);
my $metacatUrl = $contextUrl . "/metacat";
my $cgiPrefix = "/" . $context . "/cgi-bin";
my $styleSkinsPath = $contextUrl . "/style/skins";
my $styleCommonPath = $contextUrl . "/style/common";
my $caCertFileProp = $properties->getProperty('ldap.server.ca.certificate');
my $ldapServerCACertFile;
if ($caCertFileProp eq "") {
   $ldapServerCACertFile = "/etc/ssl/certs/ca-certificates.crt";
   debug("Metacat doesn't specify the ca file, we use the default one " . $ldapServerCACertFile);
} else {
   $ldapServerCACertFile = $workingDirectory. "/../" . $properties->getProperty('ldap.server.ca.certificate');
   debug("Metacat does specify the ca file, we will use it - " . $ldapServerCACertFile);
}


#recaptcha key information
my $recaptchaPublicKey=$properties->getProperty('ldap.recaptcha.publickey');
my $recaptchaPrivateKey=$properties->getProperty('ldap.recaptcha.privatekey');

my @errorMessages;
my $error = 0;

my $emailVerification= 'emailverification';

 my $dn_store_next_uid=$properties->getProperty('ldap.nextuid.storing.dn');
 my $attribute_name_store_next_uid = $properties->getProperty('ldap.nextuid.storing.attributename');

# Import all of the HTML form fields as variables
import_names('FORM');

# Must have a config to use Metacat
my $skinName = "";
# the skinDisplayName is used to prevent the cross-site scripting attack.
my $skinDisplayName="";
if ($FORM::cfg) {
    $skinName = $FORM::cfg;
    $skinDisplayName=$skinName;
    $skinDisplayName=~s/[^A-Za-z0-9 ]*//g;
} elsif ($ARGV[0]) {
    $skinName = $ARGV[0];
    $skinDisplayName=$skinName;
    $skinDisplayName=~s/[^A-Za-z0-9 ]*//g;
} else {
    debug("No configuration set.");
    print "Content-type: text/html\n\n";
    print 'LDAPweb Error: The registry requires a skin name to continue.';
    exit();
}

# Metacat isn't initialized, the registry will fail in strange ways.
if (!($metacatUrl)) {
    debug("No Metacat.");
    print "Content-type: text/html\n\n";
    'Registry Error: Metacat is not initialized! Make sure' .
        ' MetacatUrl is set correctly in ' .  $skinDisplayName . '.properties';
    exit();
}

my $skinProperties = new Config::Properties();
if (!($skinName)) {
    $error = "Application misconfigured.  Please contact the administrator.";
    push(@errorMessages, $error);
} else {
    my $skinProps = "$skinsDir/$skinName/$skinName.properties";
    my $skinDisplayProps = "$skinsDir/$skinDisplayName/$skinDisplayName.properties";
    unless (open (SKIN_PROPERTIES, $skinProps)) {
        print "Content-type: text/html\n\n";
        print "Unable to locate skin properties at $skinDisplayProps.  Is this path correct?";
        exit(0);
    }
    $skinProperties->load(*SKIN_PROPERTIES);
}

my $config = $skinProperties->splitToTree(qr/\./, 'registry.config');

# XXX HACK: this is a temporary fix to pull out the UCNRS password property from the
#           NRS skin instead of metacat.properties. The intent is to prevent editing
#           of our core properties file, which is manipulated purely through the web.
#           Once organizations are editable, this section should be removed as should
#           the properties within nrs/nrs.properties.
my $nrsProperties = new Config::Properties();
my $nrsProps = "$skinsDir/nrs/nrs.properties";
unless (open (NRS_PROPERTIES, $nrsProps)) {
    print "Content-type: text/html\n\n";
    print "Unable to locate skin properties at $nrsProps.  Is this path correct?";
    exit(0);
}
$nrsProperties->load(*NRS_PROPERTIES);

my $nrsConfig = $nrsProperties->splitToTree(qr/\./, 'registry.config');

# XXX END HACK

my $useStartTLS = $properties->getProperty('ldap.web.startTLS');

my $searchBase;
my $ldapUsername;
my $ldapPassword;
# TODO: when should we use surl instead? Is there a setting promoting one over the other?
# TODO: the default tree for accounts should be exposed somewhere, defaulting to unaffiliated
my $ldapurl;
if($useStartTLS eq 'true') {
   $ldapurl = $properties->getProperty('auth.url');
} else {
   $ldapurl = $properties->getProperty('auth.surl');
}
 

# Java uses miliseconds, Perl expects whole seconds
my $timeout = $properties->getProperty('ldap.connectTimeLimit') / 1000;

# Get the CGI input variables
my $query = new CGI;
my $debug = 1;

#--------------------------------------------------------------------------80c->
# Set up the Template Toolkit to read html form templates

# templates hash, imported from ldap.templates tree in metacat.properties
my $templates = $properties->splitToTree(qr/\./, 'ldap.templates');
$$templates{'header'} = $skinProperties->getProperty("registry.templates.header");
$$templates{'footer'} = $skinProperties->getProperty("registry.templates.footer");

# set some configuration options for the template object
my $ttConfig = {
             INCLUDE_PATH => $templatesDir,
             INTERPOLATE  => 0,
             POST_CHOMP   => 1,
             DEBUG        => 1, 
             };

# create an instance of the template
my $template = Template->new($ttConfig) || handleGeneralServerFailure($Template::ERROR);

# custom LDAP properties hash
my $ldapCustom = $properties->splitToTree(qr/\./, 'ldap');

# This is a hash which has the keys of the organization's properties 'name', 'base', 'organization'.
my $orgProps = $properties->splitToTree(qr/\./, 'organization');

#This is a hash which has the keys of the ldap sub tree names of the organizations, such as 'NCEAS', 'LTER' and 'KU', and values are real name of the organization.
my $orgNames = $properties->splitToTree(qr/\./, 'organization.name');
# pull out properties available e.g. 'name', 'base'
my @orgData = keys(%$orgProps);

my @orgList; #An array has the names (i.e, sub tree names, such as 'NCEAS', 'LTER' and 'KU')  of the all organizations in the metacat.properties. 
while (my ($oKey, $oVal) = each(%$orgNames)) {
    push(@orgList, $oKey);
}

my $authBase = $properties->getProperty("auth.base");
my $ldapConfig;
foreach my $o (@orgList) {
    foreach my $d (@orgData) {
        $ldapConfig->{$o}{$d} = $properties->getProperty("organization.$d.$o");
    }

    # XXX hack, remove after 1.9
    if ($o eq 'UCNRS') {
        $ldapConfig->{'UCNRS'}{'base'} = $nrsConfig->{'base'};
        $ldapConfig->{'UCNRS'}{'user'} = $nrsConfig->{'username'};
        $ldapConfig->{'UCNRS'}{'password'} = $nrsConfig->{'password'};
    }

    # set default base
    if (!$ldapConfig->{$o}{'base'}) {
        $ldapConfig->{$o}{'base'} = $authBase;
    }

    # include filter information. By default, our filters are 'o=$name', e.g. 'o=NAPIER'
    # these can be overridden by specifying them in metacat.properties. Non-default configs
    # such as UCNRS must specify all LDAP properties.
    if ($ldapConfig->{$o}{'base'} eq $authBase) {
        my $filter = "o=$o";
        if (!$ldapConfig->{$o}{'org'}) {
            $ldapConfig->{$o}{'org'} = $filter;
        }
        if (!$ldapConfig->{$o}{'filter'}) {
            #$ldapConfig->{$o}{'filter'} = $filter;
            $ldapConfig->{$o}{'filter'} = $ldapConfig->{$o}{'org'};
        }
        # also include DN, which is just org + base
        if ($ldapConfig->{$o}{'org'}) {
            $ldapConfig->{$o}{'dn'} = $ldapConfig->{$o}{'org'} . "," . $ldapConfig->{$o}{'base'};
        }
    } else {
        $ldapConfig->{$o}{'dn'} = $ldapConfig->{$o}{'base'};
    }
    
    # set LDAP administrator user account
    if (!$ldapConfig->{$o}{'user'}) {
        $ldapConfig->{$o}{'user'} = $ldapConfig->{'unaffiliated'}{'user'};
    }
    # check for a fully qualified LDAP name. If it doesn't exist, append base.
    my @userParts = split(',', $ldapConfig->{$o}{'user'});
    if (scalar(@userParts) == 1) {
        $ldapConfig->{$o}{'user'} = $ldapConfig->{$o}{'user'} . "," . $ldapConfig->{$o}{'base'};
    }

    if (!$ldapConfig->{$o}{'password'}) {
        $ldapConfig->{$o}{'password'} = $ldapConfig->{'unaffiliated'}{'password'};
    }
}

### Determine the display organization list (such as NCEAS, Account ) in the ldap template files
my $displayOrgListStr;
$displayOrgListStr = $skinProperties->getProperty("ldap.templates.organizationList") or $displayOrgListStr = $properties->getProperty('ldap.templates.organizationList');
debug("the string of the org from properties : " . $displayOrgListStr);
my @displayOrgList = split(';', $displayOrgListStr);

my @validDisplayOrgList; #this array contains the org list which will be shown in the templates files.

my %orgNamesHash = %$orgNames;
foreach my $element (@displayOrgList) {
    if(exists $orgNamesHash{$element}) {
         my $label = $ldapConfig->{$element}{'label'};
         my %displayHash;
         $displayHash{$element} = $label;
         debug("push a hash containing the key " . $element . "with the value label" . $label . " into the display array");
         #if the name is found in the organization part of metacat.properties, put it into the valid array
         push(@validDisplayOrgList, \%displayHash);
    } 
    
}

if(!@validDisplayOrgList) {
     my $sender;
     my $contact;
     $sender = $skinProperties->getProperty("email.sender") or $sender = $properties->getProperty('email.sender');
     $contact = $skinProperties->getProperty("email.contact") or $contact = $properties->getProperty('email.contact');
    print "Content-type: text/html\n\n";
    print "The value of property ldap.templates.organizationList in " 
     . $skinDisplayName . ".properties file or metacat.properties file (if the property doesn't exist in the " 
     . $skinDisplayName . ".properties file) is invalid. Please send the information to ". $contact;
    exit(0);
}


#--------------------------------------------------------------------------80c->
# Define the main program logic that calls subroutines to do the work
#--------------------------------------------------------------------------80c->

# The processing step we are handling
my $stage = $query->param('stage') || $templates->{'stage'};

my $cfg = $query->param('cfg');
debug("started with stage $stage, cfg $cfg");

# define the possible stages
my %stages = (
              'initregister'      => \&handleInitRegister,
              'register'          => \&handleRegister,
              'registerconfirmed' => \&handleRegisterConfirmed,
              'simplesearch'      => \&handleSimpleSearch,
              'initaddentry'      => \&handleInitAddEntry,
              'addentry'          => \&handleAddEntry,
              'initmodifyentry'   => \&handleInitModifyEntry,
              'modifyentry'       => \&handleModifyEntry,
              'changepass'        => \&handleChangePassword,
              'initchangepass'    => \&handleInitialChangePassword,
              'resetpass'         => \&handleResetPassword,
              'initresetpass'     => \&handleInitialResetPassword,
              'emailverification' => \&handleEmailVerification,
              'lookupname'        => \&handleLookupName,
              'searchnamesbyemail'=> \&handleSearchNameByEmail,
              #'getnextuid'        => \&getExistingHighestUidNum,
             );

# call the appropriate routine based on the stage
if ( $stages{$stage} ) {
  $stages{$stage}->();
} else {
  &handleResponseMessage();
}

#--------------------------------------------------------------------------80c->
# Define the subroutines to do the work
#--------------------------------------------------------------------------80c->

sub clearTemporaryAccounts {
	
    #search accounts that have expired
	my $org = $query->param('o'); 
    my $ldapUsername = $ldapConfig->{$org}{'user'};
    my $ldapPassword = $ldapConfig->{$org}{'password'};
    my $orgAuthBase = $ldapConfig->{$org}{'base'};
    my $orgExpiration = $ldapConfig->{$org}{'expiration'};
    my $tmpSearchBase = 'dc=tmp,' . $orgAuthBase; 
	
	my $dt = DateTime->now;
	$dt->subtract( hours => $orgExpiration );
	my $expirationDate = $dt->ymd("") . $dt->hms("") . "Z";
    my $filter = "(&(objectClass=inetOrgPerson)(createTimestamp<=" . $expirationDate . "))";
    debug("Clearing expired accounts with filter: " . $filter . ", base: " . $tmpSearchBase);    
    my @attrs = [ 'uid', 'o', 'ou', 'cn', 'mail', 'telephoneNumber', 'title' ];

    my $ldap;
    my $mesg;
    
    my $dn;

    #if main ldap server is down, a html file containing warning message will be returned
    debug("clearTemporaryAccounts: connecting to $ldapurl, $timeout");
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
        if($useStartTLS eq 'true') {
                $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
        }
        $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword ); 
		$mesg = $ldap->search (
			base   => $tmpSearchBase,
			filter => $filter,
			attrs => \@attrs,
		);
	    if ($mesg->count() > 0) {
			my $entry;
			foreach $entry ($mesg->all_entries) { 
            	$dn = $entry->dn();
            	# remove the entry
   				debug("Removing expired account: " . $dn);
            	$ldap->delete($dn);
			}
        }
    	$ldap->unbind;   # take down session
    }

    return 0;
}

sub fullTemplate {
    my $templateList = shift;
    my $templateVars = setVars(shift);
    my $c = Captcha::reCAPTCHA->new;
    my $captcha = 'captcha';
    #my $error=null;
    my $use_ssl= 1;
    #my $options=null;
    # use the AJAX style, only need to provide the public key to the template
    $templateVars->{'recaptchaPublicKey'} = $recaptchaPublicKey;
    #$templateVars->{$captcha} = $c->get_html($recaptchaPublicKey,undef, $use_ssl, undef);
    $template->process( $templates->{'header'}, $templateVars );
    foreach my $tmpl (@{$templateList}) {
        $template->process( $templates->{$tmpl}, $templateVars );
    }
    $template->process( $templates->{'footer'}, $templateVars );
}


#
# Initialize a form for a user to request the account name associated with an email address
#
sub handleLookupName {
    
    print "Content-type: text/html\n\n";
    # process the template files:
    fullTemplate(['lookupName']); 
    exit();
}

#
# Handle the user's request to look up account names with a specified email address.
# This relates to "Forget your user name"
#
sub handleSearchNameByEmail{

    print "Content-type: text/html\n\n";
   
    my $allParams = {'mail' => $query->param('mail')};
    my @requiredParams = ('mail');
    if (! paramsAreValid(@requiredParams)) {
        my $errorMessage = "Required information is missing. " .
            "Please fill in all required fields and resubmit the form.";
        fullTemplate(['lookupName'], { allParams => $allParams,
                                     errorMessage => $errorMessage });
        exit();
    }
    my $mail = $query->param('mail');
    
    #search accounts with the specified emails 
    $searchBase = $authBase; 
    my $filter = "(mail=" . $mail . ")";
    my @attrs = [ 'uid', 'o', 'ou', 'cn', 'mail', 'telephoneNumber', 'title' ];
    my $notHtmlFormat = 1;
    my $found = findExistingAccounts($ldapurl, $searchBase, $filter, \@attrs, $notHtmlFormat);
    my $accountInfo;
    if ($found) {
        $accountInfo = $found;
    } else {
        $accountInfo = "There are no accounts associated with the email " . $mail . ".\n";
    }

    my $mailhost = $properties->getProperty('email.mailhost');
    my $sender;
    my $contact;
    $sender = $skinProperties->getProperty("email.sender") or $sender = $properties->getProperty('email.sender');
    $contact = $skinProperties->getProperty("email.contact") or $contact = $properties->getProperty('email.contact');
    debug("the sender is " . $sender);
    debug("the contact is " . $contact);
    my $recipient = $query->param('mail');
    # Send the email message to them
    my $smtp = Net::SMTP->new($mailhost) or do {  
                                                  fullTemplate( ['lookupName'], {allParams => $allParams, 
                                                                errorMessage => "Our mail server currently is experiencing some difficulties. Please contact " . 
                                                                $skinProperties->getProperty("email.recipient") . "." });  
                                                  exit(0);
                                               };
    $smtp->mail($sender);
    $smtp->to($recipient);

    my $message = <<"     ENDOFMESSAGE";
    To: $recipient
    From: $sender
    Subject: Your Account Information
        
    Somebody (hopefully you) looked up the account information associated with the email address.  
    Here is the account information:
    
    $accountInfo

    Thanks,
        $sender
    
     ENDOFMESSAGE
     $message =~ s/^[ \t\r\f]+//gm;
    
     $smtp->data($message);
     $smtp->quit;
     fullTemplate( ['lookupNameSuccess'] );
    
}


#
# create the initial registration form 
#
sub handleInitRegister {
  my $vars = shift;
  print "Content-type: text/html\n\n";
  # process the template files:
  fullTemplate(['register'], {stage => "register"}); 
  exit();
}



#
# process input from the register stage, which occurs when
# a user submits form data to create a new account
#
sub handleRegister {
    
    #print "Content-type: text/html\n\n";
    if ($query->param('o') =~ "LTER") {
      print "Content-type: text/html\n\n";
      fullTemplate( ['registerLter'] );
      exit(0);
    } 
    
    my $allParams = { 'givenName' => $query->param('givenName'), 
                      'sn' => $query->param('sn'),
                      'o' => $query->param('o'), 
                      'mail' => $query->param('mail'), 
                      'uid' => $query->param('uid'), 
                      'userPassword' => $query->param('userPassword'), 
                      'userPassword2' => $query->param('userPassword2'), 
                      'title' => $query->param('title'), 
                      'telephoneNumber' => $query->param('telephoneNumber') };
    
    # Check the recaptcha
    my $c = Captcha::reCAPTCHA->new;
    #my $challenge = $query->param('recaptcha_challenge_field');
    my $response = $query->param('g-recaptcha-response');
    if ($response) {
       #do nothing
       debug("users passed the test");
    } else {
       debug("users didn't pass the test and reset the reponse to error");
       $response="error";
    }
    #debug("the reponse of recaptcha is $response");
    # Verify submission (v2 version)
    my $result = $c->check_answer_v2($recaptchaPrivateKey, $response, $ENV{REMOTE_ADDR});

    if ( $result->{is_valid} ) {
        #print "Yes!";
        #exit();
    }
    else {
        print "Content-type: text/html\n\n";
        my $errorMessage = "The verification code is wrong. Please input again.";
        fullTemplate(['register'], { stage => "register",
                                     allParams => $allParams,
                                     errorMessage => $errorMessage });
        exit();
    }
    
    
    # Check that all required fields are provided and not null
    my @requiredParams = ( 'givenName', 'sn', 'o', 'mail', 
                           'uid', 'userPassword', 'userPassword2');
    if (! paramsAreValid(@requiredParams)) {
        print "Content-type: text/html\n\n";
        my $errorMessage = "Required information is missing. " .
            "Please fill in all required fields and resubmit the form.";
        fullTemplate(['register'], { stage => "register",
                                     allParams => $allParams,
                                     errorMessage => $errorMessage });
        exit();
    } else {
         if ($query->param('userPassword') ne $query->param('userPassword2')) {
            print "Content-type: text/html\n\n";
            my $errorMessage = "The passwords do not match. Try again.";
            fullTemplate( ['registerFailed', 'register'], { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $errorMessage });
            exit();
        }
        my $o = $query->param('o');    
        $searchBase = $ldapConfig->{$o}{'base'};  
    }
    
    # Remove any expired temporary accounts for this subtree before continuing
    clearTemporaryAccounts();
    
    # Check if the uid was taken in the production space
    my @attrs = [ 'uid', 'o', 'ou', 'cn', 'mail', 'telephoneNumber', 'title' ];
    my $uidExists;
    my $uid=$query->param('uid');
    my $uidFilter = "uid=" . $uid;
    my $newSearchBase = $ldapConfig->{$query->param('o')}{'org'} . "," .  $searchBase;
    debug("the new search base is $newSearchBase");
    $uidExists = uidExists($ldapurl, $newSearchBase, $uidFilter, \@attrs);
    debug("the result of uidExists $uidExists");
    if($uidExists) {
         print "Content-type: text/html\n\n";
            my $errorMessage = $uidExists;
            fullTemplate( ['registerFailed', 'register'], { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $errorMessage });
            exit();
    }

    # Search LDAP for matching entries that already exist
    # Some forms use a single text search box, whereas others search per
    # attribute.
    my $filter;
    if ($query->param('searchField')) {

      $filter = "(|" . 
                "(uid=" . $query->param('searchField') . ") " .
                "(mail=" . $query->param('searchField') . ")" .
                "(&(sn=" . $query->param('searchField') . ") " . 
                "(givenName=" . $query->param('searchField') . "))" . 
                ")";
    } else {
      $filter = "(|" . 
                "(uid=" . $query->param('uid') . ") " .
                "(mail=" . $query->param('mail') . ")" .
                "(&(sn=" . $query->param('sn') . ") " . 
                "(givenName=" . $query->param('givenName') . "))" . 
                ")";
    }

    
    my $found = findExistingAccounts($ldapurl, $searchBase, $filter, \@attrs);

    # If entries match, send back a request to confirm new-user creation
    if ($found) {
      print "Content-type: text/html\n\n";
      fullTemplate( ['registerMatch', 'register'], { stage => "registerconfirmed",
                                                     allParams => $allParams,
                                                     foundAccounts => $found });
    # Otherwise, create a new user in the LDAP directory
    } else {
        createTemporaryAccount($allParams);
    }

    exit();
}

#
# process input from the registerconfirmed stage, which occurs when
# a user chooses to create an account despite similarities to other
# existing accounts
#
sub handleRegisterConfirmed {
  
    my $allParams = { 'givenName' => $query->param('givenName'), 
                      'sn' => $query->param('sn'),
                      'o' => $query->param('o'), 
                      'mail' => $query->param('mail'), 
                      'uid' => $query->param('uid'), 
                      'userPassword' => $query->param('userPassword'), 
                      'userPassword2' => $query->param('userPassword2'), 
                      'title' => $query->param('title'), 
                      'telephoneNumber' => $query->param('telephoneNumber') };
    #print "Content-type: text/html\n\n";
    createTemporaryAccount($allParams);
    exit();
}

#
# change a user's password upon request
#
sub handleChangePassword {

    print "Content-type: text/html\n\n";

    my $allParams = { 'test' => "1", };
    if ($query->param('uid')) {
        $$allParams{'uid'} = $query->param('uid');
    }
    if ($query->param('o')) {
        $$allParams{'o'} = $query->param('o');
        my $o = $query->param('o');
        
        $searchBase = $ldapConfig->{$o}{'base'};
    }


    # Check that all required fields are provided and not null
    my @requiredParams = ( 'uid', 'o', 'oldpass', 
                           'userPassword', 'userPassword2');
    if (! paramsAreValid(@requiredParams)) {
        my $errorMessage = "Required information is missing. " .
            "Please fill in all required fields and submit the form.";
        fullTemplate( ['changePass'], { stage => "changepass",
                                        allParams => $allParams,
                                        errorMessage => $errorMessage });
        exit();
    }

    # We have all of the info we need, so try to change the password
    if ($query->param('userPassword') eq $query->param('userPassword2')) {

        my $o = $query->param('o');
        $searchBase = $ldapConfig->{$o}{'base'};
        $ldapUsername = $ldapConfig->{$o}{'user'};
        $ldapPassword = $ldapConfig->{$o}{'password'};

        my $dn = "uid=" . $query->param('uid') . "," . $ldapConfig->{$o}{'dn'};;
        if ($query->param('o') =~ "LTER") {
            fullTemplate( ['registerLter'] );
        } else {
            my $errorMessage = changePassword(
                    $dn, $query->param('userPassword'), 
                    $dn, $query->param('oldpass'), $query->param('o'));
            if ($errorMessage) {
                fullTemplate( ['changePass'], { stage => "changepass",
                                                allParams => $allParams,
                                                errorMessage => $errorMessage });
                exit();
            } else {
                fullTemplate( ['changePassSuccess'], { stage => "changepass",
                                                       allParams => $allParams });
                exit();
            }
        }
    } else {
        my $errorMessage = "The passwords do not match. Try again.";
        fullTemplate( ['changePass'], { stage => "changepass",
                                        allParams => $allParams,
                                        errorMessage => $errorMessage });
        exit();
    }
}

#
# change a user's password upon request - no input params
# only display chagepass template without any error
#
sub handleInitialChangePassword {
    print "Content-type: text/html\n\n";

    my $allParams = { 'test' => "1", };
    my $errorMessage = "";
    fullTemplate( ['changePass'], { stage => "changepass",
                                    errorMessage => $errorMessage });
    exit();
}

#
# reset a user's password upon request
#
sub handleResetPassword {

    print "Content-type: text/html\n\n";

    my $allParams = { 'test' => "1", };
    if ($query->param('uid')) {
        $$allParams{'uid'} = $query->param('uid');
    }
    if ($query->param('o')) {
        $$allParams{'o'} = $query->param('o');
        my $o = $query->param('o');
        
        $searchBase = $ldapConfig->{$o}{'base'};
        $ldapUsername = $ldapConfig->{$o}{'user'};
        $ldapPassword = $ldapConfig->{$o}{'password'};
    }

    # Check that all required fields are provided and not null
    my @requiredParams = ( 'uid', 'o' );
    if (! paramsAreValid(@requiredParams)) {
        my $errorMessage = "Required information is missing. " .
            "Please fill in all required fields and submit the form.";
        fullTemplate( ['resetPass'],  { stage => "resetpass",
                                        allParams => $allParams,
                                        errorMessage => $errorMessage });
        exit();
    }

    # We have all of the info we need, so try to change the password
    my $o = $query->param('o');
    my $dn = "uid=" . $query->param('uid') . "," . $ldapConfig->{$o}{'dn'};
    debug("handleResetPassword: dn: $dn");
    if ($query->param('o') =~ "LTER") {
        fullTemplate( ['registerLter'] );
        exit();
    } else {
        my $errorMessage = "";
        my $recipient;
        my $userPass;
        my $entry = getLdapEntry($ldapurl, $searchBase, 
                $query->param('uid'), $query->param('o'));

        if ($entry) {
            $recipient = $entry->get_value('mail');
            $userPass = getRandomPassword();
            $errorMessage = changePassword($dn, $userPass, $ldapUsername, $ldapPassword, $query->param('o'));
        } else {
            $errorMessage = "User not found in database.  Please try again.";
        }

        if ($errorMessage) {
            fullTemplate( ['resetPass'], { stage => "resetpass",
                                           allParams => $allParams,
                                           errorMessage => $errorMessage });
            exit();
        } else {
            my $errorMessage = sendPasswordNotification($query->param('uid'),
                    $query->param('o'), $userPass, $recipient, $cfg);
            fullTemplate( ['resetPassSuccess'], { stage => "resetpass",
                                                  allParams => $allParams,
                                                  errorMessage => $errorMessage });
            exit();
        }
    }
}

#
# reset a user's password upon request- no initial params
# only display resetpass template without any error
#
sub handleInitialResetPassword {
    print "Content-type: text/html\n\n";
    my $errorMessage = "";
    fullTemplate( ['resetPass'], { stage => "resetpass",
                                   errorMessage => $errorMessage });
    exit();
}

#
# Construct a random string to use for a newly reset password
#
sub getRandomPassword {
    my $length = shift;
    if (!$length) {
        $length = 8;
    }
    my $newPass = "";

    my @chars = ( "A" .. "Z", "a" .. "z", 0 .. 9, qw(! @ $ ^) );
    $newPass = join("", @chars[ map { rand @chars } ( 1 .. $length ) ]);
    return $newPass;
}

#
# Change a password to a new value, binding as the provided user
#
sub changePassword {
    my $userDN = shift;
    my $userPass = shift;
    my $bindDN = shift;
    my $bindPass = shift;
    my $o = shift;

    my $searchBase = $ldapConfig->{$o}{'base'};

    my $errorMessage = 0;
    my $ldap;

    #if main ldap server is down, a html file containing warning message will be returned
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    
    if ($ldap) {
        if($useStartTLS eq 'true') {
             $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
        }
        debug("changePassword: attempting to bind to $bindDN");
        my $bindresult = $ldap->bind( version => 3, dn => $bindDN, 
                                  password => $bindPass );
        if ($bindresult->code) {
            $errorMessage = "Failed to log in. Are you sure your connection credentails are " .
                            "correct? Please correct and try again...";
            return $errorMessage;
        }

    	# Find the user here and change their entry
    	my $newpass = createSeededPassHash($userPass);
    	my $modifications = { userPassword => $newpass };
      debug("changePass: setting password for $userDN to $newpass");
    	my $result = $ldap->modify( $userDN, replace => { %$modifications });
    
    	if ($result->code()) {
            debug("changePass: error changing password: " . $result->error);
        	$errorMessage = "There was an error changing the password:" .
                           "<br />\n" . $result->error;
    	} 
    	$ldap->unbind;   # take down session
    }

    return $errorMessage;
}

#
# generate a Seeded SHA1 hash of a plaintext password
#
sub createSeededPassHash {
    my $secret = shift;

    my $salt = "";
    for (my $i=0; $i < 4; $i++) {
        $salt .= int(rand(10));
    }

    my $ctx = Digest::SHA1->new;
    $ctx->add($secret);
    $ctx->add($salt);
    my $hashedPasswd = '{SSHA}' . encode_base64($ctx->digest . $salt ,'');

    return $hashedPasswd;
}

#
# Look up an ldap entry for a user
#
sub getLdapEntry {
    my $ldapurl = shift;
    my $base = shift;
    my $username = shift;
    my $org = shift;

    my $entry = "";
    my $mesg;
    my $ldap;
    debug("ldap server: $ldapurl");

    #if main ldap server is down, a html file containing warning message will be returned
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    
    if ($ldap) {
        if($useStartTLS eq 'true') {
             $ldap->start_tls( verify => 'none');
             #$ldap->start_tls( verify => 'require',
             #              cafile => $ldapServerCACertFile);
        }
    	my $bindresult = $ldap->bind;
    	if ($bindresult->code) {
        	return $entry;
    	}

        $base = $ldapConfig->{$org}{'org'} . ',' . $base;
        debug("getLdapEntry, searching for $base, (uid=$username)");
        $mesg = $ldap->search ( base   => $base, filter => "(uid=$username)");
    	#if($ldapConfig->{$org}{'filter'}){
            #debug("getLdapEntry: filter set, searching for base=$base, " .
                  #"(&(uid=$username)($ldapConfig->{$org}{'filter'}))");
        	#$mesg = $ldap->search ( base   => $base,
                #filter => "(&(uid=$username)($ldapConfig->{$org}{'filter'}))");
    	#} else {
            #debug("getLdapEntry: no filter, searching for $base, (uid=$username)");
        	#$mesg = $ldap->search ( base   => $base, filter => "(uid=$username)");
    	#}
    
    	if ($mesg->count > 0) {
        	$entry = $mesg->pop_entry;
        	$ldap->unbind;   # take down session
    	} else {
        	$ldap->unbind;   # take down session
        	# Follow references by recursive call to self
        	my @references = $mesg->references();
        	for (my $i = 0; $i <= $#references; $i++) {
            	my $uri = URI->new($references[$i]);
            	my $host = $uri->host();
            	debug("the original reference $host");
            	my $index = index ($host, 'ldaps://');
            	if( $useStartTLS  ne 'true' && $index < 0) {
            	   $host = "ldaps://" . $host . ":636";
            	}
            	debug("the new reference $host");
            	my $path = $uri->path();
            	$path =~ s/^\///;
            	$entry = &getLdapEntry($host, $path, $username, $org);
            	if ($entry) {
                    debug("getLdapEntry: recursion found $host, $path, $username, $org");
                	return $entry;
            	}
        	}
    	}
    }
    return $entry;
}

# 
# send an email message notifying the user of the pw change
#
sub sendPasswordNotification {
    my $username = shift;
    my $org = shift;
    my $newPass = shift;
    my $recipient = shift;
    my $cfg = shift;

    my $errorMessage = "";
    if ($recipient) {
    
        my $mailhost = $properties->getProperty('email.mailhost');
        my $sender;
        my $contact;
        $sender = $skinProperties->getProperty("email.sender") or $sender = $properties->getProperty('email.sender');
        # Send the email message to them
        my $smtp = Net::SMTP->new($mailhost);
        $smtp->mail($sender);
        $smtp->to($recipient);

        my $message = <<"        ENDOFMESSAGE";
        To: $recipient
        From: $sender
        Subject: Your Account Password Reset
        
        Somebody (hopefully you) requested that your account password be reset.  
        Your temporary password is below. Please change it as soon as possible 
        at: $contextUrl/style/skins/account/.

            Username: $username
        Organization: $org
        New Password: $newPass

        Thanks,
            $sender
            $contact
    
        ENDOFMESSAGE
        $message =~ s/^[ \t\r\f]+//gm;
    
        $smtp->data($message);
        $smtp->quit;
    } else {
        $errorMessage = "Failed to send password because I " .
                        "couldn't find a valid email address.";
    }
    return $errorMessage;
}

#
# search the LDAP production space to see if a uid already exists
#
sub uidExists {
    my $ldapurl = shift;
    debug("the ldap ulr is $ldapurl");
    my $base = shift;
    debug("the base is $base");
    my $filter = shift;
    debug("the filter is $filter");
    my $attref = shift;
  
    my $ldap;
    my $mesg;

    my $foundAccounts = 0;

    #if main ldap server is down, a html file containing warning message will be returned
    debug("uidExists: connecting to $ldapurl, $timeout");
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
        if($useStartTLS eq 'true') {
            $ldap->start_tls( verify => 'none');
            #$ldap->start_tls( verify => 'require',
            #              cafile => $ldapServerCACertFile);
        }
        $ldap->bind( version => 3, anonymous => 1);
        $mesg = $ldap->search (
            base   => $base,
            filter => $filter,
            attrs => @$attref,
        );
        debug("the message count is " . $mesg->count());
        if ($mesg->count() > 0) {
            $foundAccounts = "The username has been taken already by another user. Please choose a different one.";
           
        }
        $ldap->unbind;   # take down session
    } else {
        $foundAccounts = "The ldap server is not running";
    }
    return $foundAccounts;
}

#
# search the LDAP directory to see if a similar account already exists
#
sub findExistingAccounts {
    my $ldapurl = shift;
    my $base = shift;
    my $filter = shift;
    my $attref = shift;
    my $notHtmlFormat = shift;
    my $ldap;
    my $mesg;

    my $foundAccounts = 0;

    #if main ldap server is down, a html file containing warning message will be returned
    debug("findExistingAccounts: connecting to $ldapurl, $timeout");
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
        if($useStartTLS eq 'true') {
                $ldap->start_tls( verify => 'none');
                #$ldap->start_tls( verify => 'require',
                #              cafile => $ldapServerCACertFile);
        }
    	$ldap->bind( version => 3, anonymous => 1);
		$mesg = $ldap->search (
			base   => $base,
			filter => $filter,
			attrs => @$attref,
		);

	    if ($mesg->count() > 0) {
			$foundAccounts = "";
			my $entry;
			foreach $entry ($mesg->all_entries) { 
                # a fix to ignore 'ou=Account' properties which are not usable accounts within Metacat.
                # this could be done directly with filters on the LDAP connection, instead.
                #if ($entry->dn !~ /ou=Account/) {
                    if($notHtmlFormat) {
                        $foundAccounts .= "\nAccount: ";
                    } else {
                        $foundAccounts .= "<p>\n<b><u>Account:</u> ";
                    }
                    $foundAccounts .= $entry->dn();
                    if($notHtmlFormat) {
                        $foundAccounts .= "\n";
                    } else {
                        $foundAccounts .= "</b><br />\n";
                    }
                    foreach my $attribute ($entry->attributes()) {
                        my $value = $entry->get_value($attribute);
                        $foundAccounts .= "$attribute: ";
                        $foundAccounts .= $value;
                         if($notHtmlFormat) {
                            $foundAccounts .= "\n";
                        } else {
                            $foundAccounts .= "<br />\n";
                        }
                    }
                    if($notHtmlFormat) {
                        $foundAccounts .= "\n";
                    } else {
                        $foundAccounts .= "</p>\n";
                    }
                    
                #}
			}
        }
    	$ldap->unbind;   # take down session

    	# Follow references
    	my @references = $mesg->references();
    	for (my $i = 0; $i <= $#references; $i++) {
        	my $uri = URI->new($references[$i]);
        	my $host = $uri->host();
        	debug("the original reference $host");
        my $index = index ($host, 'ldaps://');
        if( $useStartTLS  ne 'true' && $index < 0) {
            $host = "ldaps://" . $host . ":636";
        }
        debug("the new reference $host");
        	my $path = $uri->path();
        	$path =~ s/^\///;
        	my $refFound = &findExistingAccounts($host, $path, $filter, $attref, $notHtmlFormat);
        	if ($refFound) {
            	$foundAccounts .= $refFound;
        	}
    	}
    }

    #print "<p>Checking referrals...</p>\n";
    #my @referrals = $mesg->referrals();
    #print "<p>Referrals count: ", scalar(@referrals), "</p>\n";
    #for (my $i = 0; $i <= $#referrals; $i++) {
        #print "<p>Referral: ", $referrals[$i], "</p>\n";
    #}

    return $foundAccounts;
}

#
# Validate that we have the proper set of input parameters
#
sub paramsAreValid {
    my @pnames = @_;

    my $allValid = 1;
    foreach my $parameter (@pnames) {
        if (!defined($query->param($parameter)) || 
            ! $query->param($parameter) ||
            $query->param($parameter) =~ /^\s+$/) {
            $allValid = 0;
        }
    }

    return $allValid;
}

#
# Create a temporary account for a user and send an email with a link which can click for the
# verification. This is used to protect the ldap server against spams.
#
sub createTemporaryAccount {
    my $allParams = shift;
    my $org = $query->param('o'); 
    my $ldapUsername = $ldapConfig->{$org}{'user'};
    my $ldapPassword = $ldapConfig->{$org}{'password'};
    my $tmp = 1;

    ################## Search LDAP to see if the dc=tmp which stores the inactive accounts exist or not. If it doesn't exist, it will be generated
    my $orgAuthBase = $ldapConfig->{$org}{'base'};
    my $tmpSearchBase = 'dc=tmp,' . $orgAuthBase; 
    my $tmpFilter = "dc=tmp";
    my @attributes=['dc'];
    my $foundTmp = searchDirectory($ldapurl, $orgAuthBase, $tmpFilter, \@attributes);
    if (!$foundTmp) {
        my $dn = $tmpSearchBase;
        my $additions = [ 
                    'dc' => 'tmp',
                    'o'  => 'tmp',
                    'objectclass' => ['top', 'dcObject', 'organization']
                    ];
        createItem($dn, $ldapUsername, $ldapPassword, $additions, $tmp, $allParams);
    } else {
     debug("found the tmp space");
    }
    
    ################## Search LDAP for matching o or ou under the dc=tmp that already exist. If it doesn't exist, it will be generated
    my $filter = $ldapConfig->{$org}{'filter'};   
    
    debug("search filer " . $filter);
    debug("ldap server ". $ldapurl);
    debug("sesarch base " . $tmpSearchBase);
    #print "Content-type: text/html\n\n";
    my @attrs = ['o', 'ou' ];
    my $found = searchDirectory($ldapurl, $tmpSearchBase, $filter, \@attrs);

    my @organizationInfo = split('=', $ldapConfig->{$org}{'org'}); #split 'o=NCEAS' or something like that
    my $organization = $organizationInfo[0]; # This will be 'o' or 'ou'
    my $organizationName = $organizationInfo[1]; # This will be 'NCEAS' or 'Account'
        
    if(!$found) {
        debug("generate the subtree in the dc=tmp===========================");
        #need to generate the subtree o or ou
        my $additions;
            if($organization eq 'ou') {
                $additions = [ 
                    $organization   => $organizationName,
                    'objectclass' => ['top', 'organizationalUnit']
                    ];
            
            } else {
                $additions = [ 
                    $organization   => $organizationName,
                    'objectclass' => ['top', 'organization']
                    ];
            
            } 
        my $dn=$ldapConfig->{$org}{'org'} . ',' . $tmpSearchBase;
        createItem($dn, $ldapUsername, $ldapPassword, $additions, $tmp, $allParams);
    } 
    
    ################create an account under tmp subtree 
    
     my $dn_store_next_uid=$properties->getProperty('ldap.nextuid.storing.dn');
    my $attribute_name_store_next_uid = $properties->getProperty('ldap.nextuid.storing.attributename');
    #get the next avaliable uid number. If it fails, the program will exist.
    my $nextUidNumber = getNextUidNumber($ldapUsername, $ldapPassword);
    if(!$nextUidNumber) {
        print "Content-type: text/html\n\n";
         my $sender;
         my $contact;
        $sender = $skinProperties->getProperty("email.recipient") or $sender = $properties->getProperty('email.recipient');
        $contact = $skinProperties->getProperty("email.contact") or $contact = $properties->getProperty('email.contact');
        my $errorMessage = "The Identity Service can't get the next avaliable uid number. Please try again.  If the issue persists, please contact the administrator - $contact.
                           The possible reasons are: the dn - $dn_store_next_uid or its attribute - $attribute_name_store_next_uid don't exist; the value of the attribute - $attribute_name_store_next_uid
                           is not a number; or lots of users were registering and you couldn't get a lock on the dn - $dn_store_next_uid.";
        fullTemplate(['register'], { stage => "register",
                                     allParams => $allParams,
                                     errorMessage => $errorMessage });
        exit(0);
    }
    my $cn = join(" ", $query->param('givenName'), $query->param('sn')); 
    #generate a randomstr for matching the email.
    my $randomStr = getRandomPassword(16);
    # Create a hashed version of the password
    my $shapass = createSeededPassHash($query->param('userPassword'));
    my $additions = [ 
                'uid'   => $query->param('uid'),
                'cn'   => $cn,
                'sn'   => $query->param('sn'),
                'givenName'   => $query->param('givenName'),
                'mail' => $query->param('mail'),
                'userPassword' => $shapass,
                'employeeNumber' => $randomStr,
                'uidNumber' => $nextUidNumber,
                'gidNumber' => $nextUidNumber,
                'loginShell' => '/sbin/nologin',
                'homeDirectory' => '/dev/null',
                'objectclass' => ['top', 'person', 'organizationalPerson', 
                                'inetOrgPerson', 'posixAccount', 'shadowAccount' ],
                $organization   => $organizationName
                ];
    my $gecos;
    if (defined($query->param('telephoneNumber')) && 
                $query->param('telephoneNumber') &&
                ! $query->param('telephoneNumber') =~ /^\s+$/) {
                $$additions[$#$additions + 1] = 'telephoneNumber';
                $$additions[$#$additions + 1] = $query->param('telephoneNumber');
                $gecos = $cn . ',,'. $query->param('telephoneNumber'). ',';
    } else {
        $gecos = $cn . ',,,';
    }
    
    $$additions[$#$additions + 1] = 'gecos';
    $$additions[$#$additions + 1] = $gecos;
    
    if (defined($query->param('title')) && 
                $query->param('title') &&
                ! $query->param('title') =~ /^\s+$/) {
                $$additions[$#$additions + 1] = 'title';
                $$additions[$#$additions + 1] = $query->param('title');
    }

    
    #$$additions[$#$additions + 1] = 'o';
    #$$additions[$#$additions + 1] = $org;
    my $dn='uid=' . $query->param('uid') . ',' . $ldapConfig->{$org}{'org'} . ',' . $tmpSearchBase;
    createItem($dn, $ldapUsername, $ldapPassword, $additions, $tmp, $allParams);
    
    
    ####################send the verification email to the user
    my $link = '/' . $context . '/cgi-bin/ldapweb.cgi?cfg=' . $skinName . '&' . 'stage=' . $emailVerification . '&' . 'dn=' . $dn . '&' . 'hash=' . $randomStr . '&o=' . $org . '&uid=' . $query->param('uid'); #even though we use o=something. The emailVerification will figure the real o= or ou=something.
    
    my $overrideURL;
    $overrideURL = $skinProperties->getProperty("email.overrideURL");
    debug("the overrideURL is $overrideURL");
    if (defined($overrideURL) && !($overrideURL eq '')) {
    	$link = $serverUrl . $overrideURL . $link;
    } else {
    	$link = $serverUrl . $link;
    }
    
    my $mailhost = $properties->getProperty('email.mailhost');
    my $sender;
    my $contact;
    $sender = $skinProperties->getProperty("email.sender") or $sender = $properties->getProperty('email.sender');
    $contact = $skinProperties->getProperty("email.contact") or $contact = $properties->getProperty('email.contact');
    debug("the sender is " . $sender);
    debug("the contact is :" . $contact);
    my $recipient = $query->param('mail');
    # Send the email message to them
    my $smtp = Net::SMTP->new($mailhost) or do {  
                                                  fullTemplate( ['registerFailed'], {errorMessage => "The temporary account " . $dn . " was created successfully. However, the vertification email can't be sent to you because the email server has some issues. Please contact " . 
                                                  $skinProperties->getProperty("email.recipient") . "." });  
                                                  exit(0);
                                               };
    $smtp->mail($sender);
    $smtp->to($recipient);

    my $message = <<"     ENDOFMESSAGE";
    To: $recipient
    From: $sender
    Subject: New Account Activation
        
    Somebody (hopefully you) registered an account on $contextUrl/style/skins/account/.  
    Please click the following link to activate your account.
    If the link doesn't work, please copy the link to your browser:
    
    $link

    Thanks,
        $sender
        $contact
    
     ENDOFMESSAGE
     $message =~ s/^[ \t\r\f]+//gm;
    
     $smtp->data($message);
     $smtp->quit;
    debug("the link is " . $link);
    fullTemplate( ['success'] );
    
}

#
# Bind to LDAP and create a new item (a user or subtree) using the information provided
# by the user
#
sub createItem {
    my $dn = shift;
    my $ldapUsername = shift;
    my $ldapPassword = shift;
    my $additions = shift;
    my $temp = shift; #if it is for a temporary account.
    my $allParams = shift;
    
    my @failureTemplate;
    if($temp){
        @failureTemplate = ['registerFailed', 'register'];
    } else {
        @failureTemplate = ['registerFailed'];
    }
    print "Content-type: text/html\n\n";
    debug("the dn is " . $dn);
    debug("LDAP connection to $ldapurl...");    
    debug("the ldap ca certificate is " . $ldapServerCACertFile);
    #if main ldap server is down, a html file containing warning message will be returned
    my $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
        if($useStartTLS eq 'true') {
            $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
        }
            debug("Attempting to bind to LDAP server with dn = $ldapUsername, pwd = $ldapPassword");
            $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword ); 
            my $result = $ldap->add ( 'dn' => $dn, 'attr' => [@$additions ]);
            if ($result->code()) {
                fullTemplate(@failureTemplate, { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $result->error });
                exist(0);
                # TODO SCW was included as separate errors, test this
                #$templateVars    = setVars({ stage => "register",
                #                     allParams => $allParams });
                #$template->process( $templates->{'register'}, $templateVars);
            } else {
                #fullTemplate( ['success'] );
            }
            $ldap->unbind;   # take down session
            
    } else {   
         fullTemplate(@failureTemplate, { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => "The ldap server is not available now. Please try it later"});
         exit(0);
    }
  
}






#
# This subroutine will handle a email verification:
# If the hash string matches the one store in the ldap, the account will be
# copied from the temporary space to the permanent tree and the account in 
# the temporary space will be removed.
sub handleEmailVerification {

    my $cfg = $query->param('cfg');
    my $dn = $query->param('dn');
    my $hash = $query->param('hash');
    my $org = $query->param('o');
    my $uid = $query->param('uid');
    
    my $ldapUsername;
    my $ldapPassword;
    #my $orgAuthBase;

    $ldapUsername = $ldapConfig->{$org}{'user'};
    $ldapPassword = $ldapConfig->{$org}{'password'};
    #$orgAuthBase = $ldapConfig->{$org}{'base'};
    
    debug("LDAP connection to $ldapurl...");    
    

   print "Content-type: text/html\n\n";
   #if main ldap server is down, a html file containing warning message will be returned
   my $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
   if ($ldap) {
        if($useStartTLS eq 'true') {
            $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
        }
        $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword );
        my $mesg = $ldap->search(base => $dn, scope => 'base', filter => '(objectClass=*)'); #This dn is with the dc=tmp. So it will find out the temporary account registered in registration step.
        my $max = $mesg->count;
        debug("the count is " . $max);
        if($max < 1) {
            $ldap->unbind;   # take down session
            fullTemplate( ['verificationFailed'], {errorMessage => "No record matched the dn " . $dn . " for the activation. You probably already activated the account."});
            #handleLDAPBindFailure($ldapurl);
            exit(0);
        } else {
            #check if the hash string match
            my $entry = $mesg->entry (0);
            my $hashStrFromLdap = $entry->get_value('employeeNumber');
            if( $hashStrFromLdap eq $hash) {
                #my $additions = [ ];
                #foreach my $attr ( $entry->attributes ) {
                    #if($attr ne 'employeeNumber') {
                        #$$additions[$#$additions + 1] = $attr;
                        #$$additions[$#$additions + 1] = $entry->get_value( $attr );
                    #}
                #}

                
                my $orgDn = $ldapConfig->{$org}{'dn'}; #the DN for the organization.
                $mesg = $ldap->moddn(
                            dn => $dn,
                            deleteoldrdn => 1,
                            newrdn => "uid=" . $uid,
                            newsuperior  =>  $orgDn);
                $ldap->unbind;   # take down session
                if($mesg->code()) {
                    fullTemplate( ['verificationFailed'], {errorMessage => "Cannot move the account from the inactive area to the ative area since " . $mesg->error()});
                    exit(0);
                } else {
                    fullTemplate( ['verificationSuccess'] );
                }
                #createAccount2($dn, $ldapUsername, $ldapPassword, $additions, $tmp, $allParams);
            } else {
                $ldap->unbind;   # take down session
                fullTemplate( ['verificationFailed'], {errorMessage => "The hash string " . $hash . " from your link doesn't match our record."});
                exit(0);
            }
            
        }
    } else {   
        handleLDAPBindFailure($ldapurl);
        exit(0);
    }

}

sub handleResponseMessage {

  print "Content-type: text/html\n\n";
  my $errorMessage = "You provided invalid input to the script. " .
                     "Try again please.";
  fullTemplate( [], { stage => $templates->{'stage'},
                      errorMessage => $errorMessage });
  exit();
}

#
# perform a simple search against the LDAP database using 
# a small subset of attributes of each dn and return it
# as a table to the calling browser.
#
sub handleSimpleSearch {

    my $o = $query->param('o');

    my $ldapurl = $ldapConfig->{$o}{'url'};
    my $searchBase = $ldapConfig->{$o}{'base'};

    print "Content-type: text/html\n\n";

    my $allParams = { 
                      'cn' => $query->param('cn'),
                      'sn' => $query->param('sn'),
                      'gn' => $query->param('gn'),
                      'o'  => $query->param('o'),
                      'facsimiletelephonenumber' 
                      => $query->param('facsimiletelephonenumber'),
                      'mail' => $query->param('cmail'),
                      'telephonenumber' => $query->param('telephonenumber'),
                      'title' => $query->param('title'),
                      'uid' => $query->param('uid'),
                      'ou' => $query->param('ou'),
                    };

    # Search LDAP for matching entries that already exist
    my $filter = "(" . 
                 $query->param('searchField') . "=" .
                 "*" .
                 $query->param('searchValue') .
                 "*" .
                 ")";

    my @attrs = [ 'sn', 
                  'gn', 
                  'cn', 
                  'o', 
                  'facsimiletelephonenumber', 
                  'mail', 
                  'telephoneNumber', 
                  'title', 
                  'uid', 
                  'labeledURI', 
                  'ou' ];

    my $found = searchDirectory($ldapurl, $searchBase, $filter, \@attrs);

    # Send back the search results
    if ($found) {
      fullTemplate( ('searchResults'), { stage => "searchresults",
                                         allParams => $allParams,
                                         foundAccounts => $found });
    } else {
      $found = "No entries matched your criteria.  Please try again\n";

      fullTemplate( ('searchResults'), { stage => "searchresults",
                                         allParams => $allParams,
                                         foundAccounts => $found });
    }

    exit();
}

#
# search the LDAP directory to see if a similar account already exists
#
sub searchDirectory {
    my $ldapurl = shift;
    my $base = shift;
    my $filter = shift;
    my $attref = shift;

	my $mesg;
    my $foundAccounts = 0;
    
    #if ldap server is down, a html file containing warning message will be returned
    my $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    
    if ($ldap) {
        if($useStartTLS eq 'true') {
                $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
        }
    	$ldap->bind( version => 3, anonymous => 1);
    	my $mesg = $ldap->search (
        	base   => $base,
        	filter => $filter,
        	attrs => @$attref,
    	);

    	if ($mesg->count() > 0) {
        	$foundAccounts = "";
        	my $entry;
        	foreach $entry ($mesg->sorted(['sn'])) {
          		$foundAccounts .= "<tr>\n<td class=\"main\">\n";
          		$foundAccounts .= "<a href=\"" unless 
                    (!$entry->get_value('labeledURI'));
         		 $foundAccounts .= $entry->get_value('labeledURI') unless
                    (!$entry->get_value('labeledURI'));
          		$foundAccounts .= "\">\n" unless 
                    (!$entry->get_value('labeledURI'));
          		$foundAccounts .= $entry->get_value('givenName');
          		$foundAccounts .= "</a>\n" unless 
                    (!$entry->get_value('labeledURI'));
          		$foundAccounts .= "\n</td>\n<td class=\"main\">\n";
          		$foundAccounts .= "<a href=\"" unless 
                    (!$entry->get_value('labeledURI'));
          		$foundAccounts .= $entry->get_value('labeledURI') unless
                    (!$entry->get_value('labeledURI'));
          		$foundAccounts .= "\">\n" unless 
                    (!$entry->get_value('labeledURI'));
          		$foundAccounts .= $entry->get_value('sn');
          		$foundAccounts .= "</a>\n";
          		$foundAccounts .= "\n</td>\n<td class=\"main\">\n";
          		$foundAccounts .= $entry->get_value('mail');
          		$foundAccounts .= "\n</td>\n<td class=\"main\">\n";
          		$foundAccounts .= $entry->get_value('telephonenumber');
          		$foundAccounts .= "\n</td>\n<td class=\"main\">\n";
          		$foundAccounts .= $entry->get_value('title');
          		$foundAccounts .= "\n</td>\n<td class=\"main\">\n";
          		$foundAccounts .= $entry->get_value('ou');
          		$foundAccounts .= "\n</td>\n";
          		$foundAccounts .= "</tr>\n";
        	}
    	}
    	$ldap->unbind;   # take down session
    }
    return $foundAccounts;
}

sub debug {
    my $msg = shift;
    
    if ($debug) {
        print STDERR "LDAPweb: $msg\n";
    }
}

sub handleLDAPBindFailure {
    my $ldapAttemptUrl = shift;
    my $primaryLdap =  $properties->getProperty('auth.url');

    if ($ldapAttemptUrl eq  $primaryLdap) {
        handleGeneralServerFailure("The main LDAP server $ldapurl is down!");
    } else {
        debug("attempted to bind to nonresponsive LDAP server $ldapAttemptUrl, skipped.");
    }
}

sub handleGeneralServerFailure {
    my $errorMessage = shift;
    fullTemplate( ['mainServerFailure'], { errorMessage => $errorMessage });
    exit(0);   
   }
    
sub setVars {
    my $paramVars = shift;
    # initialize default parameters 
    my $templateVars = { cfg => $cfg,
                         styleSkinsPath => $contextUrl . "/style/skins",
                         styleCommonPath => $contextUrl . "/style/common",
                         contextUrl => $contextUrl,
                         cgiPrefix => $cgiPrefix,
                         orgList => \@validDisplayOrgList,
                         config  => $config,
    };
    
    # append customized params
    while (my ($k, $v) = each (%$paramVars)) {
        $templateVars->{$k} = $v;
    }
    
    return $templateVars;
} 

#Method to get the next avaliable uid number. We use the mechanism - http://www.rexconsulting.net/ldap-protocol-uidNumber.html
sub getNextUidNumber {

    my $maxAttempt = $properties->getProperty('ldap.nextuid.maxattempt');
    
    my $ldapUsername = shift;
    my $ldapPassword = shift;
    
    my $realUidNumber;
    my $uidNumber;
    my $entry;
    my $mesg;
    my $ldap;
    
    debug("ldap server: $ldapurl");
    
    #if main ldap server is down, a html file containing warning message will be returned
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    
    if ($ldap) {
    	my $existingHighUid=getExistingHighestUidNum($ldapUsername, $ldapPassword);
    	   if($useStartTLS eq 'true') {
    	        $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
    	   }
        my $bindresult = $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword);
        #read the uid value stored in uidObject class
        for(my $index=0; $index<$maxAttempt; $index++) {
            $mesg = $ldap->search(base  => $dn_store_next_uid, filter => '(objectClass=*)');
            if ($mesg->count() > 0) {
                debug("Find the cn - $dn_store_next_uid");
                $entry = $mesg->pop_entry;
                $uidNumber = $entry->get_value($attribute_name_store_next_uid);
                if($uidNumber) {
                    if (looks_like_number($uidNumber)) {
                        debug("uid number is $uidNumber");
                        #remove the uid attribute with the read value
                        my $delMesg = $ldap->modify($dn_store_next_uid, delete => { $attribute_name_store_next_uid => $uidNumber});
                        if($delMesg->is_error()) {
                            my $error=$delMesg->error();
                            my $errorName = $delMesg->error_name();
                            debug("can't remove the attribute - $error");
                            debug("can't remove the attribute and the error name - $errorName");
                            #can't remove the attribute with the specified value - that means somebody modify the value in another route, so try it again
                        } else {
                            debug("Remove the attribute successfully and write a new increased value back");
                            if($existingHighUid) {
                            	debug("exiting high uid exists =======================================");
                            	if($uidNumber <= $existingHighUid ) {
                            		debug("The stored uidNumber $uidNumber is less than or equals the used uidNumber $existingHighUid, so we will use the new number which is $existingHighUid+1");
                            		$uidNumber = $existingHighUid +1;
                            	} 
                            }                  
                            my $newValue = $uidNumber +1;
                            $delMesg = $ldap->modify($dn_store_next_uid, add => {$attribute_name_store_next_uid => $newValue});
                            $realUidNumber = $uidNumber;
                            last;
                        }
                    }
                    
               } else {
                 debug("can't find the attribute - $attribute_name_store_next_uid in the $dn_store_next_uid and we will try again");
               }
            } 
        }
        $ldap->unbind;   # take down session
    }
    return $realUidNumber;
}

#Method to get the existing high uidNumber in the account tree.
sub getExistingHighestUidNum {
    my $ldapUsername = shift;
    my $ldapPassword = shift;
   
    my $high;
    my $ldap;
    my $storedUidNumber;
    
    
    #if main ldap server is down, a html file containing warning message will be returned
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
        if($useStartTLS eq 'true') {
            $ldap->start_tls( verify => 'require',
                      cafile => $ldapServerCACertFile);
        }
        my $bindresult = $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword);
        my $mesg = $ldap->search(base  => $dn_store_next_uid, filter => '(objectClass=*)');
         if ($mesg->count() > 0) {
                debug("Find the cn - $dn_store_next_uid");
                my  $entry = $mesg->pop_entry;
                $storedUidNumber = $entry->get_value($attribute_name_store_next_uid);
        }
        my $authBase = $properties->getProperty("auth.base");
        my $uids = $ldap->search(
                        base => $authBase,
                        scope => "sub",
                        filter => "uidNumber=*", 
                        attrs   => [ 'uidNumber' ],
                        );
       return unless $uids->count;
  	    my @uids;
        if ($uids->count > 0) {
                foreach my $uid ($uids->all_entries) {
                		if($storedUidNumber) {
                			if( $uid->get_value('uidNumber') >= $storedUidNumber) {
                				push @uids, $uid->get_value('uidNumber');
                			}
                		} else {
                        	push @uids, $uid->get_value('uidNumber');
                        }
                }
        }       
        
        if(@uids) {
        	@uids = sort { $b <=> $a } @uids;
        	$high = $uids[0];   
        }    
        debug("the highest exiting uidnumber is $high");
        $ldap->unbind;   # take down session
    }
    return $high;

}


