$:.unshift File.join(File.dirname(__FILE__), "..", "lib")
require "rexml/document"
require "net/http"
require "uri"
require "eml.rb"

# Changing buffer size to greatly improve performance 
class Net::BufferedIO
  def rbuf_fill
    timeout(@read_timeout) {
      #changed from 1024
      @rbuf << @io.sysread(32768)
    }
  end
end

# = Metacat Client Library
# == What is it
# A client for the Metacat data catalog. For a description of Metacat, see
# http://knb.ecoinformatics.org/software/metacat
# For now, this client does not implement all features of the API. Rather, 
# it focuses on querying and returning Eml metadata objects from either 
# pathqueries or docid's. Should you find yourself using methods other than find()
# very often, you may be veering from the original intent.
# 
# ==   Examples
# ===  Read metadata for a public document
#
#   require 'lib/metacat.rb'
#   metacat = Metacat.new('http://data.piscoweb.org/catalog/metacat')
#   eml = metacat.find(:docid => 'pisco.10.4')
#   puts eml.docid
#   => 'pisco.10.4'
#
# === Log into Metacat and read Eml metadata. Then logout
#
#   username = 'uid=cburt,o=PISCO,dc=ecoinformatic,dc=org'
#   password = *****
#   Metacat.new('http://data.piscoweb.org/catalog/metacat', username, password) do |metacat|
#     eml = metacat.find(:docid => 'pisco.10.3')
#     start, end = eml.temporal_coverage
#     puts "start: #{start}, end: #{end}" 
#   end
#
# === Search for oceanographic data
#
#   metacat = Metacat.new('http://data.piscoweb.org/catalog/metacat')
#   pathquery = '...' # see example at http://knb.ecoinformatics.org/software/metacat/metacatquery.html
#   docs = metacat.find(:squery => pathquery)
#   docs.each { |eml| puts eml.docid }
#
# === Find and write a data_table to local disk
#   Metacat.new('http://data.piscoweb.org/catalog/metacat', username, password) do |metacat|
#     file = File.new('tmp', 'w+')
#     # using a block you can avoid loading the whole file into memory!
#     metacat.read('data_table.1.1') do |fragment|
#       file.write(fragment)
#     end
#     file.close
#   end 
class Metacat
      
  def initialize(path_to_metacat, options = {}, &block)
    @uri = URI.parse(path_to_metacat)
    @cookie = false
    if options.has_key?('username') && options.has_key?('password')
      login(options['username'], options['password'])
    end
    if block_given?
      yield self
      logout if @logged_in
    end
  end
  
  # Check if the metacat instance has a session cookie
  def logged_in?
    if @cookie 
      true
    else 
      false
    end
  end
  
  # Returns either an array of Eml documents(or nil) if :squery is passed or
  # a single Eml document(or nil) if passed :docid. This function _will_ _not_
  # return a data table, only Eml objects.
  #
  # If you need to retrieve a data table or other document, use read()
  #
  # Examples:
  #   Metacat.find(:docid => 'cbs_10.1')
  #   Metacat.find(:squery => xml_path_query)
  # 
  def find(args)
    if args[:docid] && args[:squery]
      raise ArgumentError, "Too many parameters. Choose :docid or :squery"
    elsif args[:docid]
      result = read(args[:docid], 'only_eml' => true)
      unless result.nil?
        try_eml(result) || raise(ArgumentError, "#{args[:docid]} does not refer to eml metadata. To read other documents use read.")
      end
    elsif args[:squery]
      results = squery(args[:squery])
      doc = REXML::Document.new(results)
      documents = Array.new()
      doc.elements.each("/resultset/document") { |document|
        docid = document.elements[1].text
        documents.push(try_eml(read(docid, 'only_eml' => true)))
      }
      return documents.delete_if {|doc| doc == false }
    end
  end
  
  # Logs into metacat using ldap authentication. Usernames are complex, such as 
  # 'uid=cburt,o=PISCO,dc=ecoinformatics,dc=org'
  #
  # Raises MetacatPermissionDenied exception on fail
  #
  # Example
  #   metacat.login('uid=cburt,o=PISCO,dc=ecoinformatics,dc=org', '******')
  #   => true
  def login(username, password)  
    response = metacat_get({
      'action'    =>  'login',
      'qformat'   =>  'xml',
      'username'  =>  username,
      'password'  =>  password
    })
    if(response.content_type == 'text/xml')
      doc = REXML::Document.new(response.read_body)
      if(doc.root.name == 'login')
        @cookie = response.response['set-cookie']
        @logged_in = true
      else
        raise MetacatPermissionDenied, "login error: "+doc.root.elements['message'].text
      end
    else
      raise MetacatResponseError
    end
  end
  
  def logout
    response = metacat_get({
      'action'    =>  'logout',
      'qformat'   =>  'xml'
    })
    if(response.content_type == 'text/xml')
      doc = REXML::Document.new(response.read_body)
      if doc.root.name = 'logout'
        @cookie = false
        return true
      else
        raise 'Failed to logout: '+doc.root.text
      end
    else
      raise MetacatResponseError
    end    
  end
  
  # Reads a specified document from metacat. If xml is found, a REXML::Document will be returned
  #
  # When reading text data tables, it should be noted that loading the entire large file can
  # consume an enormous amount of memory. To avoid this, read can be passed a &block. The block
  # will recieve fragments of the file as it comes in.
  #
  # Examples:
  # Reading an EML document
  #   metacat.read('eml_doc.1.1')
  #   => <REXML::Document >
  # 
  # Writing a data table to disk
  #   file = File.new('tmp', 'w+')
  #   metacat.read('data_table.1.1') do |fragment|
  #     file.write(fragment)
  #   end
  #   file.close
  #
  # Reading an entire data table into memory
  #   data_table = metacat.read('data_table.1.1')
  def read(docid, options = {}, &block) # :yields: xml or data_table fragment
    data = {
      'action'  =>  'read',
      'qformat' =>  'xml',
      'docid'   =>  docid
    }
    metacat_get(data) do |response|
      if response.content_type == 'text/xml'
        doc = REXML::Document.new(response.read_body)
        if(doc.root.name == 'error')
          if(doc.root.text.match('permission'))
            raise MetacatPermissionDenied, doc.root.text
          elsif(doc.root.text.match('does not exist'))
            # Nothing found, return nil
            return nil
          else
            raise 'Unrecognized response from metacat: '+doc.root.text
          end
        else # xml data
          return doc
        end
      else # probably a data table
        if (options.has_key?('only_eml') && options['only_eml'] == true)
          return nil
        else
          if block_given?
            response.read_body { |buffer| yield buffer }
          else
            response.read_body
          end
        end
      end      
    end
  end  
  
  # Uses the metacat pathquery search and returns the xml response as a string.
  # For query format information, see 
  # http://knb.ecoinformatics.org/software/metacat/metacatquery.html 
  def squery(squery)
    response = metacat_get({
      'action'  =>  'squery',
      'qformat' =>  'xml',
      'query'   =>  squery
    })
    if(response.content_type == 'text/xml')
      response.read_body
    else
      raise "Metacat returned unexpected Content Type"
    end
  end

  private
  
  def try_eml(doc)
    begin
      Eml.new(doc)
    rescue ArgumentError
      return false
    end
  end
  
  def metacat_post(data, &block)
    Net::HTTP.start(@uri.host, @uri.port) do |http|
      if block_given?
        http.request_post(@uri.path, data, headers) {|r| yield(r) }
      else
        http.post(@uri.path, data, headers)
      end
    end
  end
  
  def metacat_get(data, &block)
    path = @uri.path
    path = path+query_string(data)
    Net::HTTP.start(@uri.host, @uri.port) do |http|
      if block_given?
        http.request_get(path, headers) {|r| yield(r) }
      else
        http.get(path, headers)
      end
    end
  end
  
  def query_string(hash)
    qstring = []
    hash.each {|k, v| qstring << "#{k}=#{URI.encode(v)}" }
    '?'+qstring.join('&')
  end
  
  def headers
    {'Cookie' =>  @cookie} if @cookie
  end
  
end

class MetacatPermissionDenied < RuntimeError
end

class MetacatResponseError < RuntimeError
end