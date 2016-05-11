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

package Metacat;

require 5.005_62;
use strict;
use warnings;

require Exporter;
use AutoLoader qw(AUTOLOAD);

use LWP::UserAgent;
use HTTP::Request::Common qw(POST);
use HTTP::Cookies;

our @ISA = qw(Exporter);

# Items to export into callers namespace by default. Note: do not export
# names by default without a very good reason. Use EXPORT_OK instead.
# Do not simply export all your public functions/methods/constants.

# This allows declaration	use Metacat ':all';
# If you do not need this, moving things directly into @EXPORT or @EXPORT_OK
# will save memory.
our %EXPORT_TAGS = ( 'all' => [ qw(
	
) ] );

our @EXPORT_OK = ( @{ $EXPORT_TAGS{'all'} } );

our @EXPORT = qw(
	
);
our $VERSION = '0.01';


# Preloaded methods go here.

#############################################################
# Constructor creates a new class instance and inits all
# of the instance variables to their proper default values,
# which can later be changed using "set_options"
#############################################################
sub new {
  my($type,$metacatUrl) = @_;
  my $cookie_jar = HTTP::Cookies->new;

  my $self = {
    metacatUrl     => $metacatUrl,
    message        => '',
    cookies        => \$cookie_jar
  };

  bless $self, $type; 
  return $self;
}

#############################################################
# subroutine to set options for the class, including the URL 
# for the Metacat database to which we would connect
#############################################################
sub set_options {
  my $self = shift;
  my %newargs = ( @_ );

  my $arg;
  foreach $arg (keys %newargs) {
    $self->{$arg} = $newargs{$arg};
  }
}

#############################################################
# subroutine to send data to metacat and get the response
# return response from metacat
#############################################################
sub sendData {
  my $self = shift;
  my %postData = ( @_ );

  $self->{'message'} = '';
  my $userAgent = new LWP::UserAgent;
  $userAgent->agent("MetacatClient/1.0");

  # determine encoding type
  my $contentType = 'application/x-www-form-urlencoded';
  my $expect = "100-continue";
	if ($postData{'enctype'}) {
      $contentType = $postData{'enctype'};
      delete $postData{'enctype'};
  }
  
  
  my $request;
  if ( $self->{'auth_token_header'} ) {
      # if available, set the Authorization header from the auth_token_header instance variable
      $request = POST("$self->{'metacatUrl'}",
                      Content_Type => $contentType,
                      Expect => $expect,
                      Authorization => $self->{'auth_token_header'},
                      Content => \%postData
                      );
      
  } else {
      $request = POST("$self->{'metacatUrl'}",
                      Content_Type => $contentType,
                      Expect => $expect,
                      Content => \%postData
                      );      
  }

  # set cookies on UA object
  my $cookie_jar = $self->{'cookies'};
  $$cookie_jar->add_cookie_header($request);
  #print "Content_type:text/html\n\n";
  #print "request: " . $request->as_string();

  my $response = $userAgent->request($request);
  #print "response: " . $response->as_string();
   
  if ($response->is_success) {
    # save the cookies
    $$cookie_jar->extract_cookies($response);
    # save the metacat response message
    $self->{'message'} = $response->content;
  } else {
    #print "SendData content is: ", $response->content, "\n";
    return 0;
  } 
  return $response;
}

#############################################################
# subroutine to log into Metacat and save the cookie if the
# login is valid.  If not valid, return 0. If valid then send 
# following values to indicate user status
# 1 - user
# 2 - moderator
# 3 - administrator
# 4 - moderator and administrator
#############################################################
sub login {
  my $self = shift;
  my $username = shift;
  my $password = shift;

  my $returnval = 0;

  my %postData = ( action => 'login',
                   qformat => 'xml',
                   username => $username,
                   password => $password
                 );
  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~ /<login>/) {
    $returnval = 1;
  }

  if (($response) && $response->content =~ /<isAdministrator>/) {
	if (($response) && $response->content =~ /<isModerator>/) {
    		$returnval = 4;
	} else {
		$returnval = 3;
	}
  } elsif (($response) && $response->content =~ /<isModerator>/){
	$returnval = 2;
  }

  return $returnval;
}

