package org.activitymgr.ui.web.logic.spi;

import org.activitymgr.ui.web.logic.IButtonLogic;
import org.activitymgr.ui.web.logic.ITabLogic;

public interface ITabButtonFactory<TABLOGIC extends ITabLogic<?>> {
	
	IButtonLogic<?> create(TABLOGIC parent);

}
