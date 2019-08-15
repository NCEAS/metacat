package edu.ucsb.nceas.metacat.index.annotation;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import org.dataone.cn.indexer.parser.ISolrDataField;
import org.dataone.cn.indexer.annotation.SparqlField;
import org.dataone.configuration.Settings;

public class OntologyModelService {
    private static Logger log = Logger.getLogger(OntologyModelService.class);

    private static OntologyModelService instance = null;

    private static OntModel ontModel = null;

    public static final String FIELD_ANNOTATION = "sem_annotation";

    private static List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();

    private static List<String> ontologyList = new ArrayList<String>();

    private static Map<String, String> altEntryList = new HashMap<String, String>();

    private OntologyModelService() {
    }

    public static OntologyModelService getInstance() {
        if (instance == null) {
            instance = new OntologyModelService();

            // Populate the ontology model with registered ontologies and alternate
            // entries (local cache locations)
            instance.init();
        }

        return instance;
    }

    private void init() {
        log.debug(OntologyModelService.class.getName() + " init() called");

        if (ontModel != null) {
            return;
        }

        ontModel = ModelFactory.createOntologyModel();

        // Add any configured local copies of ontologies
        loadAltEntries();

        // Safely try to read the ontologies from disk
        for (String ontologyUri : ontologyList) {
            try {
                log.debug("Loading ontology " + ontologyUri);
                ontModel.read(ontologyUri);

            } catch (Exception e) {
                log.debug("Failed to read " + ontologyUri);
                e.printStackTrace();
            }
        }
    }

    protected Map<String, Set<String>> expandConcepts(String uri) {
        log.debug("expandConcepts " + uri);
        Map<String, Set<String>> conceptFields = new HashMap<String, Set<String>>();

        if (uri == null || uri.length() < 1) {
            log.debug("Expansion failed because uri " + uri + " was either null or non-zero length.");

            return conceptFields;
        }

        log.debug("About to run through fieldList which is size " + fieldList.size());

        for (ISolrDataField field : fieldList) {
            String q = null;

            if (!(field instanceof SparqlField)) {
                continue;
            }

            q = ((SparqlField) field).getQuery();
            q = q.replaceAll("\\$CONCEPT_URI", uri);

            log.debug("SPARQL Query" + q.toString());
            Query query = QueryFactory.create(q);
            QueryExecution qexec = QueryExecutionFactory.create(query, ontModel);
            ResultSet results = qexec.execSelect();

            // each field might have multiple solution values
            String name = field.getName();
            Set<String> values = new HashSet<String>();

            while (results.hasNext()) {
                QuerySolution solution = results.next();
                log.debug("Solution SPARQL result: " + solution.toString());
                if (!solution.contains(name)) {
                    continue;
                }

                // Don't include blank nodes (anonymous)
                if (solution.get(field.getName()).isAnon()) {
                    continue;
                }

                String value = solution.get(name).toString();
                log.debug("Adding value " + value);
                values.add(value);
            }

            conceptFields.put(name, values);
        }

        // Debug
        for (Map.Entry<String, Set<String>> entry : conceptFields.entrySet()) {
            log.debug("entryKey: " + entry.getKey());
            Set<String> values = entry.getValue();

            for (String value : values) {
                log.debug("  " + value);
            }

        }
        return conceptFields;
    }

    public List<ISolrDataField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<ISolrDataField> flds) {
        fieldList = flds;
    }

    private List<String> getOntologyList() {
        return ontologyList;
    }

    public void setOntologyList(List<String> ontlist) {
        ontologyList = ontlist;
    }

    public Map<String, String> getAltEntryList() {
        return altEntryList;
    }

    public void setAltEntryList(Map<String, String> entryList) {
        altEntryList = entryList;
    }

    public void loadAltEntries() {
        log.debug("OntologyModelService - Loading altEntries of size " + altEntryList.size());

        OntDocumentManager ontManager = ontModel.getDocumentManager();

        // Ignore reading imports. This prevents Jena from going onto the network
        // to grab imported OWL files in OWL files contained in the altEntries
        // list. I did this for speed and for security.
        ontManager.setProcessImports(false);

        // This is a bit funky but I couldn't find a better way to do this
        // Jena needs to be given URIs with `file:` as the scheme and we need to provide Jena with
        // the location of the alt entries. To get the tests to pass, we use file:src/main/... but
        // when this webapp is deployed, we need Jena to see /var/lib/tomcat7/...
        // Here, we default to src/main... unless we can get the deployDir and context from Settings
        // and, if we do, we replace the values here

        String deployDir = null;
        String context = null;

        try {
            deployDir = Settings.getConfiguration().getString("application.deployDir");
        } catch (Exception e) {
            log.error("Failed to read configuration vlaues for application.deployDir");
        }

        try {
            context = Settings.getConfiguration().getString("index.context");
        } catch (Exception e) {
            log.error("Failed to read configuration vlaues for index.context");
        }

        for (Map.Entry<String, String> entry : altEntryList.entrySet()) {
            if (deployDir != null && context != null) {
                ontManager.addAltEntry(entry.getKey(), entry.getValue().replace("src/main/resources/", deployDir + "/" + context + "/" + "WEB-INF/classes/"));
            } else {
                ontManager.addAltEntry(entry.getKey(), entry.getValue());
            }        }
    }
}
