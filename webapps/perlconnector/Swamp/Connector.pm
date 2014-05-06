#------------------------------------------------------------------------------
# Module Header
# Filename          : Swamp/Connector.pm
# Purpose           : Do remote calls to a SWAMP server
# Author            : Harald Müller-Ney <hmuelle@suse.de>
#                     Thomas Schmidt <tschmidt {AT} suse.de>
# Date              : 20040927
# Description       : This module does connect to SWAMP via HTTP. 
#                     Its functionality is limited in comparision to 
#                     the SOAP client, but does not need a SOAP and XML libs.
# Language Version  : Perl 5
#------------------------------------------------------------------------------

# each method returns a result String on Success. 
# to check if an error happened use: 
# if (not defined ($result = $swamp->method( ... )) ) {
#   print "Error: ".$swamp->error() . "\n";
# }

package Swamp::Connector;

use strict;
use LWP::UserAgent;

# Set to 1 for DEBUG OUTPUT
my $DEBUG=1;


# create already logged in object
sub new($) {
    my ($class, $server, $port, $user, $passwd) = @_;
	my $self = {};
	bless($self, $class);

	# initialize object data
	$self->{server}=$server;
	$self->{port}=$port;
	$self->{sessionID}=undef;
	$self->{http}=LWP::UserAgent->new();
	$self->{http}->timeout(10);

	# create a swamp connection
	if ( defined ( $self->login($user, $passwd) ) ) {
    	return $self;
	} else {
	    # fatal to fail here because the errormessage is unavaiable from extern.
	    print STDERR $self->error();
		return undef;
	}
};


# return last errormessage
sub error {
    my $self = shift || return undef;
    return exists $self->{lastFaultMsg} ? $self->{lastFaultMsg} : undef;
}


# send a login request and store cookie
sub login {
    my ($self, $user, $passwd) = @_;

	# prepare POST variables
	my %vars = (
		"action" => "LoginActions",
		"eventSubmit_doLoginuser" => "true",
		"username" => $user,
		"password" => $passwd,
		"xmlresponse" => "true",
	);
	my $res = $self->{http}->post("http://".$self->{server}.":".$self->{port}."/webswamp/swamp/", \%vars);
	return $self->get_message($res);
}


# parse the returned XML and return the message on success, 
# or return undef and set the $lastFaultMsg on failure.
sub get_message {
	my ($self, $res) = @_;
	
	if (not ($res->is_success)){
		$self->{lastFaultMsg} = "SWAMP Connection Error: " . $res->status_line . "\n";
		return undef;
	}

	my $body = $res->content;
	print "DEBUG: ".$res->status_line ."\n" if $DEBUG;    
    #print "DEBUG: HTML Body:\n".$body."\n" if $DEBUG;
    my $error = $1 if $body =~ /<errorcode>(.*?)<\/errorcode>/s;
    my $retval = $1 if $body =~ /<msg>(.*?)<\/msg>/s;
    
	if ($error != "0"){
		$self->{lastFaultMsg} = $retval;
		print "DEBUG: Errormessage was: $retval \n" if $DEBUG;
		return undef;
	} elsif (defined ($retval)) {
		my $cookie = $res->header('Set-Cookie');
    	# extract sessionID 
		if ($cookie){
			$cookie =~ s/.*JSESSIONID=(.*)$/$1/g;
			chomp $cookie;
			$self->{sessionID} = $cookie;
			print "DEBUG: New SessionId: ".$self->{sessionID}."\n" if $DEBUG;
		}
		print "DEBUG: Returned value was: $retval \n" if $DEBUG;
	    return $retval;
	} else {
		$self->{lastFaultMsg} = "Unparseable response from SWAMP: " . $body;
		print "DEBUG: Unparseable response from SWAMP. \n" if $DEBUG;
		return undef;
	}
}

sub logout {
	my $self=shift;
	my %var = (
		"action" => "LoginActions",
		"eventSubmit_doLogoutuser" => "true",
    		"xmlresponse" => "true",
	);
    	$self->{http}->post("http://".$self->{server}.":".$self->{port}."/webswamp/swamp/;jsessionid=".$self->{sessionID}, \%var);
	return 1;
}


# sent eventstring to a workflow
sub send_event {
	my ($self, $wfid, $event) = @_;

	# prepare POST variables
    my %var = (
        "eventSubmit_doSendevent" => "true",
        "action" => "ExternalActions",
        "etype" => $event,
        "wfid" => $wfid,
    );
	return $self->send_swamp_request(\%var);
}


# update several databits at once. $databits is supposed to be a hash reference!
sub send_databits {
	my ($self, $wfid, $databits) = @_;
    my %databits = %$databits;

	my %var = (
		"action" => "ExternalActions",
		"workflowid" => $wfid,
		"eventSubmit_doSenddata" => "true"
	);
	foreach my $databitname ( keys %databits ) {
		$var{"field_$databitname"}=$databits{$databitname};
	}
	return $self->send_swamp_request(\%var);
}

# update the value of a databit: 
sub send_databit {
	my ($self, $wfid, $databitname, $databitvalue) = @_;
	# prepare POST variables
	
	my %var = (
		"action" => "ExternalActions",
		"workflowid" => $wfid,
		"field_$databitname" => $databitvalue,
		"eventSubmit_doSenddata" => "true"
	);
	
	return $self->send_swamp_request(\%var);
}

# get the value of a databit
sub get_databitvalue {
	my ($self, $wfid, $databitname) = @_;
	# prepare POST variables
	my %var = (
		"action" => "ExternalActions",
		"workflowid" => $wfid,
		"databitname" => $databitname,
		"eventSubmit_doGetdata" => "true"
	);
	return $self->send_swamp_request(\%var);
}

# get back a patchinfoid (SUBSWAMPID) by providing the issueid
sub get_patchinfoid {
	my ($self, $wfid) = @_;

	# prepare POST variables
	my %var = (
		"action" => "workflows.MaintenanceTracker.AutobuildActions",
		"issueid" => $wfid,
		"eventSubmit_doGetpatchinfoid" => "true"
	);
	return $self->send_swamp_request(\%var);
}


# $var is a HASH reference with the values to post
sub send_swamp_request {
	my ($self, $var) = @_;
    $var->{"xmlresponse"}="true";
    my $res = $self->{http}->post("http://".$self->{server}.":".$self->{port}."/webswamp/swamp/;jsessionid=".$self->{sessionID}, $var);
    return $self->get_message($res);
}


1; # must be the last statement in the module file!

__END__
