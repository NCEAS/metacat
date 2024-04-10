#!/usr/bin/perl

# manage-domain.pl -- Manage a domain using DDNS to add or remove hosts
#

use Net::DNS;
use Getopt::Long;
use strict;

# include the configuration
require '/etc/control-services.conf';

# make my pipes piping hot
$| = 1;

# run the main routine
&main;

sub main {
    my ($host, $ip, $command);

    GetOptions( "host=s" => \$host,
                "ip=s"   => \$ip,
                "command=s"   => \$command );
    if (! defined($host) || ! defined ($ip) || !defined($command)) {
        usage();
    } else {
        my $zone = $main::zones[0];
        my $class = $main::classes[0];
        my $ttl = '60';
        my $type = 'A';
        my $message = '';
        my $success = 1;

        # get a resolver handle and set the dns server to use
        my $resref = &get_resolver($main::tsig_keyname,$main::tsig_key);
        $$resref->nameservers($main::nameservers[0]);

        if ($command eq 'add') {
            # Add a record
            ($success,$message) = &add_records( $resref, $zone, $class,
                                        $host, $ttl, $type, $ip );
            debug("Add records success: " . $success);
            debug("Add records message: " . $message);
        } elsif ($command eq 'delete') {
            # Delete a record
            my $record = "$host.$zone $type $ip";
            #my $record = "$host.$zone";
            my @rr = ($record);
            ($success,$message) = &del_records($resref,$zone,$class,@rr);
            debug("Del records success: " . $success);
            debug("Del records message: " . $message);
        } else {
            usage();
        }
    }
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
    my ($res,$zone,$class,$name,$ttl,$type,$content) = @_;
    
    # create update packet
    my $update = Net::DNS::Update->new($zone,$class);
    my $rr = "$name.$zone $ttl $type $content";
    debug("Inserting new record: \n    " . $rr);
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
    my ($res,$zone,$class,@rr) = @_;

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
        $success = 0;
        }
    } else {
        $msg = 'Update failed: ' . $res->errorstring;
    $success = 0;
    }
    return ($success,$msg);
}

# Print out debugging messages
sub debug {
    my $msg = shift;
    print $msg, "\n";
}

# Print a usage statement
sub usage {
        print "Usage: manage-domain.pl --host hostname --ip xxx.xxx.xxx.xxx --command [add|delete]\n";
}
