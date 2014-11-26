package org.activitymgr.ui.web.logic;

import java.util.Arrays;
import java.util.Collection;

public interface ILabelProviderCallback<TYPE> {
	
	public static final String NAME_PROPERTY_ID = "NAME";
	public static final Collection<String> DEFAULT_PROPERTY_IDS = Arrays
			.asList(new String[] { NAME_PROPERTY_ID });

	String getText(TYPE object, String propertyId);
	
	Collection<String> getPropertyIds();

}
