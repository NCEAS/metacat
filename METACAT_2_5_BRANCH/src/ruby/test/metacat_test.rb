$:.unshift File.join(File.dirname(__FILE__), "..", "lib")
require "metacat.rb"
require "test/unit"

class MetacatTest < Test::Unit::TestCase

  def setup
    # This test case is setup for use against the ucsb/msi metacat server
    # You need a valid login to run the test case as well as an squery, eml-docid,
    # and datatable docid that will all return documents
    @username = 'uid=cburt,o=PISCO,dc=ecoinformatics,dc=org'
    @password = '7lobster'
    @metacat = Metacat.new("http://data.piscoweb.org/catalog/metacat")
    #must return at least one eml document
    @squery = '<?xml version="1.0"?>
                 <pathquery version="1.2">
                   <returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>
                   <returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>
                   <returnfield>dataset/title</returnfield>
                   <returnfield>dataTable/entityName</returnfield>
                   <returnfield>creator/individualName/surName</returnfield>
                   <returnfield>creator/organizationName</returnfield>
                   <returnfield>dataTable/physical/distribution/online/url</returnfield>
                   <querygroup operator="INTERSECT">
                     <queryterm casesensitive="false" searchmode="starts-with">
                       <value>PISCO:</value>
                       <pathexpr>title</pathexpr>
                     </queryterm>
                     <querygroup operator="INTERSECT">
                       <queryterm casesensitive="true" searchmode="equals">
                         <value>Subtidal Community Survey Data</value>
                         <pathexpr>keywordSet/keyword</pathexpr>
                       </queryterm>
                       <queryterm casesensitive="true" searchmode="equals">
                         <value>PISCO Categories</value>
                         <pathexpr>keywordSet/keywordThesaurus</pathexpr>
                       </queryterm>
                     </querygroup>
                   </querygroup>
                 </pathquery>'
    @data_table_docid = 'HMS001_020ADCP019R00_20060612.40.1'
    @eml_docid = 'HMS001_020ADCP019R00_20060612.50.1'
    # Not accessable to user Public
    @locked_docid = 'chad.1.1'
  end
  
  def teardown
  end
  
  # Metacat.new
  def test_new
    assert_kind_of Metacat, Metacat.new("http://data.piscoweb.org/catalog/metacat")
  end
  
  def test_initial_login
    metacat = Metacat.new("http://data.piscoweb.org/catalog/metacat", 
    'username' => @username, 'password' => @password)
    assert metacat
    assert metacat.logged_in?
  end
  
  def test_login_and_yield
    Metacat.new("http://data.piscoweb.org/catalog/metacat", 
    'username' => @username, 'password' => @password) do |metacat|
      assert metacat.logged_in?
    end
  end
  
  # Metacat.find()
  def test_error_if_docid_and_squery_set?
    assert_raises ArgumentError do
      @metacat.find(
      :docid => @eml_docid, 
      :squery => 'bs'
      )
    end
  end
    
  def test_nil_if_document_does_not_exist?
    assert_nil @metacat.find(:docid => 'bs_docid.80.9')
  end
  
  def test_permission_denied
    assert_raise(MetacatPermissionDenied) { @metacat.find(:docid  =>  @locked_docid) }
  end
  
  def test_returns_eml?
    assert_kind_of Eml, 
    @metacat.find(:docid => @eml_docid)  
  end
  
  def test_will_not_return_data_table
    assert_nil @metacat.find(:docid => @data_table_docid)
  end
  
  def test_returns_array_of_eml_objects?
    results = @metacat.find(:squery => @squery)
    assert_kind_of Array, results
    assert_kind_of Eml, results[0]
  end
  
  # Metacat.login/logout
  def test_login
    assert @metacat.login(@username, @password)
    assert @metacat.logged_in?
  end
  
  def test_logout
    assert @metacat.login(@username, @password)
    assert @metacat.logout
    assert_equal false, @metacat.logged_in?
  end  
  
  def test_failed_login
    assert_raise(MetacatPermissionDenied) do
      @metacat.login('bleh', @password)
    end
  end
  
  # Metacat.read
  def test_read_eml
    doc = @metacat.read(@eml_docid)
    assert_kind_of(REXML::Document, doc)
    assert_equal doc.root.name, 'eml'
  end
  
  def test_read_xml
    # not sure how to search for this yet
  end
  
  def test_read_data_table
    file = File.open('tmp.data_table', 'w+')
    @metacat.read(@data_table_docid) do |buffer|
      file.write(buffer)
    end
    file.close
    assert_equal(File.size('tmp.data_table'), File.size(File.dirname(__FILE__)+'/example.data_table'))
    File.delete('tmp.data_table')
  end
  
  # Metacat.squery
  def test_returns_xml
    doc = REXML::Document.new(@metacat.squery(@squery))    
    assert doc
    assert_equal 'resultset', doc.root.name
  end
  
  # Metacat.insert
  
  # Metacat.update
  
  # query_string
  def test_string_formatting
    hash = {
      'genus'     =>  'Caranx',
      'species'   =>  'melampygus'
    }
    assert_equal  '?genus=Caranx&species=melampygus', @metacat.send(:query_string, hash)
  end
end
