package org.activitymgr.ui.web.logic;

import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITableCellProviderCallback<TYPE> {
	
	public static final String NAME_PROPERTY_ID = "NAME";
	public static final Collection<String> DEFAULT_PROPERTY_IDS = Arrays
			.asList(new String[] { NAME_PROPERTY_ID });

	IView<?> getCell(TYPE itemId, String propertyId);
	
	Collection<String> getPropertyIds();

	Collection<TYPE> getRootElements();
	
	boolean contains(TYPE element);

}
