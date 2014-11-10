package org.activitymgr.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.activitymgr.core.util.StringHelperTest;

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
		//$JUnit-END$
		return suite;
	}

}
