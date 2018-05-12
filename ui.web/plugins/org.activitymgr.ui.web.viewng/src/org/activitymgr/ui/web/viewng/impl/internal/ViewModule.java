package org.activitymgr.ui.web.viewng.impl.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;
import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.IContributionTaskChooserLogic;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IExternalContentDialogLogic;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILinkLogic;
import org.activitymgr.ui.web.logic.IReportsLogic;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ISelectFieldLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic;
import org.activitymgr.ui.web.logic.LogicModule;
import org.activitymgr.ui.web.viewng.IResourceCache;
import org.activitymgr.ui.web.viewng.impl.internal.util.CheckBoxView;
import org.activitymgr.ui.web.viewng.impl.internal.util.StandardButtonView;
import org.activitymgr.ui.web.viewng.impl.internal.util.TextFieldView;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;

public class ViewModule extends AbstractModule {

	@Override
	protected void configure() {
		// Bind logic module
		install(new LogicModule());

		// Resource cache
		bind(IResourceCache.class).toInstance(new ResourceCacheImpl());

		// Bind views
		bind(IAuthenticationLogic.View.class).to(AuthenticationPanel.class);
		bind(IContributionsTabLogic.View.class).to(ContributionsPanel.class);
		bind(ICollaboratorsTabLogic.View.class).to(CollaboratorsPanel.class);
		bind(ITasksTabLogic.View.class).to(TasksPanel.class);
		bind(IReportsTabLogic.View.class).to(ReportsTabPanel.class);
		// bind(IReportsLogic.View.class).to(ReportsPanel.class);
		bindFakeViewImpl(IReportsLogic.View.class);
		// bind(ITaskChooserLogic.View.class).to(TaskChooserDialog.class);
		bindFakeViewImpl(ITaskChooserLogic.View.class);
		// bind(IContributionTaskChooserLogic.View.class).to(
		// ContributionTaskChooserDialog.class);
		bindFakeViewImpl(IContributionTaskChooserLogic.View.class);
		// bind(ILabelLogic.View.class).to(LabelView.class);
		bindFakeViewImpl(ILabelLogic.View.class);
		// bind(ILinkLogic.View.class).to(LinkView.class);
		bindFakeViewImpl(ILinkLogic.View.class);
		bind(ITextFieldLogic.View.class).to(TextFieldView.class);
		bind(ICheckBoxFieldLogic.View.class).to(CheckBoxView.class);
		bind(ITabFolderLogic.View.class).to(TabFolderViewImpl.class);
		bind(IStandardButtonLogic.View.class).to(StandardButtonView.class);
		// bind(IDownloadButtonLogic.View.class).to(DownloadButtonView.class);
		bindFakeViewImpl(IDownloadButtonLogic.View.class);
		// bind(ISelectFieldLogic.View.class).to(SelectFieldView.class);
		bindFakeViewImpl(ISelectFieldLogic.View.class);
		// bind(ITwinSelectFieldLogic.View.class).to(TwinSelectView.class);
		bindFakeViewImpl(ITwinSelectFieldLogic.View.class);
		// bind(IExternalContentDialogLogic.View.class).to(
		// ExternalContentDialog.class);
		bindFakeViewImpl(IExternalContentDialogLogic.View.class);
	}

	@SuppressWarnings("unchecked")
	private <V> void bindFakeViewImpl(Class<V> viewClass) {
		bind(viewClass).toProvider(new Provider<V>() {
			@Override
			public V get() {
				final Component component = new Panel(viewClass.getName()
						+ " fake view");
				return (V) Proxy.newProxyInstance(
						ViewModule.class.getClassLoader(),
						new Class[] { viewClass, Component.class },
						new InvocationHandler() {
							@Override
							public Object invoke(Object proxy, Method method,
									Object[] args) throws Throwable {
								System.out.println(method.getDeclaringClass()
										.getSimpleName()
										+ "."
										+ method.getName() + "()");
								if (method.getDeclaringClass()
										.isAssignableFrom(Component.class)) {
									return method.invoke(component, args);
								}
								return null;
							}
						});
			}
		});
	}

}
