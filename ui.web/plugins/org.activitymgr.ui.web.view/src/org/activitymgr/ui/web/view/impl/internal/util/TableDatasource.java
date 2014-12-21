package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

@SuppressWarnings("serial")
public class TableDatasource<ITEMID_TYPE> extends
		AbstractTableDatasource<ITEMID_TYPE, ITableCellProviderCallback<ITEMID_TYPE>> {

	public TableDatasource(IResourceCache resourceCache,
			ITableCellProviderCallback<ITEMID_TYPE> cellProvider) {
		super(resourceCache, cellProvider);
	}

}
