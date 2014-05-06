#!/usr/bin/perl -w
# This is a example showing how to use Swamp::Connector;

use strict;
use Swamp::Connector;
use Data::Dumper;

my $swamp;
my $retval;


# perform login: 
if (not defined ($swamp = new Swamp::Connector("localhost","8080","tschmidt","xxx"))) {
   die "SWAMP Login Error \n";
}


#prepare events for sending: 
if (not defined ($retval = $swamp->send_event(6630,"TESTEVENT"))) {
   print "Error: ".$swamp->error() . "\n";
} else {
   print "Success: " . $retval . "\n";
}


# test setting a single databit
if (not defined ($retval = $swamp->send_databit(6630,"laufzettelset.patch_priority","99"))) {
   print "Error: ".$swamp->error() . "\n";
} else {
   print "Success: " . $retval . "\n";
}


# prepare databitvalues for writing: 
my %databits = (
	'laufzettelset.patch_priority' => "77",
	'laufzettelset.roles.owner' => "swamp_user",
);
if (not defined ($retval = $swamp->send_databits(6630, \%databits))) {
   print "Error: ".$swamp->error() . "\n";
} else {
   print "Success: " . $retval . "\n";
}


# get a databit value: 
if (not defined ($retval = $swamp->get_databitvalue(6630, 'laufzettelset.roles.owner'))) {
   print "Error: ".$swamp->error() . "\n";
} else {
   print "Success: " . $retval . "\n";
}


# get a patchinfoid: 
if (not defined ($retval = $swamp->get_patchinfoid(6630))) {
   print "Error: ".$swamp->error() . "\n";
} else {
   print "Success: " . $retval . "\n";
}



# finally log out
$swamp->logout();