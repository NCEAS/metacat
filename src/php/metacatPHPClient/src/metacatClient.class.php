<?php
class metacatClient {
	
	var $metacatUrl;
	//A file that the server has r/w access to
	var $cookieJar = './cookiejar';
	//Status messages sent back from metacat, getMessages()
	var $messages;

	//Constructor is run when oject is instanciated
	function metacatClient( $url ) {
		$this->metacatUrl = $url;
	}//END Constructor
		
	function login( $username, $password ) {	
		$post_data_array = array (
			'action'	=>	'login',
			'qformat'	=>	'xml',
			'username'	=>	$username,
			'password'	=>	$password
		);
		$response = $this->_request( $post_data_array );
		if(strpos($response, "<login>")) {
			return 1;
		}else{
			return 0;
		}
	}//END login
	
	function logout() {
		$post_data_array = array (
			'action'	=>	'logout',
			'qformat'	=>	'xml',	
		);
		$response = $this->_request( $post_data_array );
		if(strpos($response, "<logout>")) {
			return 1;
		}else{
			return 0;
		}
	}//END logout
	
	function loginAndSetBrowserCookies($username, $password) {
		//There could be multiple people logging in, so more than one cookieJar referenced by username is needed.
		$this->cookieJar = '/tmp/metacatPHPClientCookies/'.$username;
		//Now that the cookieJar has a new path, we can use our standard methods 
		$this->login($username, $password);
		//Set browser cookies from $this->cookieJar using fopen(...path..) or by parsing 
		//$this->getMessages() for metacat's xml response
		//Delete that cookieJar to be safe
		//return true if login == true && rm cookieJar == true
	}
	
	function setCookieJarFromBrowser() {
		//Get session cookie from browser
		//Write it to $this->cookieJar 
	}
		
	function read( $docid ) {
		$post_data_array = array (
			'action'	=>	'read',
			'qformat'	=>	'xml',
			'docid'		=>  $docid
		);
		$response = $this->_request( $post_data_array );
		if(strpos($response, "<error>")) {
			return 0;
		}else{
			return $response;
		}
	}//END read
	
	/**
	 *	Pass a docid without revision, or pisco_data.1.1
	 *	becomes pisco_data.1
	*/
	function getNewestDocId( $docidWithoutRevision ) {
		$pieces = explode(".", $docidWithoutRevision);
		if (count($pieces) > 2) {
			$this->_setMessages("You included the revision number. Changing $docidWithoutRevision to ".$pieces[0].".".$pieces[1]);
		}
		$docidWithoutRevision = $pieces[0].".".$pieces[1];
		$xml = $this->read($docidWithoutRevision);
		if(!$xml) {
			$this->_setMessages('No package with this docId found');
			return false;
		}
		
		$packageIdParser = new PackageIdParser();
		$xml_parser = xml_parser_create();
		xml_set_object($xml_parser,&$packageIdParser); 
		xml_set_element_handler($xml_parser, "startElement", "endElement");
		xml_parse($xml_parser, $xml, true);
		xml_parser_free($xml_parser);
		
		if($packageId = $packageIdParser->packageId) {
			return $packageId;
		}else{
			$this->_setMessages('No packageId found.');
			return false;
		}
	}
	
	function squery( $pathquery_doc ) {
		$post_data_array = array (
			'action'	=>	'squery',
			'qformat'	=>	'xml',
			'query'		=>  $pathquery_doc
		);
		$response = $this->_request( $post_data_array );
		if($response) {
			return $response;
		}else{
			return 0;
		}
	}//END squery

	function insert( $docid, $doctext ) {
		$post_data_array = array (
			'action'	=>	'insert',
			'docid'		=>  $docid,
			'doctext'	=>	$doctext
		);
		$response = $this->_request( $post_data_array );
		if(strpos($response, "<success>")) {
			return 1;
		}else{
			return 0;
		}
	}//END insert
	
	function update( $docid, $doctext ) {
		$post_data_array = array (
			'action'	=>	'update',
			'docid'		=>  $docid,
			'doctext'	=>	$doctext
		);
		$response = $this->_request( $post_data_array );
		if(strpos($response, "<success>")) {
			return 1;
		}else{
			return 0;
		}
	}//END update
	
	function delete( $docid ) {
		$post_data_array = array (
			'action'	=>	'delete',
			'docid'		=>  $docid,
		);
		$response = $this->_request( $post_data_array );
		if(strpos($response, "<success>")) {
			return 1;
		}else{
			return 0;
		}
	}//END delete
	
	function getMessages() {
		return $this->messages;
	}//END getMessages
	
	function _setMessages( $response ) {
		$this->messages .= " \n ".$response;
	}//END _set_messages
	
	function _request( $post_data_array ) {
		$request = curl_init();
		curl_setopt($request, CURLOPT_URL, $this->metacatUrl);
		curl_setopt($request, CURLOPT_COOKIEFILE, $this->cookieJar);
		curl_setopt($request, CURLOPT_COOKIEJAR, $this->cookieJar);
		curl_setopt($request, CURLOPT_POST, true);
		curl_setopt($request, CURLOPT_RETURNTRANSFER, true);
		curl_setopt($request, CURLOPT_USERAGENT, 'PHP metacat client (version 1.0)');
		
		$post_data = $this->_encode_post_data($post_data_array);
		curl_setopt ($request, CURLOPT_POSTFIELDS, $post_data);
		$response = curl_exec($request);
		$this->_setMessages($response);
		return $response;
	}//END _request
	
	function _encode_post_data( $post_data_array ) {
		foreach($post_data_array as $arg => $value) {	
			$post_data_array[$arg] = $arg."=".urlencode($value);
		}
		$post_data = implode('&', $post_data_array);
		return $post_data;
	}//END _encode_post_data
	
		
}

class PackageIdParser {
	var $packageId;
	
	function startElement($parser, $name, $attrs) {
		if($name == "EML:EML") {
			$this->packageId = $attrs['PACKAGEID'];
		}
	}

	function endElement($parser, $name) {
	}
}

?>
