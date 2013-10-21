package org.activitymgr.ui.rcp;

import org.activitymgr.ui.rcp.util.SafeRunner;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(400, 300));
		configurer.setShowMenuBar(false);
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
	}
	
	@Override
	public boolean preWindowShellClose() {
		new SafeRunner() {
			protected Object runUnsafe() throws Exception {
				MainView mainView = (MainView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences()[0].getView(true);
				mainView.getModelMgr().closeDatabaseAccess();
				return null;
			}
		}.run(PlatformUI.getWorkbench().getDisplay().getActiveShell());
		return super.preWindowShellClose();
	}
}
