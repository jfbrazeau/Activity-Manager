package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic.View;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.AbstractTabPanel.ButtonBasedShortcutListener;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.ui.Button;
import com.vaadin.ui.HasComponents;

@SuppressWarnings("serial")
public class StandardButtonView extends Button implements View {

	@SuppressWarnings("unused")
	private IStandardButtonLogic logic;
	
	@Inject
	private IResourceCache resourceCache;
	
	private ButtonBasedShortcutListener shortcut;

	@Override
	public void setIcon(String iconId) {
		setIcon(resourceCache.getResource(iconId + ".gif"));
	}

	@Override
	public void setShortcut(final char key, final boolean ctrl, final boolean shift, final boolean alt) {
		shortcut = new ButtonBasedShortcutListener(StandardButtonView.this, key, ctrl, shift, alt);
	}

	public ButtonBasedShortcutListener getShortcut() {
		return shortcut;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			// Go up in component hierarchy until the tab
			HasComponents cursor = getParent();
			while (!(cursor instanceof AbstractTabPanel)) {
				cursor = cursor.getParent();
			}
			if (enabled) {
				((AbstractTabPanel<?>)cursor)
						.enableShortcut(shortcut);
			} else {
				((AbstractTabPanel<?>)cursor)
						.disableShortcut(shortcut);
			}
			super.setEnabled(enabled);
		}
	}

	@Override
	public void registerLogic(final IStandardButtonLogic logic) {
		this.logic = logic;
		setImmediate(true);
		addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				logic.onClick();
			}
		});
	}

}
