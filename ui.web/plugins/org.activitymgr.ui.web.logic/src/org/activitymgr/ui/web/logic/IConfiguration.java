package org.activitymgr.ui.web.logic;

public interface IConfiguration {

	String getStringParameter(String key);
	
	int getIntParameter(String key);
	
	boolean getBooleanParameter(String key);

}
