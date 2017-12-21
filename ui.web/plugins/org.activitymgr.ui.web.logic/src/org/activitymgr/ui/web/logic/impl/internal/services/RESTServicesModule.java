package org.activitymgr.ui.web.logic.impl.internal.services;

import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class RESTServicesModule extends AbstractModule {

	@Override
	protected void configure() {
		// Bind rest services
		Multibinder<IRESTServiceLogic> restBinder = Multibinder.newSetBinder(
				binder(), IRESTServiceLogic.class);
		restBinder.addBinding().to(XLSReportServiceLogic.class)
				.in(Singleton.class);
	}

}

