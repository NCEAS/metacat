#!/usr/bin/perl -w
#
# This is a simple script for creating a new LDAP record with a
# predefined format that is hardcoded in the script.  This could be generalized
# to support an externally-configured record format.
# Matt Jones
#
use strict;       # turn on strict syntax checking.
use Net::LDAP;    # load the LDAP net libraries
use Digest::SHA1; # for creating the password hash
use MIME::Base64; # for creating the password hash
use URI;          # for parsing URL syntax
use AppConfig qw(:expand :argcount);
use Term::ReadKey;

# Set up our default configuration
my $ldapurl = "";
my $root = "";
my $rootpw = "";
my $searchBase = "";
my $mailhost = "";
my $sender = "";

my $debug = 0;

#--------------------------------------------------------------------------80c->
# Read the ldapweb.cfg file
my $cfgfile = "ldapweb.cfg";
my $config = AppConfig->new({ 
    GLOBAL => { ARGCOUNT => ARGCOUNT_ONE, } });

$config->define("ldapurl", { ARGCOUNT => ARGCOUNT_HASH} );           
$config->define("ldapsearchbase", { ARGCOUNT => ARGCOUNT_HASH} );
$config->define("dn", { ARGCOUNT => ARGCOUNT_HASH} );
$config->define("filter", { ARGCOUNT => ARGCOUNT_HASH} );
$config->define("user", { ARGCOUNT => ARGCOUNT_HASH} );
$config->define("password", { ARGCOUNT => ARGCOUNT_HASH} );

$config->file($cfgfile);
my $config_ldapurl = $config->get('ldapurl');
my $config_ldapsearchbase = $config->get('ldapsearchbase');
my $config_dn = $config->get('dn');
my $config_filter = $config->get('filter');
my $config_user = $config->get('user');
my $config_password = $config->get('password');

my @orglist;
foreach my $neworg (keys %$config_dn) {
    push(@orglist, $neworg);
    debug($neworg);
}


#--------------------------------------------------------------------------80c->
# Define the main program logic that calls subroutines to do the work
#--------------------------------------------------------------------------80c->

my $allParams = getAccountInfo();
my $shouldContinue = checkForDuplicateAccounts($allParams);
createAccount($allParams);
exit(0);

#--------------------------------------------------------------------------80c->
# Define the subroutines to do the work
#--------------------------------------------------------------------------80c->

#
# Prompt the user for one piece of input information
#
sub getUserInput {
    my $prompt = shift;
    my $hideInput = shift;

    print $prompt, ": ";
    if ($hideInput) {
        ReadMode('noecho');
    }

    my $value = ReadLine(0);
    chomp($value);
    if ($hideInput) {
        ReadMode('normal');
        print "\n";
    }
    return $value;
}

#
# get input about the account to be created
#
sub getAccountInfo {
    
    my $uid = getUserInput("UserID", 0);
    my $userPassword = getUserInput("Password", 1);
    my $userPassword2 = getUserInput("Password Again", 1);
    my $givenName = getUserInput("GivenName", 0);
    my $sn = getUserInput("Surname", 0);
    my $o = getUserInput("Organization", 0);
    my $mail = getUserInput("Email", 0);
    my $title = getUserInput("Title", 0);
    my $telephoneNumber = getUserInput("Telephone", 0);
    print "\n";

    my $allParams = { 'givenName' => $givenName, 
                      'sn' => $sn,
                      'o' => $o, 
                      'mail' => $mail, 
                      'uid' => $uid, 
                      'userPassword' => $userPassword, 
                      'userPassword2' => $userPassword2, 
                      'title' => $title, 
                      'telephoneNumber' => $telephoneNumber };
    # Check that all required fields are provided and not null
    my @requiredParams = ( 'givenName', 'sn', 'o', 'mail', 
                           'uid', 'userPassword', 'userPassword2');
    if (! paramsAreValid($allParams, @requiredParams)) {
        my $errorMessage = "Required information is missing. " .
            "Please try again and provide all required fields.";
        print $errorMessage, "\n";
        exit(0);
    } else {
	    $ldapurl = $config_ldapurl->{$o};
	    $searchBase = $config_ldapsearchbase->{$o};  
    }

    return $allParams;
}

