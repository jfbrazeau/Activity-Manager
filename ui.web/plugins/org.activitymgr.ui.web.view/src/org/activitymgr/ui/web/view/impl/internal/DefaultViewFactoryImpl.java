package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.view.impl.AbstractViewFactoryExtension;
import org.activitymgr.ui.web.view.impl.internal.dialogs.TaskChooserDialog;
import org.activitymgr.ui.web.view.impl.internal.util.LabelView;
import org.activitymgr.ui.web.view.impl.internal.util.TextFieldView;

import com.vaadin.ui.UI;

public class DefaultViewFactoryImpl extends AbstractViewFactoryExtension {

	@Override
	public IView<?> createView(Class<?> logicType, Object... parameters) {
		if (IRootLogic.class.isAssignableFrom(logicType)) {
			return getRootView();
		} else if (IAuthenticationLogic.class.isAssignableFrom(logicType)) {
			String defaultUser = (String) (parameters.length > 0 ? parameters[0] : null);
			return new AuthenticationPanel(getResourceCache(), defaultUser);
		} else if (IContributionsTabLogic.class.isAssignableFrom(logicType)) {
			return new ContributionsPanel(getResourceCache());
		} else if (ITaskChooserLogic.class.isAssignableFrom(logicType)) {
			TaskChooserDialog dialog = new TaskChooserDialog(getResourceCache());
			((UI)getRootView()).addWindow(dialog);
			return dialog;
		} else if (ILabelLogic.class.isAssignableFrom(logicType)) {
			return new LabelView();
		} else if (ITextFieldLogic.class.isAssignableFrom(logicType)) {
			return new TextFieldView();
		} else if (ITabFolderLogic.class.isAssignableFrom(logicType)) {
			return new TabFolderViewImpl();
		} else {
			return null;
		}
	}

	
}
