package org.activitymgr.ui.web.logic;

public interface IConfiguration {

	String get(String key, String defaultValue);

	String get(String key);
	
	int getInt(String key, int defaultValue);

	int getInt(String key);
	
	boolean getBoolean(String key, boolean defaultValue);

	boolean getBoolean(String key);

	IConfiguration getScoped(String prefix, String suffix);

	boolean isEmpty();

}
