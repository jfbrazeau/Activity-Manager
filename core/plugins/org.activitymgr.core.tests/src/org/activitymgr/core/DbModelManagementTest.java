package org.activitymgr.core;

import org.activitymgr.core.beans.Duration;


public class DbModelManagementTest extends AbstractModelTestCase {

	public void test01_InitialTablesDontExist() throws DbException {
		assertFalse(getModelMgr().tablesExist());
	}

	public void test02_TablesCreation() throws DbException, ModelException {
		if (!getModelMgr().tablesExist()) {
			getModelMgr().createTables();
		}
	}

	public void test03_FinalTablesExist() throws DbException {
		assertTrue(getModelMgr().tablesExist());
	}
}
