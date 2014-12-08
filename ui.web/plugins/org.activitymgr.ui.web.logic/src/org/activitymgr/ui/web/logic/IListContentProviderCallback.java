package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface IListContentProviderCallback<TYPE> extends ILabelProviderCallback<TYPE> {

	Collection<TYPE> getRootElements();
	
	boolean contains(TYPE element);

}
