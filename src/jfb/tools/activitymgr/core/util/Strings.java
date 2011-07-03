package jfb.tools.activitymgr.core.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// TODO Javadoc + licence
public class Strings {
	private static final String BUNDLE_NAME = "jfb.tools.activitymgr.core.util.strings"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Strings() {
	}

	// TODO Javadoc
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	// TODO Javadoc
	public static String getString(String key, Object parameter) {
		return getString(key, new Object[] { parameter });
	}

	// TODO Javadoc
	public static String getString(String key, Object parameter1,
			Object parameter2) {
		return getString(key, new Object[] { parameter1, parameter2 });
	}

	// TODO Javadoc
	public static String getString(String key, Object parameter1,
			Object parameter2, Object parameter3) {
		return getString(key,
				new Object[] { parameter1, parameter2, parameter3 });
	}

	// TODO Javadoc
	public static String getString(String key, Object[] parameters) {
		// TODO mettre en cache
		MessageFormat mf = new MessageFormat(getString(key));
		return mf.format(parameters);
	}

}
