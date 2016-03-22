package org.activitymgr.ui.web.logic.impl.internal;

import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Savepoint;
import java.util.Stack;

import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITransactionalWrapperBuilder;

import com.google.inject.Inject;

final class TransactionalManagerImpl implements
		ITransactionalWrapperBuilder {
	
	@Inject
	private IRootLogic rootLogic;
	
	@Inject
	private ThreadLocalizedDbTransactionProviderImpl dbTxProvider;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T buildTransactionalWrapper(final T wrapped, Class<?> interfaceToWrapp) {
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
								//log(txCtx, "START");
								// Call the real model manager
								System.out.println(indent(txCtx.getCalls(), wrapped));
								Object result = method.invoke(wrapped, args);
	
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
//								if (method.getReturnType().equals(void.class)) {
									return null;
//								}
//								else {
								//	throw exception;
//								}
							} finally {
								//log(txCtx, "END");
								if (txCtx != null) {
									txCtx.getCalls().pop();
									if (txCtx.getCalls().size() == 0) {
										// Release the transaction
										System.out.println("** Close connection " + txCtx.getTx());
										dbTxProvider.release();
									}
								}
							}
						}
					}
				});
	}
	
	private static String indent(Stack<Method> stack, Object owner) {
		StringWriter w = new StringWriter();
		for (int i=0; i<stack.size(); i++) {
			w.append(' ');
		}
		Method m = stack.peek();
		if (owner == null) {
			w.append(m.getDeclaringClass().getName());
		}
		else {
			w.append(owner.getClass().getName());
		}
		w.append('.');
		w.append(m.getName());
		return w.toString();
	}
}