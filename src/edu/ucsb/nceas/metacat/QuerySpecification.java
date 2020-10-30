/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents a structured query, and can be
 *             constructed from an XML serialization conforming to
 *             pathquery.dtd. The printSQL() method can be used to print
 *             a SQL serialization of the query.
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacat;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
//import edu.ucsb.nceas.utilities.UtilException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import java.util.Iterator;

/**
 * A Class that represents a structured query, and can be constructed from an
 * XML serialization conforming to
 *
 * @see pathquery.dtd. The printSQL() method can be used to print a SQL
 *      serialization of the query.
 */
public class QuerySpecification extends DefaultHandler
{

    /** flag determining whether extended query terms are present */
    private boolean containsExtendedSQL = false;

    /** flag determining whether predicates are present */
    private boolean containsPredicates = false;

    /** Identifier for this query document */
    private String meta_file_id;

    /** Title of this query */
    private String queryTitle;

    /** List of document types to be returned using package back tracing */
    private Vector returnDocList;

    /** List of document types to be searched */
    private Vector filterDocList;

    /** List of fields to be returned in result set */
    private Vector returnFieldList;
    
    /** List of fields with "[" and "]" in result set. This is a subset of returnFieldList.
     *   If some of return fields have [,  those fields will be stored this vector (we have different query for those return fields */
    private Vector returnFieldListWithPredicates;

    /** List of users owning documents to be searched */
    private Vector ownerList;

    /** The root query group that contains the recursive query constraints */
    private QueryGroup query = null;
    
    /** A string buffer to stored normalized query (Sometimes, the query have 
     * a value like "&", it will cause problem in html transform). So we need a
     * normalized query xml string.
     */
    private StringBuffer xml = new StringBuffer();

    // Query data structures used temporarily during XML parsing
    private Stack elementStack;

    private Stack queryStack;

    private String currentValue;

    private String currentPathexpr;

    private String parserName = null;

    private String accNumberSeparator = null;

    private boolean percentageSearch = false;

    private String userName = null;

    private static final String PUBLIC = "public";

    private String[] group = null;

    public static final String ATTRIBUTESYMBOL = "@";

    public static final char PREDICATE_START = '[';

    public static final char PREDICATE_END = ']';

    //private boolean hasAttributeReturnField = false;

    //private Hashtable attributeReturnList = new Hashtable();

    //private int countAttributeReturnField = 0;

    private StringBuffer textBuffer = new StringBuffer();
    
   
    private static Log logMetacat = LogFactory.getLog(QuerySpecification.class);

    /**
     * construct an instance of the QuerySpecification class
     *
     * @param queryspec
     *            the XML representation of the query (should conform to
     *            pathquery.dtd) as a Reader
     * @param parserName
     *            the fully qualified name of a Java Class implementing the
     *            org.xml.sax.XMLReader interface
     */
    public QuerySpecification(Reader queryspec, String parserName,
            String accNumberSeparator) throws IOException
    {
        super();

        // Initialize the class variables
        returnDocList = new Vector();
        filterDocList = new Vector();
        elementStack = new Stack();
        queryStack = new Stack();
        returnFieldList = new Vector();
        returnFieldListWithPredicates = new Vector();
        ownerList = new Vector();
        this.parserName = parserName;
        this.accNumberSeparator = accNumberSeparator;

        // Initialize the parser and read the queryspec
        XMLReader parser = initializeParser();
        if (parser == null) {
        	logMetacat.error("QuerySpecification() - SAX parser not instantiated properly.");
        }
        try {
            parser.parse(new InputSource(queryspec));
        } catch (SAXException se) {
            logMetacat.error("QuerySpecification() - SAX error parsing data: " + se.getMessage());
        }
    }

    /**
     * construct an instance of the QuerySpecification class
     *
     * @param queryspec
     *            the XML representation of the query (should conform to
     *            pathquery.dtd) as a String
     * @param parserName
     *            the fully qualified name of a Java Class implementing the
     *            org.xml.sax.Parser interface
     */
    public QuerySpecification(String queryspec, String parserName,
            String accNumberSeparator) throws IOException
    {
        this(new StringReader(queryspec), parserName, accNumberSeparator);
    }

