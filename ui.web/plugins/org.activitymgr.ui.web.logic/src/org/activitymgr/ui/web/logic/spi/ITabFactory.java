package org.activitymgr.ui.web.logic.spi;

import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITabLogic;

public interface ITabFactory {
	
	int getTabOrderPriority();
	
	String getTabId();

	ITabLogic<?> create(ITabFolderLogic parent);

}
