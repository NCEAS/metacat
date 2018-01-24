$:.unshift File.join(File.dirname(__FILE__), "..", "lib")
require 'test/unit'
require 'eml.rb'
require 'metacat.rb'

class EmlTest < Test::Unit::TestCase
  
  def setup
    @metacat = Metacat.new('http://data.piscoweb.org/catalog/metacat')
    @eml = @metacat.find(:docid => 'HMS001_020ADCP019R00_20060612.50.1')
  end
  
  def teardown
  end

  def test_temporal_coverage
    assert_equal [Date.strptime('2006-06-12'), Date.strptime('2006-08-30')] ,       
                  @eml.temporal_coverage
  end
  
  def test_has_data_tables
    assert_kind_of  Array, @eml.data_tables
    assert_kind_of  DataTable, @eml.data_tables[0]
  end
  
  def test_largest_of_one_data_table
    assert_kind_of  DataTable, @eml.largest_data_table
  end

  def test_largest_of_many_data_table
    eml = @metacat.find(:docid => 'pisco_subtidal.12.3')
    assert_kind_of  DataTable, eml.largest_data_table
    assert_equal    'pisco_subtidal.14.1', eml.largest_data_table.docid
  end

  def test_to_s
    assert_kind_of  String, @eml.to_s
  end
  
  def test_has_xml_doc
    assert_kind_of  REXML::Document, @eml.doc
  end
  
  def test_correct_docid
    assert_equal  'HMS001_020ADCP019R00_20060612.50.1', @eml.docid
  end
  
  def text_to_xml
    # While the method is to_xml, this method outputs the xml as a string,
    # not a REXML DOM object
    assert_kind_of  String, @eml.to_xml
  end
  
end
