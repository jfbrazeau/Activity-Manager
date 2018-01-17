package org.activitymgr.ui.web.logic.impl.internal.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;

import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;

public class LoadingWrapperServiceLogic implements IRESTServiceLogic {

	public static final String SERVICE = "service";

	@Override
	public String getPath() {
		return "/load";
	}

	@Override
	public void service(Request request, Response response) throws IOException {
		response.setContentType("text/html");
		OutputStream out = response.getOutputStream();
		PrintWriter pw = new PrintWriter(out);
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<style>");
		pw.println("html {");
		pw.println("   width:100%;");
		pw.println("   height:100%;");
		pw.println("   background:url(/VAADIN/themes/activitymgr/icons/loading.gif) center center no-repeat;");
		pw.println("}");
		pw.println("</style>");
		pw.print("<meta http-equiv=\"refresh\" content=\"0;URL='");
		Enumeration<String> parameters = request.getParameterNames();
		boolean first = true;
		pw.print(request.getParameter(SERVICE));
		while (parameters.hasMoreElements()) {
			String name = parameters.nextElement();
			if (!SERVICE.equals(name)) {
				pw.print(first ? '?' : '&');
				first = false;
				pw.print(name);
				pw.print('=');
				pw.print(StringHelper.urlEncodeAmpersand(request
						.getParameter(name)));
			}
		}
		pw.println("'\">");
		pw.println("</head>");
		pw.println("<body></body>");
		pw.println("</html>");
		pw.flush();
	}

}