#############################################################
# subroutine to logout of Metacat
#############################################################
sub logout {
    my $self = shift;
    
    my %postData = (action => 'logout');
    
    my $response = $self->sendData(%postData);
    
    my $returnval = 1;
    if (($response) && $response->content =~ /<logout>/) {
    	$returnval = 0;
  	}
  	
    # clear the cookie
    my $cookie_jar = $self->{'cookies'};
    $$cookie_jar->clear();
    
    return $returnval;
}

#############################################################
# subroutine to log into Metacat and get user and group 
# information xml for a logged in user
#############################################################
sub getUserInfo {
	my $self = shift;

	my %postData = (action => 'validatesession');
  
	my $response = $self->sendData(%postData);

	return $response->content;
}

#############################################################
# subroutine to insert an XML document into Metacat
# If success, return 1, else return 0
#############################################################
sub insert {
  my $self = shift;
  my $docid = shift;
  my $xmldocument = shift;
  my $dtd = shift;

  my $returnval = 0;

  my %postData = ( action => 'insert',
                   docid => $docid,
                   doctext => $xmldocument
                 );
  if ($dtd) {
    $postData{'dtdtext'} = $dtd;
  }

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~ /<success>/) {
    $returnval = 1;
  } elsif (($response)) {
    $returnval = 0;
    #print "Error response from sendData!\n";
    #print $response->content, "\n";
  } else {
    $returnval = 0;
    #print "Invalid response from sendData!\n";
  }

  return $returnval;
}

#############################################################
# subroutine to update an XML document in Metacat
# If success, return 1, else return 0
#############################################################
sub update {
  my $self = shift;
  my $docid = shift;
  my $xmldocument = shift;
  my $dtd = shift;

  my $returnval = 0;

  my %postData = ( action => 'update',
                   docid => $docid,
                   doctext => $xmldocument
                 );
  if ($dtd) {
    $postData{'dtdtext'} = $dtd;
  }

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~ /<success>/) {
    $returnval = 1;
  }

  return $returnval;
}

############################################################
# subroutine to upload an XML document in Metacat
# If success, return 1, else return 0
#############################################################
sub upload {
  my $self = shift;
  my $docid = shift;
  my $datafile = shift;
  my $filename = shift;

  my $returnval = 0;

  my %postData = ( action => 'upload',
                   docid => $docid,
                   datafile => [$datafile, $filename],
                   enctype => 'multipart/form-data'
                 );

  my $response = $self->sendData(%postData);
  #print "response is: $response";
  # 
  if (($response) && $response->content =~ /<success>/) {
    $returnval = $response->content;
  }

  return $returnval;
}


#############################################################
# subroutine to delete an XML document in Metacat
# If success, return 1, else return 0
#############################################################
sub delete {
  my $self = shift;
  my $docid = shift;

  my $returnval = 0;

  my %postData = ( action => 'delete',
                   docid => $docid
                 );

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~ /<success>/) {
    $returnval = 1;
  }

  return $returnval;
}

#############################################################
# subroutine to set access for an XML document in Metacat
# If success, return 1, else return 0
#############################################################
sub setaccess {
  my $self = shift;
  my $docid = shift;
  my $principal = shift;
  my $permission = shift;
  my $permType = shift;
  my $permOrder = shift;

  my $returnval = 0;

  my %postData = ( action => 'setaccess',
                   docid => $docid,
		   principal => $principal,
		   permission => $permission,
		   permType => $permType,
		   permOrder => $permOrder
                 );

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~ /<success>/) {
    $returnval = 1;
  }

  return $returnval;
}

#############################################################
# subroutine to get access info from Metacat
# returns access XML block from Metacat
#############################################################
sub getaccess {
    my $self = shift;
    my $docid = shift;
    
    my %postData = ( action => 'getaccesscontrol',
    docid => $docid
    );
    
    my $response = $self->sendData(%postData);
    
    my $returnval = 0;
    if ($response) {
        $returnval = $response;
    }
    
    return $returnval;
}

#############################################################
# subroutine to read an XML document from Metacat
# returns the XML from Metacat, which may be an error response
#############################################################
sub read {
  my $self = shift;
  my $docid = shift;

  my %postData = ( action => 'read',
                   qformat => 'xml',
                   docid => $docid
                 );

  my $response = $self->sendData(%postData);
  
  my $returnval = 0;
  if ($response) {
    $returnval = $response;
  } 
    
  return $returnval;
}