    /**
     * construct an instance of the QuerySpecification class which don't need
     * to parser a xml document
     *
     * @param accNumberSeparator
     *            the separator between doc version
     */
    public QuerySpecification(String accNumberSeparator) throws IOException
    {
        // Initialize the class variables
        returnDocList = new Vector();
        filterDocList = new Vector();
        elementStack = new Stack();
        queryStack = new Stack();
        returnFieldList = new Vector();
        returnFieldListWithPredicates = new Vector();
        ownerList = new Vector();
        this.accNumberSeparator = accNumberSeparator;
    }

    /**
     * Method to set user name
     *
     * @param myName
     *            the user name
     */
    public void setUserName(String myName)
    {
        //to lower case
        if (myName != null) {
            this.userName = myName.toLowerCase();
        } else {
            this.userName = myName;
        }
    }

    /**
     * Method to set user group
     *
     * @param myGroup
     *            the user group
     */
    public void setGroup(String[] myGroup)
    {
        this.group = myGroup;
    }

    /**
     * Method to indicate this query is a percentage search
     */
    public boolean isPercentageSearch()
    {
        return percentageSearch;
    }

    /*
     * Method to get owner query. If it is owner it has all permission
     */
    private String createOwnerQuery()
    {
        String ownerQuery = null;
        //if user is public, we don't need to run owner query
        if (userName != null && !userName.equalsIgnoreCase(PUBLIC))
        {
	        ownerQuery = "SELECT docid FROM xml_documents WHERE ";
	        if (userName != null && !userName.equals("")) {
	            ownerQuery = ownerQuery + "lower(user_owner) ='" + userName + "'";
	        }
        }
        logMetacat.info("QuerySpecification.createOwerQuery - OwnerQuery: " + ownerQuery);
        return ownerQuery;
    }

    /*
     * Method to create query for xml_access, this part is to get docid list
     * which have a allow rule for a given user
     */
    private String createAllowRuleQuery()
    {
        String allowQuery = null;
        String allowString = constructAllowString();
        allowQuery = "SELECT guid from xml_access  " +
        		"WHERE ( " + allowString;
        allowQuery = allowQuery + ")";
        logMetacat.info("QuerySpecification.createAllowRuleQuery - allow query is: " + allowQuery);
        return allowQuery;

    }

    /* Method to construct a allow rule string */
    private String constructAllowString()
    {
        String allowQuery = "";
        
       // add public
        allowQuery = "(lower(principal_name) = '" + PUBLIC
                + "'";
                
        // add user name
        if (userName != null && !userName.equals("") && !userName.equalsIgnoreCase(PUBLIC)) {
            allowQuery = allowQuery + "OR lower(principal_name) = '" + userName +"'";
                    
        }
        // add  group
        if (group != null) {
            for (int i = 0; i < group.length; i++) {
                String groupUint = group[i];
                if (groupUint != null && !groupUint.equals("")) {
                    groupUint = groupUint.toLowerCase();
                    allowQuery = allowQuery + " OR lower(principal_name) = '"
                            + groupUint + "'";
                }//if
            }//for
        }//if
        // add allow rule
        allowQuery = allowQuery + ") AND perm_type = 'allow'" + " AND permission > 3";
        logMetacat.info("QuerySpecification.constructAllowString - allow string is: " + allowQuery);
        return allowQuery;
    }

    /*
     * Method to create query for xml_access, this part is to get docid list
     * which have a deny rule and perm_order is allowFirst for a given user.
     * This means the user will be denied to read
     */
    private String createDenyRuleQuery()
    {
        String denyQuery = null;
        String denyString = constructDenyString();
        denyQuery = "SELECT guid from xml_access " +
        		"WHERE ( " + denyString;
        denyQuery = denyQuery + ") ";
        logMetacat.info("QuerySpecification.createDenyRuleQuery - denyquery is: " + denyQuery);
        return denyQuery;

    }

    /* Construct deny string */
    private String constructDenyString()
    {
        String denyQuery = "";
         
        // add public
        denyQuery = "(lower(principal_name) = '" + PUBLIC
                 + "'";
                 
         // add user name
         if (userName != null && !userName.equals("") && !userName.equalsIgnoreCase(PUBLIC)) {
        	 denyQuery = denyQuery + "OR lower(principal_name) = '" + userName +"'";
                     
         }
         // add  groups
         if (group != null) {
             for (int i = 0; i < group.length; i++) {
                 String groupUint = group[i];
                 if (groupUint != null && !groupUint.equals("")) {
                     groupUint = groupUint.toLowerCase();
                     denyQuery = denyQuery + " OR lower(principal_name) = '"
                             + groupUint + "'";
                 }//if
             }//for
         }//if
         // add deny rules
         denyQuery = denyQuery + ") AND perm_type = 'deny'" +  " AND perm_order ='allowFirst'" +" AND permission > 3";
         logMetacat.info("QuerySpecification.constructDenyString - deny string is: " + denyQuery);
         return denyQuery;
        
    }

