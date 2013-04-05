package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.ModelMgr;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO externalize connection parameters 
		ModelMgr.initDatabaseAccess("com.mysql.jdbc.Driver",
				"jdbc:mysql://localhost:3306/taskmgr_db", "taskmgr_user",
				"secret");
//		ModelMgr.initDatabaseAccess("org.hsqldb.jdbcDriver",
//				"jdbc:hsqldb:file:activitymgr", "sa",
//				"");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
