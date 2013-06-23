package org.activitymgr.core;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class AllTestsWithLogger extends AllTests {

	public static void main(String[] args) {
		TestResult result= new TestResult();
		AllTestListener testListener = new AllTestListener();
		result.addListener(testListener);
		long startTime= System.currentTimeMillis();
		TestSuite suite = (TestSuite) suite();
		suite.run(result);
		long endTime= System.currentTimeMillis();
		long runTime= endTime-startTime;
		System.out.println("Test suite terminated.");
		System.out.println("  time elapsed :" + runTime);
		System.out.println("  nb tests     :" + (testListener.getOk() + testListener.getKo()));
		System.out.println("  tests ok     :" + testListener.getOk());
		System.out.println("  tests ko     :" + testListener.getKo());
	}

}

class AllTestListener implements TestListener {
	boolean testFailed = false;
	int ok;
	int ko;
	public void addError(Test test, Throwable t) {
		log("  ERROR : " + ((TestCase)test).getName() + ", t=" + t);
		testFailed = true;
	}
	public void addFailure(Test test, AssertionFailedError t) {
		addError(test, t);
	}
	public void endTest(Test test) {
		if (testFailed)
			ko++;
		else
			ok++;
		log("  TEST STATUS :" + (testFailed ? "FAILURE" : "SUCCESS"));
	}
	public void startTest(Test test) {
		log("BEGIN TEST '" + ((TestCase)test).getName() + "'");
		// Réinitialisation
		testFailed = false;
	}
	private void log(String s) {
		System.out.println(s);
	}
	/**
	 * @return Returns the ok.
	 */
	public int getOk() {
		return ok;
	}
	/**
	 * @return Returns the ko.
	 */
	public int getKo() {
		return ko;
	}
	
}