    /**
     * Method to append a access control query to SQL. So in DBQuery class, we
     * can get docid from both user specified query and access control query.
     * We don't need to checking permission after we get the doclist. It will
     * be good to performance
     *
     */
    public String getAccessQuery()
    {
        String accessQuery = null;
        String owner = createOwnerQuery();
        String allow = createAllowRuleQuery();
        String deny = createDenyRuleQuery();

        if (owner != null)
        {
          accessQuery = " AND (xml_documents.docid IN (" + owner + ")";
          accessQuery = accessQuery + " OR (identifier.guid IN (" + allow + ")"
                + " AND identifier.guid NOT IN (" + deny + ")))";
        }
        else
        {
        	accessQuery = " AND (identifier.guid IN (" + allow + ")"
                + " AND identifier.guid NOT IN (" + deny + "))";
        }
        logMetacat.info("QuerySpecification.getAccessQuery - access query is: " + accessQuery);
        return accessQuery;
    }

    /**
     * Returns true if the parsed query contains and extended xml query (i.e.
     * there is at least one &lt;returnfield&gt; in the pathquery document)
     */
    public boolean containsExtendedSQL()
    {
        if (containsExtendedSQL) {
            return true;
        } else {
            return false;
        }
    }

  
    /**
     * Accessor method to return the identifier of this Query
     */
    public String getIdentifier()
    {
        return meta_file_id;
    }

    /**
     * method to set the identifier of this query
     */
    public void setIdentifier(String id)
    {
        this.meta_file_id = id;
    }

    /**
     * Accessor method to return the title of this Query
     */
    public String getQueryTitle()
    {
        return queryTitle;
    }

    /**
     * method to set the title of this query
     */
    public void setQueryTitle(String title)
    {
        this.queryTitle = title;
    }

    /**
     * Accessor method to return a vector of the return document types as
     * defined in the &lt;returndoctype&gt; tag in the pathquery dtd.
     */
    public Vector getReturnDocList()
    {
        return this.returnDocList;
    }

    /**
     * method to set the list of return docs of this query
     */
    public void setReturnDocList(Vector returnDocList)
    {
        this.returnDocList = returnDocList;
    }

    /**
     * Accessor method to return a vector of the filter doc types as defined in
     * the &lt;filterdoctype&gt; tag in the pathquery dtd.
     */
    public Vector getFilterDocList()
    {
        return this.filterDocList;
    }

    /**
     * method to set the list of filter docs of this query
     */
    public void setFilterDocList(Vector filterDocList)
    {
        this.filterDocList = filterDocList;
    }

    /**
     * Accessor method to return a vector of the extended return fields as
     * defined in the &lt;returnfield&gt; tag in the pathquery dtd.
     */
    public Vector getReturnFieldList()
    {
        return this.returnFieldList;
    }

    /**
     * method to set the list of fields to be returned by this query
     */
    public void setReturnFieldList(Vector returnFieldList)
    {
        this.returnFieldList = returnFieldList;
    }

    /**
     * Accessor method to return a vector of the owner fields as defined in the
     * &lt;owner&gt; tag in the pathquery dtd.
     */
    public Vector getOwnerList()
    {
        return this.ownerList;
    }

    /**
     * method to set the list of owners used to constrain this query
     */
    public void setOwnerList(Vector ownerList)
    {
        this.ownerList = ownerList;
    }

    /**
     * get the QueryGroup used to express query constraints
     */
    public QueryGroup getQueryGroup()
    {
        return query;
    }

    /**
     * set the querygroup
     */
    public void setQueryGroup(QueryGroup group)
    {
        query = group;
    }

    /**
     * set if this query sepcification has extendQuery(has return doc type or
     * not)
     */
    public void setContainsExtenedSQL(boolean hasExtenedQuery)
    {
        containsExtendedSQL = hasExtenedQuery;
    }

