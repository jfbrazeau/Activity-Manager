package org.activitymgr.ui.web.view.impl.internal.util;

import com.vaadin.ui.Table.Align;

public class AlignHelper {
	
	public static Align toVaadinAlign(org.activitymgr.ui.web.logic.Align align) {
		if (align == null) {
			return Align.LEFT;
		}
		switch (align) {
		case CENTER:
			return Align.CENTER;
		case RIGHT:
			return Align.RIGHT;
		default:
			return Align.LEFT;
		}
	}

}
