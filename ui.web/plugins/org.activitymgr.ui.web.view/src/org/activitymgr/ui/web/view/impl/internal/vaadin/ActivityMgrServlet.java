package org.activitymgr.ui.web.view.impl.internal.vaadin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;

import com.google.inject.Inject;
import com.vaadin.server.VaadinServlet;

final class ActivityMgrServlet extends VaadinServlet {
	/**
	 * 
	 */
	private final long start;

	@Inject
	private Set<IRESTServiceLogic> serviceLogics;

	private Map<String, IRESTServiceLogic> serviceLogicsMap = new HashMap<String, IRESTServiceLogic>();

	ActivityMgrServlet() {
		this.start = System.currentTimeMillis();
		Activator.getDefault().getInjector().injectMembers(this);
		for (IRESTServiceLogic serviceLogic : serviceLogics) {
			String path = serviceLogic.getPath().trim();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			serviceLogicsMap.put(path, serviceLogic);
		}
		System.out.println(serviceLogics);
	}

	@Override
	public void service(ServletRequest req,
			ServletResponse res) throws ServletException,
			IOException {
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		if (uri.startsWith("/service")) {
			String servicePath = uri.substring("/service".length());
			IRESTServiceLogic serviceLogic = serviceLogicsMap.get(servicePath);
			if (serviceLogic != null) {
				serviceLogic.service(new IRESTServiceLogic.Parameters() {
					@Override
					public String getParameter(String name) {
						return request.getParameter(name);
					}

					@Override
					public String[] getListParameter(String name) {
						String str = request.getParameter(name);
						String[] result = null;
						if (str != null) {
							result = str.split(",");
							for (int i = 0; i < result.length; i++) {
								result[i] = result[i].trim();
							}
						}
						return result;
					}
				}, res.getOutputStream());
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
		else if (!uri.startsWith("/VAADIN/")) {
			logS(uri + "- in");
			long now = System.currentTimeMillis();
			super.service(req, res);
			logS(uri + "- Time spent : " + (System.currentTimeMillis() - now));
		}
		else {
			super.service(req, res);
		}
	}

	private void logS(String s) {
//		String format = "0000000";
//		String time = format + String.valueOf(System.currentTimeMillis() - start);
//		System.out.println(time.substring(time.length() - format.length()) + "-" + s);
	}
}