package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.ISelectFieldLogic;
import org.activitymgr.ui.web.logic.ISelectFieldLogic.View;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;

@SuppressWarnings("serial")
public class SelectFieldView<ITEM_ID_TYPE> extends ComboBox implements View<ITEM_ID_TYPE> {

	@SuppressWarnings("unused")
	private ISelectFieldLogic<ITEM_ID_TYPE> logic;
	
	@Inject
	private IResourceCache resourceCache;

	@Override
	public void registerLogic(final ISelectFieldLogic<ITEM_ID_TYPE> logic) {
		this.logic = logic;
		setImmediate(true);
		setTextInputAllowed(false);
		addValueChangeListener(new Property.ValueChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				logic.onSelectedItemChanged((ITEM_ID_TYPE) getValue());
			}
		});
	}
	@Override
	public void setSelectedItem(ITEM_ID_TYPE selectedItemId) {
		setValue(selectedItemId);
	}

	@Override
	public void setValuesProviderCallback(
			ITableCellProviderCallback<ITEM_ID_TYPE> callback) {
    	TableDatasource<ITEM_ID_TYPE> datasource = new TableDatasource<ITEM_ID_TYPE>(resourceCache, callback);
    	setContainerDataSource(datasource);
    	String propertyId = callback.getPropertyIds().iterator().next();
		setItemCaptionPropertyId(propertyId);
    	setWidth(callback.getColumnWidth(propertyId), Unit.PIXELS);
	}

}
