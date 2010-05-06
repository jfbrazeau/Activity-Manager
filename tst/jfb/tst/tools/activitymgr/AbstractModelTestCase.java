package jfb.tst.tools.activitymgr;

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.ui.util.CfgMgr;
import jfb.tst.tools.activitymgr.core.TaskTest;

import org.apache.log4j.PropertyConfigurator;

import junit.framework.TestCase;

public abstract class AbstractModelTestCase extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(TaskTest.class);
	}

	protected void setUp() throws Exception {
		// Initialisation des logs et chargement de la config
		PropertyConfigurator.configure("cfg/log4j.properties");
		CfgMgr.load();

		// Initialisation de la connexion à la base de données
		String jdbcDriver = CfgMgr.get(CfgMgr.JDBC_DRIVER);
		String jdbcUrl = CfgMgr.get(CfgMgr.JDBC_URL);
		String jdbcUser = CfgMgr.get(CfgMgr.JDBC_USER);
		String jdbcPassword = CfgMgr.get(CfgMgr.JDBC_PASSWORD);
		ModelMgr.initDatabaseAccess(
				jdbcDriver,
				jdbcUrl,
				jdbcUser,
				jdbcPassword
			);

	}
	
	protected void tearDown() throws Exception {
	}

}
