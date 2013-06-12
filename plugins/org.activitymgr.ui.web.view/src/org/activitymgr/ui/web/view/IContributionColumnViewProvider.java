package org.activitymgr.ui.web.view;

public interface IContributionColumnViewProvider {
	
	boolean isProviderFor(String columnId);
	
	String getLabel(String columnId);
	
	Class<?> getColumnType(String columnId);

	int getColumnWidth(String id);

}
