package org.activitymgr.ui.web.logic.spi;

import java.io.IOException;
import java.io.OutputStream;

public interface IRESTServiceLogic {

	interface Parameters {

		String getParameter(String name);

		String[] getListParameter(String name);

	}

	String getContentType();

	String getPath();

	void service(Parameters parameters, OutputStream response)
			throws IOException;

}
