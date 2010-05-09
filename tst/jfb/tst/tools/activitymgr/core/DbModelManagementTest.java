package jfb.tst.tools.activitymgr.core;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

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
