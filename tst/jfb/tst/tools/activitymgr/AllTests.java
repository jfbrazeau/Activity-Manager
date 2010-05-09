package jfb.tst.tools.activitymgr;

import jfb.tst.tools.activitymgr.core.CollaboratorsTest;
import jfb.tst.tools.activitymgr.core.ContributionTest;
import jfb.tst.tools.activitymgr.core.DbModelManagementTest;
import jfb.tst.tools.activitymgr.core.DurationTest;
import jfb.tst.tools.activitymgr.core.TaskTest;
import jfb.tst.tools.activitymgr.core.XmlTest;
import jfb.tst.tools.activitymgr.core.util.StringHelperTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(AllTests.class);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for jfb.tst.tools.activitymgr");
		//$JUnit-BEGIN$
		suite.addTestSuite(DbModelManagementTest.class);
		suite.addTestSuite(StringHelperTest.class);
		suite.addTestSuite(DurationTest.class);
		suite.addTestSuite(CollaboratorsTest.class);
		suite.addTestSuite(TaskTest.class);
		suite.addTestSuite(ContributionTest.class);
		suite.addTestSuite(XmlTest.class);
		//$JUnit-END$
		return suite;
	}

}