    /**
     * Set up the SAX parser for reading the XML serialized query
     */
    private XMLReader initializeParser()
    {
        XMLReader parser = null;

        // Set up the SAX document handlers for parsing
        try {

            // Get an instance of the parser
            parser = XMLReaderFactory.createXMLReader(parserName);

            // Set the ContentHandler to this instance
            parser.setContentHandler(this);

            // Set the error Handler to this instance
            parser.setErrorHandler(this);

        } catch (Exception e) {
            logMetacat.error("QuerySpecification.getAccessQuery - Error: " + e.getMessage());
        }

        return parser;
    }

    /**
     * callback method used by the SAX Parser when the start tag of an element
     * is detected. Used in this context to parse and store the query
     * information in class variables.
     */
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException
    {
        logMetacat.debug("QuerySpecification.startElement - start element " + localName);
        BasicNode currentNode = new BasicNode(localName);
        //write element name into xml buffer.
        xml.append("<");
        xml.append(localName);
        // add attributes to BasicNode here
        if (atts != null) {
            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                currentNode
                        .setAttribute(atts.getLocalName(i), atts.getValue(i));
                xml.append(" ");
                xml.append(atts.getLocalName(i));
                xml.append("=\"");
                xml.append(atts.getValue(i));
                xml.append("\"");
            }
        }
        xml.append(">");

