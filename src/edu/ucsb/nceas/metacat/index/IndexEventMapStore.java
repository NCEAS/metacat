/**
 *  '$RCSfile$'
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Leinfelder
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
package edu.ucsb.nceas.metacat.index;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;

public class IndexEventMapStore implements MapStore<Identifier, IndexEvent>, MapLoader<Identifier, IndexEvent> {

	private Log logMetacat = LogFactory.getLog(IndexEventMapStore.class);

	/**
	 * The map store/loader methods
	 */
	
	@Override
	public IndexEvent load(Identifier identifier) {
		try {
			return IndexEventDAO.getInstance().get(identifier);
		} catch (SQLException e) {
			logMetacat.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public Map<Identifier, IndexEvent> loadAll(Collection<Identifier> identifiers) {
		Map<Identifier, IndexEvent> eventMap = new TreeMap<Identifier, IndexEvent>();
		for (Identifier identifier: identifiers) {
			IndexEvent event = null;
			try {
				event = IndexEventDAO.getInstance().get(identifier);
				eventMap.put(identifier, event);
			} catch (SQLException e) {
				logMetacat.error(e.getMessage(), e);
			}
		}
		return eventMap;
	}

	@Override
	public Set<Identifier> loadAllKeys() {
		try {
			return IndexEventDAO.getInstance().getAllIdentifiers();
		} catch (SQLException e) {
			logMetacat.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public void delete(Identifier identifier) {
		try {
			IndexEventDAO.getInstance().remove(identifier);
		} catch (SQLException e) {
			logMetacat.error(e.getMessage(), e);
		}		
	}

	@Override
	public void deleteAll(Collection<Identifier> identifiers) {
		for (Identifier identifier: identifiers) {
			try {
				IndexEventDAO.getInstance().remove(identifier);
			} catch (SQLException e) {
				logMetacat.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void store(Identifier identifier, IndexEvent event) {
		try {
			IndexEventDAO.getInstance().add(event);
		} catch (SQLException e) {
			logMetacat.error(e.getMessage(), e);
		}		
	}

	@Override
	public void storeAll(Map<Identifier, IndexEvent> indexEventMap) {
		for (IndexEvent event: indexEventMap.values()) {
			try {
				IndexEventDAO.getInstance().add(event);
			} catch (SQLException e) {
				logMetacat.error(e.getMessage(), e);
			}
		}
	}
	
}
