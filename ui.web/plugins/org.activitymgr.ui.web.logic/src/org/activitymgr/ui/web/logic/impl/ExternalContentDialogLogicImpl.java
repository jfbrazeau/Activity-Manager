package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IExternalContentDialogLogic;
import org.activitymgr.ui.web.logic.IExternalContentDialogLogic.View;
import org.activitymgr.ui.web.logic.ILogic;

public class ExternalContentDialogLogicImpl extends AbstractLogicImpl<View>
		implements IExternalContentDialogLogic {

	public ExternalContentDialogLogicImpl(ILogic<?> parent, String title,
			String url) {
		super(parent);
		getView().setTitle(title);
		getView().setContentUrl(url);
	}

}
