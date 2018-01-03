package org.activitymgr.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.activitymgr.core.report.ReportTest;
import org.activitymgr.core.report.XLSReportTest;
import org.activitymgr.core.util.StringHelperTest;
import org.activitymgr.core.xml.XmlTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.activitymgr");
		//$JUnit-BEGIN$
		suite.addTestSuite(StringHelperTest.class);
		suite.addTestSuite(DurationTest.class);
		suite.addTestSuite(CollaboratorsTest.class);
		suite.addTestSuite(TaskTest.class);
		suite.addTestSuite(ContributionTest.class);
		suite.addTestSuite(XmlTest.class);
		suite.addTestSuite(XlsTest.class);
		suite.addTestSuite(ReportTest.class);
		suite.addTestSuite(XLSReportTest.class);
		suite.addTestSuite(TaskCacheTest.class);
		//$JUnit-END$
		return suite;
	}

}
