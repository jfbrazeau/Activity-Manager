package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface ITreeContentProviderCallback extends IListContentProviderCallback {

	Collection<String> getChildren(String itemId);

	boolean isRoot(String itemId);

}
