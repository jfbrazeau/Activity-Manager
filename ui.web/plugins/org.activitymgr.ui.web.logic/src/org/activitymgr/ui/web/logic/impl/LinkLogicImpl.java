package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILinkLogic;

public class LinkLogicImpl extends AbstractLogicImpl<ILinkLogic.View> implements ILinkLogic {

	public LinkLogicImpl(AbstractLogicImpl<?> parent, String label, String href) {
		super(parent);
		getView().setLabel(label);
		getView().setHref(href);
	}

}