#############################################################
# subroutine to query metacat using a structured path query
# returns the XML from Metacat, which may be an error response
#############################################################
sub squery {
  my $self = shift;
  my $query = shift;

  my %postData = ( action => 'squery',
                   qformat => 'xml',
                   query => $query
                 );

  my $response = $self->sendData(%postData);

  my $returnval = 0;
  if ($response) {
    $returnval = $response;
  } 
    
  return $returnval;
}

#############################################################
# subroutine to get the maximimum id in a series
# If success, return max id, else return 0
#############################################################
sub getLastId {
  my $self = shift;
  my $scope = shift;

  my $returnval = 0;

  my %postData = ( action => 'getlastdocid',
                   scope => $scope
                 );

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~  /<docid>(.*)<\/docid>/s) {
      $returnval = "$1";
  } elsif (($response)) {
    $returnval = 0;
    #print "Error response from sendData!\n";
    #print $response->content, "\n";
  } else {
    $returnval = 0;
    #print "Invalid response from sendData!\n";
  }

  return $returnval;
}

#############################################################
# subroutine to get the maximimum id in a series
# If success, return max id, else return 0
#############################################################
sub getLastRevision {
  my $self = shift;
  my $docid = shift;

  my $returnval = 0;

  my %postData = ( action => 'getrevisionanddoctype',
                   docid => $docid
                 );

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~ /(.*);(.*)/s)  {
      $returnval = "$1";
  } elsif (($response)) {
    $returnval = 0;
    #print "Error response from sendData!\n";
    #print $response->content, "\n";
  } else {
    $returnval = 0;
    #print "Invalid response from sendData!\n";
  }

  return $returnval;
}

#############################################################
# subroutine to get the docid for a given PID
# If success, return docid, else return -1
#############################################################
sub getDocid {
  my $self = shift;
  my $pid = shift;

  my $returnval = 0;

  my %postData = ( action => 'getdocid',
                   pid => $pid
                 );

  my $response = $self->sendData(%postData);
  if (($response) && $response->content =~  /<docid>(.*)<\/docid>/s) {
      $returnval = "$1";
  } elsif (($response)) {
    $returnval = -1;
    #print "Error response from sendData!\n";
    #print $response->content, "\n";
  } else {
    $returnval = -1;
    #print "Invalid response from sendData!\n";
  }

  return $returnval;
}

#############################################################
# subroutine to get the message returned from the last executed
# metacat action.  These are generally XML formatted messages.
#############################################################
sub getMessage {
  my $self = shift;

  return $self->{'message'};
}

#############################################################
# subroutine to get the cookies returned from the metacat 
# server to establish (and pass on) session info (JSESSIONID).
#############################################################
sub getCookies {
  my $self = shift;

  return $self->{'cookies'};
}

# Autoload methods go after =cut, and are processed by the autosplit program.

1;
__END__
# Below is stub documentation for your module. You better edit it!

=head1 NAME

Metacat - Perl extension for communicating with the Metacat XML database

=head1 SYNOPSIS

  use Metacat;
  my $metacat = Metacat->new();
  my $response = $metacat->login($username, $password); 
  print $metacat->getMessage();
  $response = $metacat->insert($docid, $xmldoc); 
  print $metacat->getMessage();
  $response = $metacat->insert($docid, $xmldoc, $dtd); 
  print $metacat->getMessage();
  $response = $metacat->update($docid, $xmldoc); 
  print $metacat->getMessage();
  $response = $metacat->upload($docid, $data); 
  print $metacat->getMessage();
  $htmlResponse = $metacat->read($docid); 
  $xmldoc = $htmlResponse->content();
  print $xmldoc;
  $resultset = $metacat->squery($pathquery); 
  print $resultset;
  $response = $metacat->delete($docid); 
  $response = $metacat->setaccess($docid,$principal,$permission,$permType,$permOrder); 
  my $lastid = $metacat->getLastId("obfs");
  print $metacat->getMessage();
  $response = $metacat->getCookies(); 
  print $metacat->getMessage();

=head1 DESCRIPTION

This is a client library for accessing the Metacat XML database.  Metacat
is a Java servlet that accepts commands over HTTP and returns XML and
HTML responses.  See http://knb.ecoinformatics.org for details about
Metacat and its interface.

=head2 EXPORT

None by default.


=head1 AUTHOR

Matthew B. Jones, jones@nceas.ucsb.edu

=head1 SEE ALSO

perl(1).

=cut
