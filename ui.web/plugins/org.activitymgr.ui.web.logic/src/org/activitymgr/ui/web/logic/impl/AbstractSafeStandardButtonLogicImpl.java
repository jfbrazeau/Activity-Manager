package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.logic.impl.internal.KeyBinding;

public abstract class AbstractSafeStandardButtonLogicImpl extends AbstractLogicImpl<IStandardButtonLogic.View> implements IStandardButtonLogic {

	public AbstractSafeStandardButtonLogicImpl(ITabLogic<?> parent, String label, String iconId, String shortcutKey) {
		super(parent);
		if (iconId != null) {
			getView().setIcon(iconId);
		} else if (label != null) {
			getView().setLabel(label);
		}
		if (label != null) {
			getView().setDescription(
					label
							+ (shortcutKey != null ? " <em>" + shortcutKey
									+ "</em>" : ""));
		}
		if (shortcutKey != null) {
			KeyBinding kb = new KeyBinding(shortcutKey);
			getView().setShortcut(kb.getKey(), kb.isCtrl(), kb.isShift(), kb.isAlt());
		}
	}

	@Override
	public final void onClick() {
		try {
			unsafeOnClick();
		}
		catch (Throwable t) {
			doThrow(t);
		}
	}

	protected abstract void unsafeOnClick() throws Exception;
	
}
