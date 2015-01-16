package org.activitymgr.ui.web.logic;

public interface ITabLogic<VIEW extends ITabLogic.View<?>> extends ILogic<VIEW> {

	public interface View<LOGIC extends ITabLogic<?>> extends IView<LOGIC> {

		void addButton(String label, char key, boolean ctrl, boolean shift,
				boolean alt, IButtonLogic.View button);

		void addDownloadButton(IDownloadButtonLogic.View button);

	}

}
