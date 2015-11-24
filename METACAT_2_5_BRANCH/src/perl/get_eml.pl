#!/usr/bin/perl

# check that the correct number or parameters are issued
if (($#ARGV +1) != 3) {die "Usage: %./get_eml.pl <metacat_url> <login> <passwd>\n\n";}

($url, $username, $password) = @ARGV; #get the input file and output file names


use Metacat;
use XML::DOM;

my $metacat = Metacat->new();


if ($metacat) {
    $metacat->set_options( metacatUrl => $url );
} else {
#print "Failed during metacat creation.";
    $error = 1;
    exit();
}

# Login to metacat
# //print "Logging in to metacat..........\n";
my $response1 = $metacat->login($username, $password);
if (! $response1) {
#    //print $metacat->getMessage();
#    //print "Failed during login: metacat.\n";
    $error = 2;
} else {
#    //print "Logged in.\n";
}


my $query = "<?xml version=\"1.0\" ?> <pathquery version=\"1.2\">  <querytitle>Untitled-Search-2</querytitle>  <returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype> <returndoctype>-//NCEAS//eml-dataset-2.0//EN</returndoctype>  <returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>  <returnfield>dataset/title</returnfield>  <returnfield>individualName/surName</returnfield> <returnfield>keyword</returnfield><returnfield>westBoundingCoordinate</returnfield><returnfield>eastBoundingCoordinate</returnfield><returnfield>northBoundingCoordinate</returnfield><returnfield>southBoundingCoordinate</returnfield><returnfield>westbc</returnfield><returnfield>eastbc</returnfield><returnfield>northbc</returnfield><returnfield>southbc</returnfield><querygroup operator=\"INTERSECT\"><querygroup operator=\"UNION\"><querygroup operator=\"UNION\"><queryterm searchmode=\"contains\" casesensitive=\"false\"><value>%25</value></queryterm></querygroup></querygroup></querygroup></pathquery>";

my $response = $metacat->squery($query);

my $mesg =$metacat->getMessage();

#print "\n\ngetting the message ... \n";
#print $mesg;

if($mesg eq ""){
#    print ("Too much time is reply back from metacat...");
    exit();
}

my $parser = new XML::DOM::Parser;
my $node;
my $name;
my $doc = $parser->parse($mesg);
my $nodes = $doc->getElementsByTagName("docid");

$numberNodes = $nodes->getLength;


for (my $loop_index =0; $loop_index < $numberNodes; $loop_index++)
{
    $node = $nodes->item($loop_index);
    $name =  trimwhitespace($node->getFirstChild()->getNodeValue());
    
    $node = $node->getParentNode(); 
    my $tempnodes = $node->getElementsByTagName("param");
    my $tempnumberNodes = $tempnodes->getLength;
 
    my $title = "";
    my $keyword = "";
    my $cName = "";
    my $eBC = "";
    my $wBC = "";
    my $nBC = "";
    my $sBC = "";
    
 for (my $loop =0; $loop < $tempnumberNodes; $loop++) 
 {
	my $tempnode = $tempnodes->item($loop);	
	my $paramname = $tempnode->getAttributeNode("name")->getValue();
	if($paramname eq "dataset/title"){
	    $title = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "keyword"){
	    $keyword = trimwhitespace($keyword.",".$tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "individualName/surName"){
	    $cName = trimwhitespace($cName.",".$tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "eastBoundingCoordinate"){
	    $eBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "eastbc"){
	    $eBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "westBoundingCoordinate"){
	    $wBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "westbc"){
	    $wBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "northBoundingCoordinate"){
	    $nBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "northbc"){
	    $nBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "southBoundingCoordinate"){
	    $sBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
	if($paramname eq "southbc"){
	    $sBC = trimwhitespace($tempnode->getFirstChild()->getNodeValue());
	}
    }

    if($keyword ne "" ){
	$keyword = substr ($keyword, 1);
    }
    if($cName ne "" ){
	$cName = substr ($cName, 1);
    }

  #  print "$name, $title, ($cName), ($keyword), ($eBC,$wBC,$nBC,$sBC), $url?action=read&docid=$name&qformat=knb, \n";
  # print trimwhitespace($eBC) +  " \n";
 if ($eBC ne "" && $nBC ne "" && $eBC ne "0" &&  $nBC ne "0") { 
    print "$name $wBC $eBC  $sBC  $nBC $url?action=read&docid=$name&qformat=knb \n";
 }
}


# Remove whitespace from the start and end of the string
sub trimwhitespace($)
{
  my $string = shift;
  $string =~ s/^\s+//;
  $string =~ s/\s+$//;
  return $string;
}
#print $numberNodes;
