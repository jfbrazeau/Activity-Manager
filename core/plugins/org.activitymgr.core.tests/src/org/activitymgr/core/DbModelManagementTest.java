package org.activitymgr.core;


public class DbModelManagementTest extends AbstractModelTestCase {

	public void test01_InitialTablesDontExist() throws DbException {
		assertEquals(false, getModelMgr().tablesExist());
	}

	public void test02_TablesCreation() throws DbException {
		//if (!getModelMgr().tablesExist()) {
			getModelMgr().createTables();
		//}
	}

	public void test03_FinalTablesExist() throws DbException {
		assertTrue(getModelMgr().tablesExist());
	}
}
