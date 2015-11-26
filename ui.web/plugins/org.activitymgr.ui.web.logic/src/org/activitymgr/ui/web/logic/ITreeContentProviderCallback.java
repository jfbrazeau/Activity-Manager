package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface ITreeContentProviderCallback<ITEM_ID_TYPE> extends ITableCellProviderCallback<ITEM_ID_TYPE> {

	Collection<ITEM_ID_TYPE> getChildren(ITEM_ID_TYPE element);

	boolean hasChildren(ITEM_ID_TYPE element);

	ITEM_ID_TYPE getParent(ITEM_ID_TYPE element);

	boolean isRoot(ITEM_ID_TYPE element);

}
