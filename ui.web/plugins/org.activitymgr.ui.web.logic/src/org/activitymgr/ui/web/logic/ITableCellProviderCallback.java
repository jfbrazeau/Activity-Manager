package org.activitymgr.ui.web.logic;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITableCellProviderCallback<TYPE> {
	
	IView<?> getCell(TYPE itemId, String propertyId);
	
	Integer getColumnWidth(String propertyId);

	String getFooter(String propertyId);

	Collection<String> getPropertyIds();

	Collection<TYPE> getRootElements();
	
	boolean contains(TYPE element);

}
