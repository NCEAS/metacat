$:.unshift File.join(File.dirname(__FILE__), "..", "lib")

require 'test/unit'
require 'eml.rb'
require 'metacat.rb'
require 'data_table.rb'

class DataTableTest < Test::Unit::TestCase

  def setup
    @eml = Metacat.find(:docid => 'HMS001_020ADCP019R00_20060612.50.1')
    @data_table = @eml.largest_data_table
  end
  
  def teardown
  end

  # Replace this with your real tests.
  def test_id
    assert_equal  @data_table.docid, @data_table.id
  end
  
  def test_docid
    assert_equal  'HMS001_020ADCP019R00_20060612.40.1', @data_table.docid
  end
  
  def test_location
    assert_equal  'ecogrid://knb/HMS001_020ADCP019R00_20060612.40.1', 
                    @data_table.location
  end
  
  def test_size
    assert_equal @data_table.size, 158041130
  end
  
  def test_physical
    assert_kind_of  REXML::Element, @data_table.physical
  end
  
  def test_data_format
    assert_equal 'textFormat', @data_table.data_format
  end
  
  def test_text_format
    assert_kind_of REXML::Element, @data_table.text_format
  end
  
  def test_simple_delimited
    assert_kind_of REXML::Element, @data_table.simple_delimited
  end
  
  def test_field_delimiter
    assert_kind_of  String, @data_table.field_delimiter
  end
  
  def test_num_headers
    assert_equal 1, @data_table.num_headers
  end
  
  def test_record_delimiter
    assert_equal '#x0A', @data_table.record_delimiter
  end
    
  def test_metadata_attr_reader
    assert_kind_of  REXML::Element, @data_table.metadata
  end
  
  def test_read
    f1 = File.new("tmp/#{@data_table.docid}", "w+")
    @data_table.read do |buffer|
      f1.write buffer
    end
    f1.close
    assert_equal  File.size("test/fixtures/#{@data_table.id}"),
                    File.size("tmp/#{@data_table.id}")
  end

end
