package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IAOPWrappersBuilder {

	<LOGIC> LOGIC buildLogicWrapperForView(final LOGIC wrapped,
			final Class<LOGIC> interfaceToWrapp);

	<V extends IView<?>> V buildViewWrapperForLogic(
			V wrappedView, Class<V> viewInterface);

}
