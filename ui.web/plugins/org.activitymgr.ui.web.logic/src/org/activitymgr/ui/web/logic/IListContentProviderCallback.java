package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface IListContentProviderCallback {

	ILabelProviderCallback getLabelProvider(String itemId);

	Collection<String> rootItemIds();

}
