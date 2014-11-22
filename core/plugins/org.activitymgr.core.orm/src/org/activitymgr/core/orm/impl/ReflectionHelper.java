package org.activitymgr.core.orm.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectionHelper {

	public static List<Field> getFields(Class<?> theClass) {
		return collectFields(theClass, new ArrayList<Field>());
	}

	private static List<Field> collectFields(Class<?> theClass,
			List<Field> fields) {
		if (theClass != null) {
			collectFields(theClass.getSuperclass(), fields);
			for (Field f : theClass.getDeclaredFields()) {
				if (!Modifier.isStatic(f.getModifiers())) 
					fields.add(f);
			}
		}
		return fields;
	}

	/**
	 * Creates a new instance of the given class constructor.
	 * 
	 * @param the
	 *            class to instantiate.
	 * @return the new instance.
	 */
	public static <TYPE> TYPE newInstance(Constructor<TYPE> c, Object... initArgs) {
		try {
			return c.newInstance(initArgs);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static List<Field> selectFields(Class<?> theClass, String... fieldNames) {
		List<Field> result = new ArrayList<Field>();
		for (int i=0; i<fieldNames.length; i++) {
			result.add(select(theClass, fieldNames[i]));
		}
		return result;
	}

	public static Field select(Class<?> theClass, String fieldName) {
		Field result = doSelect(theClass, fieldName);
		if (result == null) {
			throw new IllegalArgumentException("Unknown field '" + fieldName + "' for '" + theClass.getName() + "'");
		}
		return result;
	}

	private static Field doSelect(Class<?> theClass, String fieldName) {
		if (theClass != null) {
			for (Field f : theClass.getDeclaredFields()) {
				if (!Modifier.isStatic(f.getModifiers()) && fieldName.equals(f.getName())) {
					return f;
				}
			}
			return doSelect(theClass.getSuperclass(), fieldName);
		}
		return null;
	}

}
