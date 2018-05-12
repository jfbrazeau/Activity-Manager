package org.activitymgr.ui.web.viewng.impl.internal.util;

import org.activitymgr.ui.web.logic.Align;

import com.vaadin.ui.StyleGenerator;

@SuppressWarnings("serial")
public class DefaultStyleGenerator<T> implements StyleGenerator<T> {

	private String style;

	public DefaultStyleGenerator(Align align) {
		if (align != null) {
			switch (align) {
			case CENTER:
				style = "v-align-center";
			case RIGHT:
				style = "v-align-right";
			case LEFT:
			default:
			}
		}
	}

	@Override
	public String apply(T item) {
		return style;
	}

}