sub checkForDuplicateAccounts { 
    my $allParams = shift;

    # Search LDAP for matching entries that already exist
    # Some forms use a single text search box, whereas others search per
    # attribute.
    my $filter;
    $filter = "(|" . 
              "(uid=" . $allParams->{'uid'} . ") " .
              "(mail=" . $allParams->{'mail'} . ")" .
              "(&(sn=" . $allParams->{'sn'} . ") " . 
              "(givenName=" . $allParams->{'givenName'} . "))" . 
              ")";

    my @attrs = [ 'uid', 'o', 'cn', 'mail', 'telephoneNumber', 'title' ];

    my $found = findExistingAccounts($ldapurl, $searchBase, $filter, \@attrs);

    # If entries match, ask if the account should be created
    if ($found) {
        print $found, "\n";
        my $question  = "Similar accounts already exist.  Do you want to " .
            "create a\nnew account anyways? (y/n)";
        my $continue = getUserInput($question, 0);
        if ($continue =~ "y") {
            return 1;
        } else {
            return 0;
        }
    # Otherwise, create a new user in the LDAP directory
    } else {
        return 1;
    }
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
# search the LDAP directory to see if a similar account already exists
#
sub findExistingAccounts {
    my $ldapurl = shift;
    my $base = shift;
    my $filter = shift;
    my $attref = shift;

    my $foundAccounts = 0;

    my $ldap = Net::LDAP->new($ldapurl) or die "$@";
    $ldap->bind( version => 3, anonymous => 1);
    my $mesg = $ldap->search (
        base   => $base,
        filter => $filter,
        attrs => @$attref,
    );

    if ($mesg->count() > 0) {
        $foundAccounts = "";
        my $entry;
        foreach $entry ($mesg->all_entries) { 
            $foundAccounts .= "\nAccount: ";
            $foundAccounts .= $entry->dn();
            $foundAccounts .= "\n";
            foreach my $attribute ($entry->attributes()) {
                $foundAccounts .= "    $attribute: ";
                $foundAccounts .= $entry->get_value($attribute);
                $foundAccounts .= "\n";
            }
            $foundAccounts .= "\n";
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
    my $allParams = shift;
    my @pnames = @_;

    my $allValid = 1;

    foreach my $parameter (@pnames) {
        if (!defined($allParams->{$parameter}) || 
            ! $allParams->{$parameter} ||
            $allParams->{$parameter} =~ /^\s+$/) {
            $allValid = 0;
        }
    }

    return $allValid;
}

#
# Bind to LDAP and create a new account using the information provided
# by the user
#
sub createAccount {
    my $allParams = shift;

    my $o = $allParams->{'o'};

    if ($o =~ "LTER") {
        # Handle LTER case and redirect them there
    } else {

        # Be sure the passwords match
        if ($allParams->{'userPassword'} !~ $allParams->{'userPassword2'}) {
            my $errorMessage = "The passwords do not match. Try again.";
            print $errorMessage, "\n";
            exit(0);
        }

	    my $ldapurl = $config_ldapurl->{$o};
	    my $root = $config_user->{$o};
	    my $rootpw = $config_password->{$o};
	    my $searchBase = $config_ldapsearchbase->{$o};
	    my $dnBase = $config_dn->{$o};

        my $ldap = Net::LDAP->new($ldapurl) or die "$@";
        $ldap->bind( version => 3, dn => $root, password => $rootpw );
        print "Inserting new entry for $allParams->{'uid'} ...\n";
        my $dn = 'uid=' . $allParams->{'uid'} . ',' . $dnBase;

        # Create a hashed version of the password
        my $shapass = createSeededPassHash($allParams->{'userPassword'});

        # Do the insertion
        my $additions = [ 
                'uid'   => $allParams->{'uid'},
                'o'   => $allParams->{'o'},
                'cn'   => join(" ", $allParams->{'givenName'}, 
                                    $allParams->{'sn'}),
                'sn'   => $allParams->{'sn'},
                'givenName'   => $allParams->{'givenName'},
                'mail' => $allParams->{'mail'},
                'userPassword' => $shapass,
                'objectclass' => ['top', 'person', 'organizationalPerson', 
                                'inetOrgPerson', 'uidObject' ]
            ];
        if (defined($allParams->{'telephoneNumber'}) && 
            $allParams->{'telephoneNumber'} &&
            ! $allParams->{'telephoneNumber'} =~ /^\s+$/) {
            $$additions[$#$additions + 1] = 'telephoneNumber';
            $$additions[$#$additions + 1] = $allParams->{'telephoneNumber'};
        }
        if (defined($allParams->{'title'}) && 
            $allParams->{'title'} &&
            ! $allParams->{'title'} =~ /^\s+$/) {
            $$additions[$#$additions + 1] = 'title';
            $$additions[$#$additions + 1] = $allParams->{'title'};
        }
        my $result = $ldap->add ( 'dn' => $dn, 'attr' => [ @$additions ]);
    
        if ($result->code()) {
            # Post an error message
            print "Error while creating account:\n";
            print $result->code(), "\n";
        } else {
            print "Account created.\n";
        }

        $ldap->unbind;   # take down session
    }
}

sub debug {
    my $msg = shift;
    
    if ($debug) {
        print STDERR "$msg\n";
    }
}
