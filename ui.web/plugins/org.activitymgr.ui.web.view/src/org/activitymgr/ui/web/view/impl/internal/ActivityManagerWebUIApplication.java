package org.activitymgr.ui.web.view.impl.internal;

import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * This class controls all aspects of the application's execution
 */
public class ActivityManagerWebUIApplication implements IApplication {

	private CountDownLatch stopLatch;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		System.out.println("Activity Manager started");
		context.applicationRunning();
		stopLatch = new CountDownLatch(1);
		stopLatch.await();
		stopLatch = null;
		return IApplication.EXIT_OK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		System.out.println("Activity Manager stopped");
		if (stopLatch != null) {
			stopLatch.countDown();
		}
	}

}