$:.unshift File.join(File.dirname(__FILE__), "..", "lib")
require "date"
require "rexml/document"
require "data_table.rb"

# == What is it
# The goal of this object is to encapsulate a dom representation of
# an EML(http://knb.ecoinformatics.org/software/eml) document and provide
# quick helper methods to access commonly needed attributes. These methods
# will return a more "ruby friendly" representation of this metadata.
# 
# At their core Eml objects contain a REXML::Document in the instance variable @doc.
# Until this object is feature-complete, this dom document can be used when this
# object is returned from this module's Metacat client.
# 
# ==   Examples
# ===  Get temporal coverage
#
#   metacat = Metacat.new('http://data.piscoweb.org/catalog/metacat')
#   eml_object = metacat.find(:docid => 'HMS001_020ADCP019R00_20060612.50.1')
#   geographic_coverage = eml_object.geographic_coverage
#   => [{ "latitude"=>-121.8996,
#         "longitude"=>36.6214,
#         "id"=>"HMS001",
#         "description"=>
#         "Hopkins Marine Station: HMS001: This inner-shelf mooring is located offshore 
#         of the city of Monterey, California, USA, near Hopkins Marine Station.  The 
#         mooring is located in an overall water depth of 020 meters (referenced to Mean 
#         Sea Level, MSL).  The altitudeMinimum and altitudeMaximum tags in this initial 
#         coverage section refer to the ADCP measurement range and are also referenced to 
#         MSL.  They do not represent the overall water depth.  Note the nominal range of 
#         the ADCP may extend from near-bottom (a depth expressed as a negative altitude) 
#         to slightly above MSL (a height expressed as a positive altitude)."}]
#
# ===  Get associated data table(DataTable) and write it to disk
#   eml_object.data_tables.each do |data_table|
#     file = File.new("./store/#{data_table.id}")
#     # data_table is an object, with method read
#     data_table.read do |buffer|
#       file.write(buffer)
#     end
#     file.close()
#   end
class Eml
  attr_reader :docid, :doc
  
  # Accepts an EML REXML::Document
  def initialize(metadata)
    if(metadata.class != REXML::Document || metadata.root.name != 'eml')
      raise ArgumentError, 'Must initialize with REXML::Document representation of EML metadata'
    else
      @doc = metadata
      @docid = @doc.root.attributes['packageId']
    end
  end
  
  def to_s
    @doc.to_s
  end
    
  def data_tables
    tables = []
    @doc.root.elements.each("dataset/dataTable") { |element|  
      tables.push(DataTable.new(element, self))
    }
    return tables
  end

  def largest_data_table
    if(data_tables.length == 1)
      return data_tables[0]
    else
      size = 0
      largest = nil
      data_tables.each do |data_table|
        if(data_table.size > size)
          size = data_table.size
          largest = data_table
        end
      end
      largest
    end
  end
  
  # Pulls a date range from the temporalCoverage element
  #
  # Note : EML supports multiple date ranges to account for gaps
  # this code just lumps them into one
  # Also, it does not support cases of singleDateTime
  def temporal_coverage
    beginDates = endDates = Array.new()
    path = "dataset/coverage/temporalCoverage/rangeOfDates"
    @doc.root.elements.each(path){ |range|  
      beginDates.push(
        Date.strptime(range.elements["beginDate"].elements[1].text)
      )
      endDates.push(
        Date.strptime(range.elements["endDate"].elements[1].text)
      )
    }
    return beginDates.min, endDates.max
  end
  
  def geographic_coverage
    sites = Array.new
    coverage.elements.each('geographicCoverage') do |g|
      site = {
        'id'          =>  g.attributes['id'],
        'description' =>  g.elements['geographicDescription'].text,
        'latitude'    =>  g.elements['boundingCoordinates/westBoundingCoordinate'].text.to_f,
        'longitude'   =>  g.elements['boundingCoordinates/northBoundingCoordinate'].text.to_f
      }
      sites << site
    end
    return sites
  end
  
  def coverage
    @doc.root.elements["dataset/coverage"]
  end
  
  def title
    @doc.root.elements["dataset/title"].text
  end
  
  def short_name
    @doc.root.elements["dataset/shortName"].text
  end
end