package org.activitymgr.ui.web.logic;

import java.util.Map;

public interface IConfiguration {

	String getStringParameter(String key);
	
	int getIntParameter(String key);
	
	boolean getBooleanParameter(String key);

	Map<String, String> getScopedParameters(String prefix, String suffix);

}
