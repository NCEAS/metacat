package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.metacat.admin.AdminException;

public interface UpgradeUtilityInterface {

	public boolean upgrade() throws AdminException;
}
