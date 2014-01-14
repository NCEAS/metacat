package edu.ucsb.nceas.metacat.index.annotation;

import java.io.File;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;

public class TripleStoreService {

	private static TripleStoreService instance;
	
	private TripleStoreService() {}
	
	public static TripleStoreService getInstance() {
		if (instance == null) {
			instance = new TripleStoreService();
		}
		return instance;
	}
	
	public Dataset getDataset() {
		String directory = "./tdb";

    	// for testing, delete the triplestore each time
    	File dir = new File(directory);
//    	if (dir.exists()) {
//    		dir.delete();
//    	}
		Dataset dataset = TDBFactory.createDataset(directory);
		return dataset;
	}
}
