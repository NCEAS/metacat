#!/usr/bin/perl -w
#
# This is a web-based application for allowing users to register a new
# account for Metacat access.  We currently only support LDAP even
# though metacat could potentially support other types of directories.
#
use strict;       # turn on strict syntax checking.
use Net::LDAP;    # load the LDAP net libraries
use Digest::SHA1; # for creating the password hash
use MIME::Base64; # for creating the password hash
use Term::ReadKey;# for not displaying the password on command line

# Set up our default configuration
my $ldapurl = "@ldapurl@";
my $bindDN = "@user@";
my $searchBase = "@ldapSearchBase@";

my $userDN = readReqdParam("Enter the user DN:");
ReadMode('noecho'); # set no echo mode on the term so that passwords are not displayed
my $userPass = readReqdParam("Enter the new user password:");
my $bindPass = readReqdParam("Enter the root password:");
ReadMode('normal'); # set terminal mode back to normal 

my $errorMessage = 0;
my $ldap = Net::LDAP->new($ldapurl) or die "$@";
my $bindresult = $ldap->bind( version => 3, dn => $bindDN,
                                  password => $bindPass );
if ($bindresult->code) {
    $errorMessage = "Failed to log in. Are you sure your old " .
                    "password is correct? Try again...\n";
    print $errorMessage;
    exit 0;
}

# Find the user here and change their entry
my $newpass = createSeededPassHash($userPass);
my $modifications = { userPassword => $newpass };
my $result = $ldap->modify( $userDN, replace => { %$modifications });

if ($result->code()) {
   $errorMessage = "There was an error changing the password: " .
                      $result->error."\n";
} else {
   $errorMessage = "The password has been changed.\n";

}

$ldap->unbind;   # take down session

print $errorMessage;

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

