package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;
import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.LogicModule;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.TaskChooserDialog;
import org.activitymgr.ui.web.view.impl.internal.util.CheckBoxView;
import org.activitymgr.ui.web.view.impl.internal.util.DownloadButtonView;
import org.activitymgr.ui.web.view.impl.internal.util.LabelView;
import org.activitymgr.ui.web.view.impl.internal.util.StandardButtonView;
import org.activitymgr.ui.web.view.impl.internal.util.TextFieldView;

import com.google.inject.AbstractModule;
import com.vaadin.ui.UI;

public class ViewModule extends AbstractModule {

	@Override
	protected void configure() {
		// Bind logic module
		install(new LogicModule());

		// Resource cache
		bind(IResourceCache.class).toInstance(new ResourceCacheImpl());
		
		// Bind Vaadin UI
		bind(UI.class).to(ActivityManagerUI.class);
		
		// Bind views
		bind(IAuthenticationLogic.View.class).to(AuthenticationPanel.class);
		bind(IContributionsTabLogic.View.class).to(ContributionsPanel.class);
		bind(ICollaboratorsTabLogic.View.class).to(CollaboratorsPanel.class);
		bind(ITasksTabLogic.View.class).to(TasksPanel.class);
		bind(ITaskChooserLogic.View.class).to(TaskChooserDialog.class);
		bind(ILabelLogic.View.class).to(LabelView.class);
		bind(ITextFieldLogic.View.class).to(TextFieldView.class);
		bind(ICheckBoxFieldLogic.View.class).to(CheckBoxView.class);
		bind(ITabFolderLogic.View.class).to(TabFolderViewImpl.class);
		bind(IStandardButtonLogic.View.class).to(StandardButtonView.class);
		bind(IDownloadButtonLogic.View.class).to(DownloadButtonView.class);
	}

}
