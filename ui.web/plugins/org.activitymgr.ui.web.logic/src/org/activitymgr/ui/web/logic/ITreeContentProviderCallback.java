package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface ITreeContentProviderCallback<TYPE> extends IListContentProviderCallback<TYPE> {

	Collection<TYPE> getChildren(TYPE element);

	boolean hasChildren(TYPE element);

	TYPE getParent(TYPE element);

	boolean isRoot(TYPE element);

}
