package edu.ucsb.nceas.metacat.shared;


/**
 * A suite of utility classes for querying DB
 * 
 */
public abstract class BaseService {
	protected String _serviceName = null;

	// package level method reporting if service is refreshable.  Basically,
	// we only want ServiceService calling this.
	public abstract boolean refreshable();
	
	// subclass must define doRefresh.  It is only called from the refresh() method.
	protected abstract void doRefresh() throws ServiceException;
	
	// package level method to refresh service.  We only want ServiceService 
	// calling this.
	public void refresh() throws ServiceException{
		if (refreshable()) {
			doRefresh();
		} else {
			throw new ServiceException("Service: " + _serviceName + " is not refreshable");
		}
	}
	
	public abstract void stop() throws ServiceException;

}
