package org.activitymgr.ui.web.logic;

public interface ITransactionalWrapperBuilder {

	<T> T buildTransactionalWrapper(final T wrapped, final Class<?> interfaceToWrapp);

}
