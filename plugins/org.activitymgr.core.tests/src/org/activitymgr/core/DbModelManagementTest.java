package org.activitymgr.core;


public class DbModelManagementTest extends AbstractModelTestCase {

	public void test01_InitialTablesDontExist() throws DbException {
		assertEquals(false, ModelMgr.tablesExist());
	}

	public void test02_TablesCreation() throws DbException {
		//if (!ModelMgr.tablesExist()) {
			ModelMgr.createTables();
		//}
	}

	public void test03_FinalTablesExist() throws DbException {
		assertTrue(ModelMgr.tablesExist());
	}
}
