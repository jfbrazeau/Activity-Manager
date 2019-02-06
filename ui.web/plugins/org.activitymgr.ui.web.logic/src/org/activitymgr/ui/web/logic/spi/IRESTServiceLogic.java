package org.activitymgr.ui.web.logic.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import org.activitymgr.ui.web.logic.IUILogicContext;

public interface IRESTServiceLogic {

	interface Request {

		Enumeration<String> getParameterNames();

		String getHeader(String name);

		String getCookie(String name);

		String getParameter(String name);

		String[] getListParameter(String name);

		IUILogicContext getAttachedUILogicContext();

	}

	interface Response {

		void setContentType(String contentType);

		String getContentType();

		void addHeader(String name, String value);

		void sendError(int sc, String msg) throws IOException;

		OutputStream getOutputStream() throws IOException;

	}

	String getPath();

	void service(Request request, Response response)
			throws IOException;

}
