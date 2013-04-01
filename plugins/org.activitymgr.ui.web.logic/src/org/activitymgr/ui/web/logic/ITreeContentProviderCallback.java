package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface ITreeContentProviderCallback {

	ILabelProviderCallback getLabelProvider(String itemId);

	Collection<String> getChildren(String itemId);

	Collection<String> rootItemIds();

	boolean isRoot(String itemId);

}