        elementStack.push(currentNode);
        if (currentNode.getTagName().equals("querygroup")) {
            QueryGroup currentGroup = new QueryGroup(currentNode
                    .getAttribute("operator"));
            if (query == null) {
                query = currentGroup;
            } else {
                QueryGroup parentGroup = (QueryGroup) queryStack.peek();
                parentGroup.addChild(currentGroup);
            }
            queryStack.push(currentGroup);
        }
        logMetacat.debug("QuerySpecification.startElement - ending startElement " + localName);
    }

    /**
     * callback method used by the SAX Parser when the end tag of an element is
     * detected. Used in this context to parse and store the query information
     * in class variables.
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
    	 logMetacat.debug("QuerySpecification.endElement - endElement "+localName);
        BasicNode leaving = (BasicNode) elementStack.pop();
        if (leaving.getTagName().equals("queryterm")) {
            boolean isCaseSensitive = (new Boolean(leaving
                    .getAttribute("casesensitive"))).booleanValue();
            QueryTerm currentTerm = null;
            if (currentPathexpr == null) {
                currentTerm = new QueryTerm(isCaseSensitive, leaving
                        .getAttribute("searchmode"), currentValue);
            } else {
                currentTerm = new QueryTerm(isCaseSensitive, leaving
                        .getAttribute("searchmode"), currentValue,
                        currentPathexpr);
            }
            QueryGroup currentGroup = (QueryGroup) queryStack.peek();
            currentGroup.addChild(currentTerm);
            currentValue = null;
            currentPathexpr = null;
        } else if (leaving.getTagName().equals("querygroup")) {
            QueryGroup leavingGroup = (QueryGroup) queryStack.pop();
        } else if (leaving.getTagName().equals("meta_file_id")) {
              meta_file_id = textBuffer.toString().trim();
        } else if (leaving.getTagName().equals("querytitle")) {
              queryTitle = textBuffer.toString().trim();
        } else if (leaving.getTagName().equals("value")) {
              currentValue = textBuffer.toString().trim();
              currentValue = MetacatUtil.normalize(currentValue);
        } else if (leaving.getTagName().equals("pathexpr")) {
              currentPathexpr = textBuffer.toString().trim();
        } else if (leaving.getTagName().equals("returndoctype")) {
              returnDocList.add(textBuffer.toString().trim());
        } else if (leaving.getTagName().equals("filterdoctype")) {
              filterDocList.add(textBuffer.toString().trim());
        } else if (leaving.getTagName().equals("returnfield")) {
              handleReturnField(textBuffer.toString().trim());
        } else if (leaving.getTagName().equals("filterdoctype")) {
              filterDocList.add(textBuffer.toString().trim());
        } else if (leaving.getTagName().equals("owner")) {
              ownerList.add(textBuffer.toString().trim());
        }
        String normalizedXML = textBuffer.toString().trim();
        logMetacat.debug("QuerySpecification.endElement - before normalize: " + normalizedXML);
        normalizedXML =  MetacatUtil.normalize(normalizedXML);
        logMetacat.debug("QuerySpecification.endElement - after normalize " + normalizedXML);
        xml.append(normalizedXML);
        xml.append("</");
        xml.append(localName);
        xml.append(">");
        //rest textBuffer
        textBuffer = new StringBuffer();

    }
    
    /**
     * Gets normailized query string in xml format, which can be transformed
     * to html
     */
    public String getNormalizedXMLQuery()
    {
    	//System.out.println("normailized xml \n"+xml.toString());
    	return xml.toString();
    }
    

    /**
     * callback method used by the SAX Parser when the text sequences of an xml
     * stream are detected. Used in this context to parse and store the query
     * information in class variables.
     */
    public void characters(char ch[], int start, int length)
    {
      // buffer all text nodes for same element. This is for text was splited
      // into different nodes
      String text = new String(ch, start, length);
      logMetacat.debug("QuerySpecification.characters - the text in characters " + text);
      textBuffer.append(text);

    }

   /**
    * Method to handle return field. It will be callied in ecogrid part
    * @param inputString
    */
    public void handleReturnField(String inputString)
    {
        int attributePos = inputString.indexOf(ATTRIBUTESYMBOL);
        int predicateStart = -1;
        int predicateEnd;
        boolean hasPredicate = false;

        while (true)
        {
            predicateStart = inputString.indexOf(PREDICATE_START, predicateStart + 1);

            if (attributePos == -1)
                break;

            if (predicateStart == -1)
                break;

            hasPredicate = true;

            if (attributePos < predicateStart)
                break;

            predicateEnd = inputString.indexOf(PREDICATE_END, predicateStart);

            if (predicateEnd == -1)
            {
                logMetacat.warn("QuerySpecification.handleReturnField - Invalid path: " + inputString);
                return;
            }

            while (attributePos < predicateEnd)
            {
                attributePos = inputString.indexOf(ATTRIBUTESYMBOL, attributePos + 1);

                if (attributePos == -1)
                    break;
            }
        }

        if (hasPredicate)
        {
            containsPredicates = true;
            returnFieldListWithPredicates.add(inputString);
        }

        containsExtendedSQL = true;
   
        // no attribute value will be returned
        logMetacat.info("QuerySpecification.handleReturnField - there are no attributes in the XPATH statement" );
        returnFieldList.add(inputString);       
    }

    /**
     * create a SQL serialization of the query that this instance represents
     */
    public String printSQL(boolean useXMLIndex, List<Object> parameterValues)
    {

        StringBuffer self = new StringBuffer();
        StringBuffer queryString = new StringBuffer();

        queryString.append("SELECT xml_documents.docid, identifier.guid, docname, doctype, date_created, date_updated, xml_documents.rev ");
        queryString.append("FROM xml_documents, identifier ");
        queryString.append("WHERE xml_documents.docid = identifier.docid AND xml_documents.rev = identifier.rev AND");

        // Get the query from the QueryGroup and check
        // if no query has been returned
        String queryFromQueryGroup;
        // keep track of the values we add as prepared statement question marks (?)
        List<Object> groupValues = new ArrayList<Object>();
        if (query != null) {
        	queryFromQueryGroup = query.printSQL(useXMLIndex, groupValues);
        } else {
        	queryFromQueryGroup = "";
        }
        logMetacat.info("QuerySpecification.printSQL - Query : " + queryFromQueryGroup);
        
        if(!queryFromQueryGroup.trim().equals("")){
            self.append(" xml_documents.docid IN (");
            self.append(queryFromQueryGroup);
            self.append(") ");
            // add the parameter values
            parameterValues.addAll(groupValues);
        }

        // Add SQL to filter for doctypes requested in the query
        // This is an implicit OR for the list of doctypes. Only doctypes in
        // this
        // list will be searched if the tag is present
        if (!filterDocList.isEmpty()) {
            boolean firstdoctype = true;

            if (!self.toString().equals("")){
                self.append(" AND ");
            }
            self.append(" (");

            Enumeration en = filterDocList.elements();
            while (en.hasMoreElements()) {
                String currentDoctype = (String) en.nextElement();
                if (firstdoctype) {
                    firstdoctype = false;
                    self.append(" doctype = ?");
                } else {
                    self.append(" OR doctype = ?");
                }
                parameterValues.add(currentDoctype);

            }

            self.append(") ");
            
        }

        // Add SQL to filter for owners requested in the query
        // This is an implicit OR for the list of owners
        if (!ownerList.isEmpty()) {
            boolean first = true;

            if (!self.toString().equals("")){
                self.append(" AND ");
            }
            self.append(" (");
            

            Enumeration en = ownerList.elements();
            while (en.hasMoreElements()) {
                String current = (String) en.nextElement();
                if (current != null) {
                    current = current.toLowerCase();
                }
                if (first) {
                    first = false;
                    self.append(" lower(user_owner) = ?");
                } else {
                    self.append(" OR lower(user_owner) = ?");
                }
                parameterValues.add(current);
            }

            self.append(") ");
            
        }

        // if there is only one percentage search item, this query is a
        // percentage search query
        if (query != null) {
        	logMetacat.info("QuerySpecification.printSQL - percentage number: " + query.getPercentageSymbolCount());
			if (query.getPercentageSymbolCount() == 1) {
				logMetacat.info("QuerySpecification.printSQL - It is a percentage search");
				percentageSearch = true;
			}
        }

        queryString.append(self.toString());
        return queryString.toString();
    }

   

    /**
     * This method prints sql based upon the &lt;returnfield&gt; tag in the
     * pathquery document. This allows for customization of the returned fields.
     * If the boolean useXMLIndex paramter is false, it uses a recursive query on
     * xml_nodes to find the fields to be included by their path expression, and
     * avoids the use of the xml_index table.
     *
     * @param doclist the list of document ids to search
     * @param unaccessableNodePair the node pairs (start id and end id) which
     *            this user should not access
     * @param useXMLIndex a boolean flag indicating whether to search using
     *            xml_index
     */
    public String printExtendedSQL(String doclist, boolean useXMLIndex, List<Object> allValues, List<Object> docListValues)
    {
    	
    	// keep track of the values we add as prepared statement question marks (?)
    	//List<Object> allValues = new ArrayList<Object>();
    	
        if (useXMLIndex && !containsPredicates) {
        	// keep track of the values we add as prepared statement question marks (?)
        	List<Object> parameterValues = new ArrayList<Object>();
        	String query = printExtendedSQL(doclist, parameterValues, docListValues);
        	// add parameter values to our running list
        	allValues.addAll(parameterValues);
        	return query;
        }
        else
        {
            StringBuffer self = new StringBuffer();
            boolean firstfield = true;
            // keep track of the values we add as prepared statement question marks (?)
        	List<Object> parameterValues = new ArrayList<Object>();
            // first part comes from fields without  predicates 
            String queryFromWithoutPrecidates = printExtendedSQL(doclist, parameterValues, docListValues);
            // add parameter values to our running list
        	allValues.addAll(parameterValues);
        	if (queryFromWithoutPrecidates != null) {
            	 // it has return fields without predicate
            	 self.append(queryFromWithoutPrecidates);
            	 firstfield = false;
        	}
            //put the returnfields into the query
            //the for loop allows for multiple fields
            for (int i = 0; i <   returnFieldListWithPredicates.size(); i++)
            {
                if (firstfield)
                {
                    firstfield = false;
                }
                else
                {
                    self.append(" UNION ");
                }
                String path  = (String)  returnFieldListWithPredicates.elementAt(i);
                //path = path.replaceAll("'", "''");
                // TODO: can we use prepared statements for this?
                allValues.add(path);
                self.append("select xml_nodes.docid, ");
                self.append("? as path, ");
                self.append("xml_nodes.nodedata, ");
                self.append("xml_nodes.parentnodeid, ");
                self.append("xml_nodes.nodetype ");
                //self.append("from xml_nodes, xml_documents ");
                self.append("from xml_nodes ");
                self.append("where ");
                // keep track of the values we add as prepared statement question marks (?)
            	List<Object> nestedParameterValues = new ArrayList<Object>();
                String nestedQuery = QueryTerm.useNestedStatements(path, nestedParameterValues);
                self.append(nestedQuery);
                // add to the running total
                allValues.addAll(nestedParameterValues);

                self.append(" AND xml_nodes.docid in (");
                self.append(doclist);
                allValues.addAll(docListValues);

                if (returnFieldIsAttribute(path))
                {
                    self.append(")");
                }
                else
                {
                     self.append(") AND xml_nodes.nodetype = 'TEXT'");
                }
                //self.append(" AND xml_nodes.rootnodeid = xml_documents.rootnodeid");

                //addAccessRestrictionSQL(unaccessableNodePair, self);
            }

            return self.toString();
        }
    }
    
    /*
     * Determines the returnfield is an attribute of not. 
     * For given returnfield, this programm will cut the part of path after last slash.
     * If no slash in the path, the original string will be considered as last part.
     * If first character of last part is @ it will retrun true. 
     */
    private boolean returnFieldIsAttribute(String path)
    {
    	boolean isAttribute = false;
    	if (path != null)
    	{
    	    int slashIndex = path.lastIndexOf("/");
    	    if (slashIndex !=-1)
    	    {
    	    	// if there is slash in the path, path should be replace by the last part
    	    	path = path.substring(slashIndex+1);
    	    }
    	    logMetacat.debug("QuerySpecification.returnFieldIsAttribute - final path is " + path);
    	    // if first of character of path is @, the path is attribute
    	    if (path.charAt(0) == '@')
    	    {
    	    	logMetacat.debug("QuerySpecification.returnFieldIsAttribute - it is an attribute");
    	    	isAttribute = true;
    	    }
    	}
    	return isAttribute;
    }

    /**
     * This method prints sql based upon the &lt;returnfield&gt; tag in the
     * pathquery document. This allows for customization of the returned fields.
     * It uses the xml_index table and so assumes that this table has been
     * built.
     *
     * @param doclist the list of document ids to search
     * @param unaccessableNodePair the node pairs (start id and end id)
     *            which this user should not access
     */
    private String printExtendedSQL(String doclist, List<Object> values, List<Object> docListValues) {
    	
    	// keep track of the values we add as prepared statement question marks (?)
    	//List<Object> values = new ArrayList<Object>();
    	
        logMetacat.debug("QuerySpecification.printExtendedSQL - in printExtendedSQL");
        StringBuffer self = new StringBuffer();
        Vector<String> elementVector = new Vector<String>();
        Vector<String> attributeVector = new Vector<String>();

        boolean usePathIndex = true;

        // test if the are elements in the return fields
        if ( returnFieldList.size() == 0 ) {
            return null;
        }

        for (int i = 0; i < returnFieldList.size(); i++) {
        	String path = (String)returnFieldList.elementAt(i);
        	// Since return fileds having preicates will be handle in another path,
        	// we should skip it.
        	if (returnFieldListWithPredicates.contains(path)) {
        		continue;
        	}
        	
        	if (path != null && path.indexOf(ATTRIBUTESYMBOL) != -1) {
        		attributeVector.add(path);
        	} else {
        		elementVector.add(path);
        	} 
        	

        	try {
				if (!SystemUtil.getPathsForIndexing().contains(path)) {
					usePathIndex = false;   
				}
			} catch (MetacatUtilException mue) {
				logMetacat.warn("QuerySpecification.printExtendedSQL - Could not get index paths: "  + mue.getMessage());
			}
         
        }
        // check if has return field
        if (elementVector.size() == 0 && attributeVector.size()==0)
        {
        	return null;
        }

        if (usePathIndex){
            self.append("select docid, path, nodedata, parentnodeid, null as nodetype ");
            self.append("from xml_path_index where path in ( ");

            boolean firstfield = true;
            //put the returnfields into the query
            //the for loop allows for multiple fields
            for (int i = 0; i < returnFieldList.size(); i++) {
            	String returnField = (String) returnFieldList.elementAt(i);
            	// in case we have predicate conditions with quotes
            	returnField = returnField.replaceAll("'", "''");
                if (firstfield) {
                    firstfield = false;
                    self.append("? ");
                	values.add(returnField);
                }
                else {
                    self.append(", ? ");
                    values.add(returnField);
                }
            }
            self.append(") AND docid in (");
            self.append(doclist);
            values.addAll(docListValues);
            self.append(")");

        } else {
            self.append("select xml_nodes.docid, xml_index.path, xml_nodes.nodedata,  ");
            self.append("xml_nodes.parentnodeid, ");
            self.append("xml_nodes.nodetype ");
            self.append("FROM xml_index, xml_nodes WHERE (");
           
            boolean firstElement = true;
            boolean firstAttribute = true;
            //put the returnfields into the query
            //the for loop allows for multiple fields
            if (elementVector.size() != 0)
            {
	            for (int i = 0; i < elementVector.size(); i++) {
	            	String path = (String) elementVector.elementAt(i);
	                if (firstElement) {
	                	firstElement = false;
	                	self.append(" (xml_index.nodeid=xml_nodes.parentnodeid AND xml_index.path IN ( ");
	                    self.append("?");
	                    values.add(path);
	                 }
	                else 
	                {
	                    self.append(", ? ");
	                    values.add(path);
	                }
	            }
	            self.append(") AND xml_nodes.nodetype = 'TEXT')");
            }
            
            if (attributeVector.size() != 0)
            {
            	for (int j=0; j<attributeVector.size(); j++)
            	{
            		String path = (String) attributeVector.elementAt(j);
            		if (firstAttribute)
            		{
            			firstAttribute = false;
            			if (!firstElement)
                		{
                			self.append(" OR ");
                		}
            			self.append(" (xml_index.nodeid=xml_nodes.nodeid AND ( xml_index.path IN ( ");
	                    self.append("?");
	                    values.add(path);
            		}
            		else 
	                {
	                    self.append(", ? ");
	                    values.add(path);
	                }
            	}
            	self.append(") AND xml_nodes.nodetype = 'ATTRIBUTE'))");
            }
            
          
            self.append(") AND xml_nodes.docid in (");
            self.append(doclist);
            values.addAll(docListValues);
            self.append(")");

        }

        return self.toString();
    }


    /**
     * Method to return a String generated after sorting the returnFieldList
     * Vector
     */
    public String getSortedReturnFieldString(){
        String returnFields = "";

        // Create a temporary vector and copy returnFieldList into it
        Vector tempVector = new Vector();

        Iterator it = returnFieldList.iterator();
        while(it.hasNext()){
            tempVector.add(it.next());
        }

        /*Enumeration attEnum = attributeReturnList.elements();
        while(attEnum.hasMoreElements()){
            Iterator tempIt = ((Vector)attEnum.nextElement()).iterator();
	    String rfield = "";
            if(tempIt.hasNext()){
		String element = (String)tempIt.next();
		if(element != null) {
		    rfield +=element;
		}
	    }
            if(tempIt.hasNext()){
		String attribute = (String)tempIt.next();
		if(attribute != null) {
  		    rfield = rfield + "@" + attribute;
                }
	    }
            tempVector.add(rfield);
        }*/

        // Sort the temporary vector
        java.util.Collections.sort(tempVector);

        // Generate the string and return it
        it = tempVector.iterator();
        while(it.hasNext()){
            returnFields = returnFields + it.next() + "|";
        }
        return returnFields;
    }


  


    public static String printRelationSQL(String docid)
    {
        StringBuffer self = new StringBuffer();
        self.append("select subject, relationship, object, subdoctype, ");
        self.append("objdoctype from xml_relation ");
        self.append("where docid like '").append(docid).append("'");
        return self.toString();
    }

    public static String printGetDocByDoctypeSQL(String docid)
    {
        StringBuffer self = new StringBuffer();

        self.append("SELECT docid,docname,doctype,");
        self.append("date_created, date_updated ");
        self.append("FROM xml_documents WHERE docid IN (");
        self.append(docid).append(")");
        return self.toString();
    }

    /**
     * create a String description of the query that this instance represents.
     * This should become a way to get the XML serialization of the query.
     */
    public String toString()
    {
        return "meta_file_id=" + meta_file_id + "\n" + query;
        //DOCTITLE attr cleared from the db
        //return "meta_file_id=" + meta_file_id + "\n" +
        //"querytitle=" + querytitle + "\n" + query;
    }

    /** A method to get rid of attribute part in path expression */
    public static String newPathExpressionWithOutAttribute(String pathExpression)
    {
        if (pathExpression == null) { return null; }
        int index = pathExpression.lastIndexOf(ATTRIBUTESYMBOL);
        String newExpression = null;
        if (index != 0) {
            newExpression = pathExpression.substring(0, index - 1);
        }
        logMetacat.info("QuerySpecification.newPathExpressionWithOutAttribute - The path expression without attributes: "
                + newExpression);
        return newExpression;
    }

    /** A method to get attribute name from path */
    public static String getAttributeName(String path)
    {
        if (path == null) { return null; }
        int index = path.lastIndexOf(ATTRIBUTESYMBOL);
        int size = path.length();
        String attributeName = null;
        if (index != 1) {
            attributeName = path.substring(index + 1, size);
        }
        logMetacat.info("QuerySpecification.getAttributeName - The attirbute name from path: " + attributeName);
        return attributeName;
    }

}
