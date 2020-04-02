/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.doi.datacite.relation;

import java.io.InputStream;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.vocabulary.DC_TERMS;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
 * A class to extract provenance relationship from a resource map
 * @author tao
 *
 */
public class ProvenanceRelationHandler implements RelationshipHandler {
    
    public final static String PROVNAMESPACE = "http://www.w3.org/ns/prov#";
    public final static String ISDERIVEDFROM = "IsDerivedFrom";
    public final static String ISOURCEOF = "IsSourceOf";
    public final static String WASDERIVEDFROM = "wasDerivedFrom";
    public final static String USED = "used";
    private static Log log = LogFactory.getLog(ProvenanceRelationHandler.class);
    
    private Model model = ModelFactory.createDefaultModel();
    /**
     * Constructor
     * @param resourceMap  the input stream of the resource map which needs to be parsed
     */
    public ProvenanceRelationHandler(InputStream resourceMap) {
        model.read(resourceMap, null);
    }
    
    /**
     * Method to get the relationship. Now we only handle IsDerivedFrom/IsSourceOf
     * @param identifier  the subject of the triple relationship
     * @return a vector of triple statements
     */
    public Vector<Statement> getRelationship(String identifier) {
        Vector<Statement> statementList = new Vector<Statement>();
        //Get the resource object whose id is the identifier
        Resource resource = null;
        //create a identifier property (statement)
        Property identifierPred = DC_TERMS.identifier;
        Literal identifierObj = ResourceFactory.createPlainLiteral(identifier);
        Selector selector = new SimpleSelector(resource, identifierPred, identifierObj);
        StmtIterator iterator = model.listStatements(selector);
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            resource = statement.getSubject();
            if (resource != null) {
                //find the resource and jump out off the loop
                break;
            }
        }
        
        //Get the object of predication wasDerivedFrom
        Property derivedFromPredic = ResourceFactory.createProperty(PROVNAMESPACE, WASDERIVEDFROM);
        Property isDerivedFromProperty = ResourceFactory.createProperty(null, ISDERIVEDFROM);
        RDFNode nullNode = null;
        selector = new SimpleSelector(resource, derivedFromPredic, nullNode);
        iterator = model.listStatements(selector);
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            RDFNode object = statement.getObject();
            if (object.isResource()) {
               //Get the identifier of the object resource
               Resource objectResource = (Resource) object;
               Selector selector2 = new SimpleSelector(objectResource, identifierPred, nullNode);
               StmtIterator iterator2 = model.listStatements(selector2);
               RDFNode node2 = null;
               Literal idLiteral = null;
               while (iterator2.hasNext()) {
                   Statement statement2 = iterator2.nextStatement();
                   node2 = statement2.getObject();
                   if (node2.isLiteral()) {
                       idLiteral = (Literal) node2;
                       break;
                   }
               }
               if (idLiteral != null) {
                   //create a new statement with the new predication isDerivedFrom and add it into the return list
                   Statement newStatement = ResourceFactory.createStatement(resource, isDerivedFromProperty, idLiteral);
                   statementList.add(newStatement);
               }
            }
        }
        return statementList;
    }

}
