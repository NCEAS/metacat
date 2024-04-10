#!/usr/bin/perl -w

# control-services.pl -- Monitor a listed set of services to be sure
#             they are running.  If not running, modify the DNS system
#             to remove them from the lookup for that service
#

use Net::DNS;
use LWP::UserAgent;
use HTTP::Request;
use HTTP::Response;
use URI::URL;
use strict;

# include the configuration
require '/etc/control-services.conf';

# make my pipes piping hot
$| = 1;

# run the main routine
&updateDns;

# When a service becomes unavailable make a DNS change that will take
# that service provider out of the DNS system temporarily
sub updateDns {
    my $recovered = $ENV{"RECOVERED"};
    my $fqdn      = $ENV{"BBHOSTNAME"};
    my $ip        = $ENV{"MACHIP"};
    my $service   = $ENV{"BBSVCNAME"};
    my $color     = $ENV{"BBCOLORLEVEL"};
    my $message   = $ENV{"BBALPHAMSG"};
    my $ackcode   = $ENV{"ACKCODE"};
    my $zone = $main::zones[0];
    my $class = $main::classes[0];
    my $ttl = $main::default_ttl;
    my $type = $main::types[0];
    my $success = 0;

    # Convert the hobbit IP number format to dotted decimal format
    $ip =~ s/(...)(...)(...)(...)/$1.$2.$3.$4/;
    $ip =~ s/^0*//;
    $ip =~ s/\.0*/\./g;

    # Check if the service went down or recovered
    if (!$recovered && $color eq 'red') {
        # If it is down, remove the host from the DNS 
        my $record = "$service.$zone $type $ip";
        my @rr = ($record);
        ($success,$message) = &del_records($zone,$class,@rr);
        my $response = "";
        if ($success) {
            $response = "Relying on failover hosts.";
            #$response = &acknowledgeAlert($ackcode, $text);
        }
        &log("Failed:", $recovered, $fqdn, $ip, $service, 
                $color, $message, $response);
    } elsif ($recovered) {
        # If it is being restored, add the host back to the DNS
        $ttl      = '60';
        ($success,$message) = &add_records($zone, $class, $service, $ttl, 
                                           $type, $ip);
        my $response = "";
        if ($success) {
            $response = "Host restored to DNS.";
        }
        &log("Recovered:", $recovered, $fqdn, $ip, $service, 
                $color, $message, $response);
    }
}

# Acknowledge the failure with Hobbit so that additional notifications 
# are supressed 
# This seems to not be working properly with hobbit right now  --TODO
sub acknowledgeAlert {
    my ($ackcode, $message) = @_;
    my $action = "Ack";
    my $url = url($main::hobbit_cgi);
    $url->query_form(ACTION => $action, 
                     NUMBER => $ackcode, 
                     MESSAGE => $message);
    my $ua = LWP::UserAgent->new();
    $ua->agent("control-services/0.1");
    my $request = HTTP::Request->new(GET => $url);
    $request->referer($main::hobbit_cgi);
    $request->authorization_basic($main::uname, $main::password);
    my $response = $ua->request($request);
    if ($response->is_error() ) {
        return $response->status_line;
    } else {
        my $content = $response->content();
        return $content;
    }
}

# Log the run of the script to a temporary log file
sub log {
    my ($lead, $recovered, $fqdn, $ip, $service, $color, $message, $response) = @_;

    open(LOG,">>$main::logfile") || 
        die "Log file could not be opened.";
    print LOG $lead;
    print LOG " ";
    print LOG $ip;
    print LOG " ";
    print LOG $fqdn;
    print LOG " ";
    print LOG $service;
    print LOG " ";
    print LOG $color;
    print LOG " ";
    print LOG $recovered;
    print LOG " ";
    print LOG $message;
    print LOG " ";
    print LOG $response;
    print LOG "\n";
    close(LOG);
}

# Get a resolver to be used for DDNS updates
sub get_resolver {
    my ($tsig_keyname,$tsig_key) = @_;
    my $res = Net::DNS::Resolver->new;
    $res->tsig($tsig_keyname,$tsig_key);
    return \$res;
}

# Add a RR using DDNS update
sub add_records {
    my ($zone,$class,$name,$ttl,$type,$content) = @_;
    
    # get a resolver handle and set the dns server to use
    my $res= &get_resolver($main::tsig_keyname,$main::tsig_key);
    $$res->nameservers($main::nameservers[0]);

    # create update packet
    my $update = Net::DNS::Update->new($zone,$class);
    my $rr = "$name.$zone $ttl $type $content";
    $update->push(update => rr_add($rr));
    my $reply = ${$res}->send($update);

    # initialize return vars
    my $success = 0;
    my $message = '';

    # Did it work?
    if ($reply) {
        if ($reply->header->rcode eq 'NOERROR') {
            $message = "Update succeeded";
            $success = 1;
        } else {
            $message = 'Update failed: ' . $reply->header->rcode;
        }
    } else {
        $message = 'Update failed: ' . $res->errorstring;
    }

    return ($success,$message);
}

# Delete one or more RRs using DDNS update
sub del_records {
    my ($zone,$class,@rr) = @_;

    # get a resolver handle and set the dns server to use
    my $res= &get_resolver($main::tsig_keyname,$main::tsig_key);
    $$res->nameservers($main::nameservers[0]);

    my $update = Net::DNS::Update->new($zone,$class);

    # build update packet(s)
    foreach my $record (@rr) {
        $update->push(update => rr_del($record));
    }

    # send it
    my $reply = ${$res}->send($update);

    my $msg = '';
    my $success = 0;
    if ($reply) {
        if ($reply->header->rcode eq 'NOERROR') {
            $msg = "Update succeeded";
            $success = 1;
        } else {
            $msg = 'Update failed: ' . $reply->header->rcode;
        }
    } else {
        $msg = 'Update failed: ' . $res->errorstring;
    }
    return ($success,$msg);
}

# Print out debugging messages
sub debug {
    my $msg = shift;
    print $msg, "\n";
}
