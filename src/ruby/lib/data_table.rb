$:.unshift File.join(File.dirname(__FILE__), "..", "lib")
require "eml.rb"

# ==  What is it
# Each EML document can contain DataTable elements representing(in most cases) plain
# text data. The attributes of this data, such as column names, types, domain info,
# are documented in the eml metadata. DataTable encapsulates data table elements from
# eml documents in the instance variable @metadata.
# 
# The purpose of this class is to provide methods to easily access metadata attributes
# pertaining to the data table. It can also be extended for specific
# applications to process the data based on the metadata description.
# == Examples
# === Get the location where this data file is stored
#   eml = metacat.find(:docid => 'somedoc.1.1')
#   data_table = eml.data_tables[0]
#   data_table.location
#   => ecogrid://knb/pisco_cbs.30.3
# === Write the data file to disk
#   # note we are using a block so the whole file isn't loaded to RAM
#   file = File.new("./tmp/#{data_table.id}")
#   data_table.read do |buffer|
#     file.write(buffer)
#   end
#   file.close 
class DataTable
  attr_reader :metadata, :eml
  
  def initialize(data_table_element, eml)
    @metadata = data_table_element
    @eml = eml
  end
  
  # Methods for accessing eml metadata
  # ----------------------------------
  
  # pulls the docid from distribution element
  def docid
    @docid ||= location.reverse.match('[^/]+')[0].reverse
  end
  
  # refers to the docid function
  def id
    docid
  end
  
  # attribute reader for online distribution
  def location
    @location ||= @metadata.elements['physical/distribution/online/url'].text
  end
  
  def physical
    @metadata.elements['physical']
  end
  
  # only supports unit bytes
  def size
    physical.elements['size'].text.to_i
  end
  
  def data_format
    physical.elements['dataFormat'].elements[1].name
  end
  
  def field_delimiter
    text_format.elements[]
  end
  
  def text_format
    physical.elements['dataFormat/textFormat']
  end
  
  def simple_delimited
    if text_format
      text_format.elements['simpleDelimited']
    else
      raise "data table is not in textFormat"
    end
  end
  
  def num_headers
    if text_format
      text_format.elements['numHeaderLines'].text.to_i      
    else
      raise "data table is not in textFormat"
    end
  end
  
  def record_delimiter
    if text_format
      text_format.elements['recordDelimiter'].text      
    else
      raise "data table is not in textFormat"
    end
  end
  
  def field_delimiter
    if simple_delimited
      simple_delimited.elements['fieldDelimiter'].text      
    else
      raise "data table is not in simpleDelimited format"
    end
  end
      
  def columns
    cols = Array.new
    @metadata.elements.each('attributeList/attribute') do |col|
      cols.push col
    end
    return cols
  end
  
  def entity_name
    @metadata.elements['entityName'].text
  end
  
  # ---------------------------
  # End Metadata Access Methods
  
  # reads the dataTable text from the url or docid specified
  # by the physical/distribution/online/url entity
  def read
    if(location =~ /ecogrid/)
      #we need to pull out the docid and do a read on metacat
      #get self.location, and pull out the string after the last "/"
      uri = URI.parse(PATH_TO_METACAT)
      uri.query = "action=read&qformat=xml&docid=#{docid}"
      # Use Net:HTTP first to get the content_type
      http = Net::HTTP.start(uri.host, uri.port)
      http.request_get(uri.to_s) do |response|
        if(response.content_type == 'text/xml')
          # error message
          doc = REXML::Document.new(response.read_body)
          if(doc.root.name == 'error')
            raise doc.root.text
          else
            raise "Unrecognized response from metacat at #{PATH_TO_METACAT}"
          end
        elsif(response.content_type == 'text/plain')
          response.read_body do |f|
            yield f
          end
        else
          raise "Unrecognized content type \"#{response.content_type}\" " +
                "from metacat at #{PATH_TO_METACAT}"
        end
      end
    elsif(location =~ /http/)
      uri = URI.parse(location)
      http = Net::HTTP.start(uri.host, uri.port)
      http.request_get(uri.to_s) do |response|
        response.read_body do |f|
          yield f
        end
      end
    else
      raise 'Unknown location for dataTable'
    end
  end 
end