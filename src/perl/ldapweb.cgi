#!/usr/bin/perl -w
#
#  '$RCSfile$'
#  Copyright: 2001 Regents of the University of California 
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
use Captcha::reCAPTCHA; # for protection against spams
use Cwd 'abs_path';

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
if ( $properties->getProperty('server.httpPort') eq '443' ) {
	$protocol = 'https://';
}
my $contextUrl = $protocol . $properties->getProperty('server.name');
if ($properties->getProperty('server.httpPort') ne '80') {
        $contextUrl = $contextUrl . ':' . $properties->getProperty('server.httpPort');
}
$contextUrl = $contextUrl . '/' .  $properties->getProperty('application.context');

my $metacatUrl = $contextUrl . "/metacat";
my $cgiPrefix = "/" . $properties->getProperty('application.context') . "/cgi-bin";
my $styleSkinsPath = $contextUrl . "/style/skins";
my $styleCommonPath = $contextUrl . "/style/common";

#recaptcha key information
my $recaptchaPublicKey=$properties->getProperty('ldap.recaptcha.publickey');
my $recaptchaPrivateKey=$properties->getProperty('ldap.recaptcha.privatekey');

my @errorMessages;
my $error = 0;

my $emailVerification= 'emailverification';

# Import all of the HTML form fields as variables
import_names('FORM');

# Must have a config to use Metacat
my $skinName = "";
if ($FORM::cfg) {
    $skinName = $FORM::cfg;
} elsif ($ARGV[0]) {
    $skinName = $ARGV[0];
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
        ' MetacatUrl is set correctly in ' .  $skinName . '.properties';
    exit();
}

