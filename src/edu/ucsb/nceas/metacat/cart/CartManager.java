package edu.ucsb.nceas.metacat.cart;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.service.SessionService;


/**
 * Class to query data
 */
public class CartManager {

	private static Log log = LogFactory.getLog(CartManager.class);

	private String sessionId = null;
	private DocumentCart documentCart = null;
	
	/**
	 * empty constructor to initialize query
	 */
	public CartManager() {
		init();
	}
	
	public CartManager(String sessionId) {
		// initialize the necessary parts
		this.sessionId = sessionId;
		init();
	}
	
	private void init() {
		documentCart = SessionService.getInstance().getRegisteredSession(sessionId).getDocumentCart();
		if (documentCart == null) {
			documentCart = new DocumentCart();
		}
		
	}
	
	public void editCart(String operation, String[] docids, Map fields) {
		
		//for attribute fields
		if (operation.equalsIgnoreCase("addField")) {
			documentCart.addFields(fields);
		}
		if (operation.equalsIgnoreCase("removeField")) {
			String field = (String) fields.keySet().toArray()[0];
			documentCart.removeField(field);
		}
		if (operation.equalsIgnoreCase("addFields")) {
			documentCart.addFields(fields);
		}
		if (operation.equalsIgnoreCase("clearfields")) {
			documentCart.clearFields();
		}
		
		//for document ids
		if (operation.equalsIgnoreCase("clear")) {
			documentCart.clear();
		}
		if (docids != null) {
			for (int i=0; i < docids.length; i++) {
				if (operation.equalsIgnoreCase("add")) {
					documentCart.addDocument(docids[i], fields);
				}
				if (operation.equalsIgnoreCase("remove")) {
					documentCart.removeDocument(docids[i]);
				}
			}
		}
		
		SessionService.getInstance().getRegisteredSession(sessionId).setDocumentCart(documentCart);
	}

}
