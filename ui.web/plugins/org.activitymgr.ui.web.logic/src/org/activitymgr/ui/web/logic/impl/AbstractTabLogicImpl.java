package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ITabLogic;

public abstract class AbstractTabLogicImpl<VIEW extends ITabLogic.View<?>> extends AbstractLogicImpl<VIEW> implements ITabLogic<VIEW> {

	public AbstractTabLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
	}

}
