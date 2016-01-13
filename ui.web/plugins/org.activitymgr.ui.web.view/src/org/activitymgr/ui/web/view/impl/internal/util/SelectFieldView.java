package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Map;

import org.activitymgr.ui.web.logic.ISelectFieldLogic;
import org.activitymgr.ui.web.logic.ISelectFieldLogic.View;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.NativeSelect;

@SuppressWarnings("serial")
public class SelectFieldView<ITEM_ID_TYPE> extends NativeSelect implements View<ITEM_ID_TYPE> {

	@SuppressWarnings("unused")
	private ISelectFieldLogic<ITEM_ID_TYPE> logic;
	
	@SuppressWarnings("unused")
	@Inject
	private IResourceCache resourceCache;

	@Override
	public void registerLogic(final ISelectFieldLogic<ITEM_ID_TYPE> logic) {
		this.logic = logic;
		setImmediate(true);
		addValueChangeListener(new Property.ValueChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				logic.onValueChanged((ITEM_ID_TYPE) getValue());
			}
		});
	}

	@Override
	public void setItems(Map<ITEM_ID_TYPE, String> items) {
    	setContainerDataSource(new MapBasedDatasource<ITEM_ID_TYPE>(items));
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width + "px");
	}

}