my $skinProperties = new Config::Properties();
if (!($skinName)) {
    $error = "Application misconfigured.  Please contact the administrator.";
    push(@errorMessages, $error);
} else {
    my $skinProps = "$skinsDir/$skinName/$skinName.properties";
    unless (open (SKIN_PROPERTIES, $skinProps)) {
        print "Content-type: text/html\n\n";
        print "Unable to locate skin properties at $skinProps.  Is this path correct?";
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


my $searchBase;
my $ldapUsername;
my $ldapPassword;
# TODO: when should we use surl instead? Is there a setting promoting one over the other?
# TODO: the default tree for accounts should be exposed somewhere, defaulting to unaffiliated
my $ldapurl = $properties->getProperty('auth.url');

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

my $orgProps = $properties->splitToTree(qr/\./, 'organization');
my $orgNames = $properties->splitToTree(qr/\./, 'organization.name');
# pull out properties available e.g. 'name', 'base'
my @orgData = keys(%$orgProps);

my @orgList;
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
            $ldapConfig->{$o}{'filter'} = $filter;
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

sub fullTemplate {
    my $templateList = shift;
    my $templateVars = setVars(shift);
    my $c = Captcha::reCAPTCHA->new;
    my $captcha = 'captcha';
    #my $error=null;
    my $use_ssl= 1;
    #my $options=null;
    $templateVars->{$captcha} = $c->get_html($recaptchaPublicKey,undef, $use_ssl, undef);
    $template->process( $templates->{'header'}, $templateVars );
    foreach my $tmpl (@{$templateList}) {
        $template->process( $templates->{$tmpl}, $templateVars );
    }
    $template->process( $templates->{'footer'}, $templateVars );
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
    
    print "Content-type: text/html\n\n";
    
    
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
    my $challenge = $query->param('recaptcha_challenge_field');
    my $response = $query->param('recaptcha_response_field');
    # Verify submission
    my $result = $c->check_answer(
        $recaptchaPrivateKey, $ENV{'REMOTE_ADDR'},
        $challenge, $response
    );

    if ( $result->{is_valid} ) {
        #print "Yes!";
        #exit();
    }
    else {
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
        my $errorMessage = "Required information is missing. " .
            "Please fill in all required fields and resubmit the form.";
        fullTemplate(['register'], { stage => "register",
                                     allParams => $allParams,
                                     errorMessage => $errorMessage });
        exit();
    } else {
        my $o = $query->param('o');    
        $searchBase = $ldapConfig->{$o}{'base'};  
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

    my @attrs = [ 'uid', 'o', 'cn', 'mail', 'telephoneNumber', 'title' ];
    my $found = findExistingAccounts($ldapurl, $searchBase, $filter, \@attrs);

    # If entries match, send back a request to confirm new-user creation
    if ($found) {
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
                      'o' => 'unaffiliated', # only accept unaffiliated registration
                      'mail' => $query->param('mail'), 
                      'uid' => $query->param('uid'), 
                      'userPassword' => $query->param('userPassword'), 
                      'userPassword2' => $query->param('userPassword2'), 
                      'title' => $query->param('title'), 
                      'telephoneNumber' => $query->param('telephoneNumber') };
    print "Content-type: text/html\n\n";
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
    if ($query->param('userPassword') =~ $query->param('userPassword2')) {

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
        #$ldap->start_tls( verify => 'require',
                      #cafile => '/usr/share/ssl/ldapcerts/cacert.pem');
        $ldap->start_tls( verify => 'none');
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
    	$ldap->start_tls( verify => 'none');
    	my $bindresult = $ldap->bind;
    	if ($bindresult->code) {
        	return $entry;
    	}

    	if($ldapConfig->{$org}{'filter'}){
            debug("getLdapEntry: filter set, searching for base=$base, " .
                  "(&(uid=$username)($ldapConfig->{$org}{'filter'})");
        	$mesg = $ldap->search ( base   => $base,
                filter => "(&(uid=$username)($ldapConfig->{$org}{'filter'}))");
    	} else {
            debug("getLdapEntry: no filter, searching for $base, (uid=$username)");
        	$mesg = $ldap->search ( base   => $base, filter => "(uid=$username)");
    	}
    
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
        my $sender =  $properties->getProperty('email.sender');
        # Send the email message to them
        my $smtp = Net::SMTP->new($mailhost);
        $smtp->mail($sender);
        $smtp->to($recipient);

        my $message = <<"        ENDOFMESSAGE";
        To: $recipient
        From: $sender
        Subject: KNB Password Reset
        
        Somebody (hopefully you) requested that your KNB password be reset.  
        This is generally done when somebody forgets their password.  Your 
        password can be changed by visiting the following URL:

        $contextUrl/cgi-bin/ldapweb.cgi?stage=changepass&cfg=$cfg

            Username: $username
        Organization: $org
        New Password: $newPass

        Thanks,
            The KNB Development Team
    
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
# search the LDAP directory to see if a similar account already exists
#
sub findExistingAccounts {
    my $ldapurl = shift;
    my $base = shift;
    my $filter = shift;
    my $attref = shift;
    my $ldap;
    my $mesg;

    my $foundAccounts = 0;

    #if main ldap server is down, a html file containing warning message will be returned
    debug("findExistingAccounts: connecting to $ldapurl, $timeout");
    $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
    	$ldap->start_tls( verify => 'none');
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
                if ($entry->dn !~ /ou=Account/) {
                    $foundAccounts .= "<p>\n<b><u>Account:</u> ";
                    $foundAccounts .= $entry->dn();
                    $foundAccounts .= "</b><br />\n";
                    foreach my $attribute ($entry->attributes()) {
                        my $value = $entry->get_value($attribute);
                        $foundAccounts .= "$attribute: ";
                        $foundAccounts .= $value;
                        $foundAccounts .= "<br />\n";
                    }
                    $foundAccounts .= "</p>\n";
                }
			}
        }
    	$ldap->unbind;   # take down session

    	# Follow references
    	my @references = $mesg->references();
    	for (my $i = 0; $i <= $#references; $i++) {
        	my $uri = URI->new($references[$i]);
        	my $host = $uri->host();
        	my $path = $uri->path();
        	$path =~ s/^\///;
        	my $refFound = &findExistingAccounts($host, $path, $filter, $attref);
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
    #my $org = 'unaffiliated';
    my $ou = $query->param('ou');
    #my $ou = 'LTER';
    
    ################## Search LDAP for matching o or ou that already exist
    my $tmpSearchBase = 'dc=tmp,' . $authBase; 
    my $filter;   
    if($org) {
        $filter = "(o" 
                  . "=" . $org .
                 ")";
    } else {
        $filter = "(ou" 
                  . "=" . $ou .
                 ")";
    }
    debug("search filer " . $filter);
    debug("ldap server ". $ldapurl);
    debug("sesarch base " . $tmpSearchBase);
    print "Content-type: text/html\n\n";
    my @attrs = ['o', 'ou' ];
    my $found = searchDirectory($ldapurl, $tmpSearchBase, $filter, \@attrs);
    
    my $ldapUsername = $ldapConfig->{$org}{'user'};
    my $ldapPassword = $ldapConfig->{$org}{'password'};
    debug("LDAP connection to $ldapurl...");    
    
        
    if(!$found) {
        debug("generate the subtree in the dc=tmp===========================");
        #need to generate the subtree o or ou
        my $dn;
        #if main ldap server is down, a html file containing warning message will be returned
        my $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
        if ($ldap) {
            $ldap->start_tls( verify => 'none');
            debug("Attempting to bind to LDAP server with dn = $ldapUsername, pwd = $ldapPassword");
            $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword );
            my $additions;
             if($org) {
                $additions = [ 
                'o'   => $org,
                'objectclass' => ['top', 'organization']
                ];
                $dn='o=' . $org . ',' . $tmpSearchBase;
             } else {
                $additions = [ 
                'ou'   => $ou,
                'objectclass' => ['top', 'organizationalUnit']
                ];
                $dn='ou=' . $ou . ',' . $tmpSearchBase;
             }
            # Do the insertion
            my $result = $ldap->add ( 'dn' => $dn, 'attr' => [ @$additions ]);
            if ($result->code()) {
                fullTemplate( ['registerFailed', 'register'], { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $result->error });
                $ldap->unbind;   # take down session
                exist(0)
                # TODO SCW was included as separate errors, test this
                #$templateVars    = setVars({ stage => "register",
                #                     allParams => $allParams });
                #$template->process( $templates->{'register'}, $templateVars);
            } 
            $ldap->unbind;   # take down session
        } else {
            fullTemplate( ['registerFailed', 'register'], { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => "The ldap server is not available now. Please try it later"});
            exit(0);
        }

    } 
    
    ################create an account under tmp subtree 
    
    #generate a randomstr for matching the email.
    my $randomStr = getRandomPassword(16);
    # Create a hashed version of the password
    my $shapass = createSeededPassHash($query->param('userPassword'));
    my $additions = [ 
                'uid'   => $query->param('uid'),
                'cn'   => join(" ", $query->param('givenName'), 
                                    $query->param('sn')),
                'sn'   => $query->param('sn'),
                'givenName'   => $query->param('givenName'),
                'mail' => $query->param('mail'),
                'userPassword' => $shapass,
                'employeeNumber' => $randomStr,
                'objectclass' => ['top', 'person', 'organizationalPerson', 
                                'inetOrgPerson', 'uidObject' ]
                ];
    if (defined($query->param('telephoneNumber')) && 
                $query->param('telephoneNumber') &&
                ! $query->param('telephoneNumber') =~ /^\s+$/) {
                $$additions[$#$additions + 1] = 'telephoneNumber';
                $$additions[$#$additions + 1] = $query->param('telephoneNumber');
    }
    if (defined($query->param('title')) && 
                $query->param('title') &&
                ! $query->param('title') =~ /^\s+$/) {
                $$additions[$#$additions + 1] = 'title';
                $$additions[$#$additions + 1] = $query->param('title');
    }
    my $dn;
    if($org) {
        $$additions[$#$additions + 1] = 'o';
        $$additions[$#$additions + 1] = $org;
        $dn='uid=' . $query->param('uid') . ',' . 'o=' . $org . ',' . $tmpSearchBase;
    } else {
        $$additions[$#$additions + 1] = 'ou';
        $$additions[$#$additions + 1] = $ou;
        $dn='uid=' . $query->param('uid') . ',' . 'ou=' . $ou . ',' . $tmpSearchBase;
    }
    my $tmp = 1;
    createAccount2($dn, $ldapUsername, $ldapPassword, $additions, $tmp, $allParams);
    
    
    ####################send the verification email to the user
    my $link = $contextUrl. '/cgi-bin/ldapweb.cgi?cfg=' . $skinName . '&' . 'stage=' . $emailVerification . '&' . 'dn=' . $dn . '&' . 'hash=' . $randomStr;
    
    my $mailhost = $properties->getProperty('email.mailhost');
    my $sender =  $properties->getProperty('email.sender');
    my $recipient = $query->param('mail');
    # Send the email message to them
    my $smtp = Net::SMTP->new($mailhost);
    $smtp->mail($sender);
    $smtp->to($recipient);

    my $message = <<"     ENDOFMESSAGE";
    To: $recipient
    From: $sender
    Subject: KNB Password Reset
        
    Somebody (hopefully you) registered a KNB account.  
    Please click the following link to activate your account.
    If the link doesn't work, please copy the link to your browser:
    
    $link

    Thanks,
        The KNB Development Team
    
     ENDOFMESSAGE
     $message =~ s/^[ \t\r\f]+//gm;
    
     $smtp->data($message);
     $smtp->quit;
    debug("the link is " . $link);
    fullTemplate( ['success'] );
    
}

#
# Bind to LDAP and create a new account using the information provided
# by the user
#
sub createAccount2 {
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
    #if main ldap server is down, a html file containing warning message will be returned
    my $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
    if ($ldap) {
            $ldap->start_tls( verify => 'none');
            debug("Attempting to bind to LDAP server with dn = $ldapUsername, pwd = $ldapPassword");
            $ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword );
            debug(" 1 here is the additions " . $additions); 
            debug(" 2 here is the additions " . @$additions);
            debug(" 3 here is the additions " . [@$additions]);  
            my $result = $ldap->add ( 'dn' => $dn, 'attr' => [@$additions ]);
            if ($result->code()) {
                fullTemplate(@failureTemplate, { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $result->error });
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
# Bind to LDAP and create a new account using the information provided
# by the user
#
sub createAccount {
    my $allParams = shift;

    if ($query->param('o') =~ "LTER") {
        fullTemplate( ['registerLter'] );
    } else {

        # Be sure the passwords match
        if ($query->param('userPassword') !~ $query->param('userPassword2')) {
            my $errorMessage = "The passwords do not match. Try again.";
            fullTemplate( ['registerFailed', 'register'], { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $errorMessage });
            exit();
        }

        my $o = $query->param('o');

        my $searchBase = $ldapConfig->{$o}{'base'};
        my $dnBase = $ldapConfig->{$o}{'dn'};
        debug("the dn is " . $dnBase);
        my $ldapUsername = $ldapConfig->{$o}{'user'};
        my $ldapPassword = $ldapConfig->{$o}{'password'};
        debug("LDAP connection to $ldapurl...");    
        #if main ldap server is down, a html file containing warning message will be returned
        my $ldap = Net::LDAP->new($ldapurl, timeout => $timeout) or handleLDAPBindFailure($ldapurl);
        
        if ($ldap) {
        	$ldap->start_tls( verify => 'none');
        	debug("Attempting to bind to LDAP server with dn = $ldapUsername, pwd = $ldapPassword");
        	$ldap->bind( version => 3, dn => $ldapUsername, password => $ldapPassword );
        
        	my $dn = 'uid=' . $query->param('uid') . ',' . $dnBase;
        	debug("Inserting new entry for: $dn");

        	# Create a hashed version of the password
        	my $shapass = createSeededPassHash($query->param('userPassword'));

        	# Do the insertion
        	my $additions = [ 
                'uid'   => $query->param('uid'),
                'o'   => $query->param('o'),
                'cn'   => join(" ", $query->param('givenName'), 
                                    $query->param('sn')),
                'sn'   => $query->param('sn'),
                'givenName'   => $query->param('givenName'),
                'mail' => $query->param('mail'),
                'userPassword' => $shapass,
                'objectclass' => ['top', 'person', 'organizationalPerson', 
                                'inetOrgPerson', 'uidObject' ]
            	];
        	if (defined($query->param('telephoneNumber')) && 
            	$query->param('telephoneNumber') &&
            	! $query->param('telephoneNumber') =~ /^\s+$/) {
            	$$additions[$#$additions + 1] = 'telephoneNumber';
            	$$additions[$#$additions + 1] = $query->param('telephoneNumber');
        	}
        	if (defined($query->param('title')) && 
            	$query->param('title') &&
            	! $query->param('title') =~ /^\s+$/) {
            	$$additions[$#$additions + 1] = 'title';
            	$$additions[$#$additions + 1] = $query->param('title');
        	}
        	my $result = $ldap->add ( 'dn' => $dn, 'attr' => [ @$additions ]);
    
        	if ($result->code()) {
            	fullTemplate( ['registerFailed', 'register'], { stage => "register",
                                                            allParams => $allParams,
                                                            errorMessage => $result->error });
            	# TODO SCW was included as separate errors, test this
           	 	#$templateVars    = setVars({ stage => "register",
           	 	#                     allParams => $allParams });
            	#$template->process( $templates->{'register'}, $templateVars);
        	} else {
            	fullTemplate( ['success'] );
        	}

        	$ldap->unbind;   # take down session
        }
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
    	$ldap->start_tls( verify => 'none');
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
                         orgList => \@orgList,
                         config  => $config,
    };
    
    # append customized params
    while (my ($k, $v) = each (%$paramVars)) {
        $templateVars->{$k} = $v;
    }
    
    return $templateVars;
} 

