package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Iterator;
import java.util.List;

import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;
import com.vaadin.data.Item;

@SuppressWarnings("serial")
public class TableDatasource<ITEMID_TYPE> extends
		AbstractTableDatasource<ITEMID_TYPE, ITableCellProviderCallback<ITEMID_TYPE>> implements Container.Ordered {

	public TableDatasource(IResourceCache resourceCache,
			ITableCellProviderCallback<ITEMID_TYPE> cellProvider) {
		super(resourceCache, cellProvider);
	}

	@Override
	public Object nextItemId(Object itemId) {
		Iterator<ITEMID_TYPE> rootElements = getCellProvider().getRootElements().iterator();
		while (rootElements.hasNext()) {
			ITEMID_TYPE current = rootElements.next();
			if (itemId.equals(current)) {
				return rootElements.hasNext() ? rootElements.next() : null;
			}
		}
		return null;
	}

	@Override
	public Object prevItemId(Object itemId) {
		ITEMID_TYPE previous = null;
		Iterator<ITEMID_TYPE> rootElements = getCellProvider().getRootElements().iterator();
		while (rootElements.hasNext()) {
			ITEMID_TYPE current = rootElements.next();
			if (itemId.equals(current)) {
				return previous;
			}
			else {
				previous = current;
			}
		}
		return null;
	}

	@Override
	public Object firstItemId() {
		List<ITEMID_TYPE> rootElements = getCellProvider().getRootElements();
		if (rootElements.size() > 0) {
			return rootElements.get(0);
		}
		return null;
	}

	@Override
	public Object lastItemId() {
		List<ITEMID_TYPE> rootElements = getCellProvider().getRootElements();
		int size = rootElements.size();
		if (size > 0) {
			return rootElements.get(size - 1);
		}
		return null;
	}

	@Override
	public boolean isFirstId(Object itemId) {
		List<ITEMID_TYPE> rootElements = getCellProvider().getRootElements();
		return rootElements.size() > 0 && itemId.equals(rootElements.get(0));
	}

	@Override
	public boolean isLastId(Object itemId) {
		List<ITEMID_TYPE> rootElements = getCellProvider().getRootElements();
		int size = rootElements.size();
		return size > 0 && itemId.equals(rootElements.get(size - 1));
	}

	@Override
	public Object addItemAfter(Object previousItemId)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item addItemAfter(Object previousItemId, Object newItemId)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
