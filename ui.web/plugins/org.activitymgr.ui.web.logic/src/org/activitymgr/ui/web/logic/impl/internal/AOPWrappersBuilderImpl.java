package org.activitymgr.ui.web.logic.impl.internal;

import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Savepoint;
import java.util.Stack;

import org.activitymgr.ui.web.logic.IAOPWrappersBuilder;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IUINotificationsBlockingViewWrapper;

import com.google.inject.Inject;

final class AOPWrappersBuilderImpl implements
		IAOPWrappersBuilder {
	
	@Inject
	private IRootLogic rootLogic;
	
	@Inject
	private ThreadLocalizedDbTransactionProviderImpl dbTxProvider;
	
	private ThreadLocal<Boolean> viewNotificationsDisabled = new ThreadLocal<Boolean>();

	@SuppressWarnings("unchecked")
	@Override
	public <V extends IView<?>> V buildViewWrapperForLogic(
			final V wrappedView, Class<V> viewInterface) {
		return (V) Proxy.newProxyInstance(viewInterface
				.getClassLoader(), new Class<?>[] { viewInterface,
				IUINotificationsBlockingViewWrapper.class }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
				if (method.getDeclaringClass().equals(IUINotificationsBlockingViewWrapper.class)) {
					return wrappedView;
				} else {
					try {
						disableViewNotifications();
						if (args != null) {
							for (int i = 0; i < args.length; i++) {
								Object arg = args[i];
								if (arg instanceof IUINotificationsBlockingViewWrapper) {
									args[i] = ((IUINotificationsBlockingViewWrapper) arg).unwrapp();
								}
							}
						}
						return method.invoke(wrappedView, args);
					} finally {
						enableViewNotifications();
					}
				}
			}
		});
	}

	private void enableViewNotifications() {
		viewNotificationsDisabled.remove();
	}

	private void disableViewNotifications() {
		viewNotificationsDisabled.set(Boolean.TRUE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T buildLogicWrapperForView(final T wrapped,
			Class<T> interfaceToWrapp) {
		return (T) Proxy.newProxyInstance(
				wrapped.getClass().getClassLoader(),
				// TODO add comments
				new Class<?>[] { interfaceToWrapp }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						if (method.getDeclaringClass().equals(Object.class)) {
							return method.invoke(wrapped, args);
						}
						else {
							DbTransactionContext txCtx = dbTxProvider.get();
							Savepoint sp = null;
							try {
								// Open the transaction if required and push a savepoint
								if (txCtx == null) {
									// Bind TX provider
									txCtx = dbTxProvider.newCtx();
								}
								else {
									sp = txCtx.getTx().setSavepoint();
								}
								txCtx.getCalls().push(method);

								// Prepare the call
								Object result = null;
								Boolean notificationsDisabled = viewNotificationsDisabled
										.get();
								// If notifications are disabled, drop all calls
								// to logic void methods which corresponds to
								// notification methods (ex :
								// logic.onTextFieldChange())
								if (method.getReturnType().equals(void.class)
										&& Boolean.TRUE
												.equals(notificationsDisabled)) {
									// Don't invoke the method and return
									result = null;
								} else {
									// Invoke
									result = method.invoke(wrapped, args);
									// If the invoked method returns a view, it
									// is probably a view wrapper which has to
									// be unwrapped for the view side
									if (result instanceof IUINotificationsBlockingViewWrapper) {
										result = ((IUINotificationsBlockingViewWrapper) result)
												.unwrapp();
									}
								}
	
								// Commit the transaction (or put a save point)
								if (txCtx.getCalls().size() > 1) {
									sp = txCtx.getTx().setSavepoint();
								}
								else {
									txCtx.getTx().commit();
								}
								return result;
							} catch (InvocationTargetException t) {
								// Rollback the transaction in case of failure
								if (txCtx.getCalls().size() > 1) {
									txCtx.getTx().rollback(sp);
								}
								else {
									txCtx.getTx().rollback();
								}
								Throwable exception = t.getTargetException();
								exception.printStackTrace();
								// Building message
								String message = exception.getMessage();
								if (message == null || "".equals(message.trim())) {
									message = exception.getClass().getSimpleName();
								}
								// Generating details
								String details = null;
								Throwable cause = exception;
								while ((cause = cause.getCause()) != null) {
									details = cause.getClass().getSimpleName() + " : " + cause.getMessage() + "\n";
								}
								rootLogic.getView().showErrorNotification(message, details);
								return null;
							} finally {
								//log(txCtx, "END");
								if (txCtx != null) {
									txCtx.getCalls().pop();
									if (txCtx.getCalls().size() == 0) {
										// Release the transaction
										dbTxProvider.release();
									}
								}
							}
						}
					}
				});
	}
	
	@SuppressWarnings("unused")
	private static String indent(Stack<Method> stack, Object owner) {
		StringWriter w = new StringWriter();
		for (int i = 0; i < stack.size(); i++) {
			w.append(' ');
		}
		Method m = stack.peek();
		if (owner == null) {
			w.append(m.getDeclaringClass().getName());
		} else {
			w.append(owner.getClass().getName());
		}
		w.append('.');
		w.append(m.getName());
		return w.toString();
	}
}