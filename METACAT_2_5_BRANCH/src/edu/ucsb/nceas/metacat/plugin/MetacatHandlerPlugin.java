package edu.ucsb.nceas.metacat.plugin;

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.metacat.shared.HandlerException;

public interface MetacatHandlerPlugin {
	
	public boolean handlesAction(String action);
	
	public boolean handleAction(
			String action, 
			Hashtable<String, String[]> params,
            HttpServletRequest request, 
            HttpServletResponse response,
            String username, 
            String[] groups,
            String sessionId) throws HandlerException;

}
