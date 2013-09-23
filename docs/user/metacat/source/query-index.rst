.. raw:: latex

  \newpage


Metacat Indexing
===========================
Metacat v2.1 introduces support for building a SOLR index of Metacat content.
While we continue to support the "pathquery" search mechanism, this will be phased out 
in favor of the more efficient SOLR query interface.


Metacat deployments that opt to use the Metacat SOLR index will be able to take advantage 
of:

* fast search performance
* built-in paging features
* customizable return formats (for advanced admins)

Indexed documents and fields
-----------------------------
Metacat integrates the existing DataONE index library which includes many common metadata formats
out-of-the-box:

1. EML
2. FGDC
3. Dryad*


Default indexed fields
-----------------------
For a complete listing of the indexed fields, please see the DataONE documentation.

http://mule1.dataone.org/ArchitectureDocs-current/design/SearchMetadata.html

Metacat also reports on the currently-indexed fields, simply navigate to:

http://mule1.dataone.org/ArchitectureDocs-current/apis/MN_APIs.html#MNQuery.getQueryEngineDescription

with "solr" as the engine.

Index configuration
----------------------------
Metacat-index is deployed as a separate web application (metacat-index.war) and should be deployed 
as a sibling of the Metacat webapp (metacat.war). Deploying metacat-index.war is only required when SOLR support
is desired and can safely be omitted if it will not be utilized for any given Metacat deployment.

During the initial installation/upgrade, an empty index will be initialized in the configured "solr-home" location.
Metacat-index will index all the existing Metacat content when the webapp next initializes.
Note: the configured solr-home directory should not exist before configuring Metacat with indexing for the first time, 
otherwise the blank index will not be created for metacat-index to utilize.

Additional advanced configuration options are available in the metacat.properties file (shared between Metacat and Metacat-index).


Adding additional document types and fields
--------------------------------------------
TBD: Step-by-step guide for adding new documents and indexed fields.


Querying the index
--------------------
The SOLR index can be queried using standard SOLR syntax and return options. 
The DataONE query interface exposes the SOLR query engine.

http://mule1.dataone.org/ArchitectureDocs-current/apis/MN_APIs.html#MNQuery.query

Please see the SOLR documentation for examples and exhaustive syntax information.

http://lucene.apache.org/solr/


Access Policy enforcement
-------------------------
Access control is enforced by the index such that only records that are readable by the 
user performing the query are returned to the user. Any SOLR query submitted will be 
augmented with access control criteria corresponding to if and how the user is currently 
authenticated. Both certificate-based (DataONE API) and JSESSIONID-based (Metacat API) 
authentication are simultaneously supported.


Regenerating the index from scratch
-----------------------------------
When the SOLR index has been drastically modified, a complete regeneration of the 
index may be necessary. In order to accomplish this:

Step-by-step instructions:

1. Entirely remove the solr-home directory
2. Step through the Metacat admin interface main properties screen, specifying the solr-home directory you wish to use
3. Restart the webapp container (Tomcat).

Content can also be submitted for index regeneration by using the the Metacat API:

1. Login as the Metacat administrator
2. Navigate to: <host>/<metacat_context>/metacat?action=reindex[&pid={pid}]
3. If the pid parameter is omitted, all objects in Metacat will be submitted for reindexing.



Class design overview
----------------------

.. figure:: images/indexing-class-diagram.png

   Figure 1. Class design overview.
   
..
  @startuml images/indexing-class-diagram.png
  
	package "Current cn-index-processor (library)" {
	
		interface IDocumentSubprocessor {
			+ boolean canProcess(Document doc)
			+ initExpression(XPath xpath)
			+ Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs, Document doc)
		}
		class AbstractDocumentSubprocessor {
			- List<SolrField> fields
			+ setMatchDocument(String matchDocument)
			+ setFieldList(List<SolrField> fieldList) 
		}
		class ResourceMapSubprocessor {
		}
		class ScienceMetadataDocumentSubprocessor {
		}
			  
		interface ISolrField {
			+ initExpression(XPath xpathObject)
			+ List<SolrElementField> getFields(Document doc, String identifier)
		}
		class SolrField {
			- String name
			- String xpath
			- boolean multivalue
		}
		class CommonRootSolrField {
		}
		class RootElement {
		}
		class LeafElement {
		}
		class FullTextSolrField {
		}
		class MergeSolrField {
		}
		class ResolveSolrField {
		}
		class SolrFieldResourceMap {
		}
		
		class SolrDoc {
		      - List<SolrElementField> fieldList
		}
		
		class SolrElementField {
		      - String name
		      - String value
		}
		    
	}
	
	IDocumentSubprocessor <|-- AbstractDocumentSubprocessor
	AbstractDocumentSubprocessor <|-- ResourceMapSubprocessor
	AbstractDocumentSubprocessor <|-- ScienceMetadataDocumentSubprocessor

	ISolrField <|-- SolrField
	SolrField <|-- CommonRootSolrField
	CommonRootSolrField o--"1" RootElement
	RootElement o--"*" LeafElement
	SolrField <|-- FullTextSolrField
	SolrField <|-- MergeSolrField
	SolrField <|-- ResolveSolrField			
	SolrField <|-- SolrFieldResourceMap
	
	AbstractDocumentSubprocessor o--"*" ISolrField
	
	IDocumentSubprocessor --> SolrDoc
	
	SolrDoc o--"*" SolrElementField
	
	package "SOLR (library)" {
          
        abstract class SolrServer {
            + add(SolrInputDocument doc)
            + deleteByQuery(String id)
            + query(SolrQuery query)
        }
        class EmbeddedSolrServer {
        }
        class HttpSolrServer {
        }
    
    }
    
    SolrServer <|-- EmbeddedSolrServer
    SolrServer <|-- HttpSolrServer
	
	package "Metact-index (webapp)" {
		  
		class ApplicationController {
		    - List<SolrIndex> solrIndex
		    + regenerateIndex()
		}
		
		class SolrIndex {
			- List<IDocumentSubprocessor> subprocessors
			- SolrServer solrServer
			+ insert(String pid, InputStream data)
			+ update(String pid, InputStream data)
			+ remove(String pid)
		}

		class SystemMetadataEventListener {
			- SolrIndex solrIndex
			+ itemAdded(ItemEvent<SystemMetadata>)
			+ itemRemoved(ItemEvent<SystemMetadata>)
		}
	
	}
	
	package "Metacat (webapp)" {
		  
		class MetacatSolrIndex {
			- SolrServer solrServer
			+ InputStream query(SolrQuery)
		}
		
		class HazelcastService {
			- IMap hzIndexQueue
			- IMap hzSystemMetadata
			- IMap hzObjectPath
		}
		
	}
	
	MetacatSolrIndex o--"1" SolrServer
	HazelcastService .. SystemMetadataEventListener
	
	ApplicationController o--"*" SolrIndex
	SolrIndex o--"1" SolrServer	
	SolrIndex "1"--o SystemMetadataEventListener
	SolrIndex o--"*" IDocumentSubprocessor: Assembled using Spring bean configuration
	
	
	
  
  @enduml