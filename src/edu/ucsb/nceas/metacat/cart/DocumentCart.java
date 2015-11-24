/**
 *  '$RCSfile$'
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 * Author: Benjamin Leinfelder 
 * '$Date:  $'
 * '$Revision:  $'
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
package edu.ucsb.nceas.metacat.cart;

import java.util.HashMap;
import java.util.Map;

public class DocumentCart {

	
	private Map docids = new HashMap();
	
	private Map fields = new HashMap();
	
	public void addDocument(String docid, Map fields) {
		docids.put(docid, fields);
	}
	
	public void removeDocument(String docid) {
		docids.remove(docid);
	}
	
	public Map getFields(String docid) {
		return (Map) docids.get(docid);
	}
	
	public String[] getDocids() {
		return (String[]) docids.keySet().toArray(new String[0]);
	}
	
	public void clear() {
		docids.clear();
	}
	
	//for accessing the metadata attribute field mappings
	public void addField(String label, String path) {
		fields.put(label, path);
	}
	
	public void removeField(String label) {
		fields.remove(label);
	}
	
	public void clearFields() {
		fields.clear();
	}
	
	public Map getFields() {
		return fields;
	}
	
	public void addFields(Map f) {
		fields.putAll(f);
	}
	
	public String[] getLabels() {
		return (String[]) fields.keySet().toArray(new String[0]);
	}
	
	public String[] getPaths() {
		return (String[]) fields.values().toArray(new String[0]);
	}
}
