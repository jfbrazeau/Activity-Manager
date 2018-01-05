package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Properties;

import org.activitymgr.ui.web.logic.IConfiguration;

public final class ConfigurationImpl implements IConfiguration {

	private final Properties props;

	public ConfigurationImpl(Properties props) {
		this.props = props;
	}

	@Override
	public String get(String key, String defaultValue) {
		String value = get(key);
		return value != null ? value : defaultValue;
	}

	@Override
	public String get(String key) {
		return props.getProperty(key);
	}

	@Override
	public int getInt(String key, int defaultValue) {
		String value = get(key);
		return value != null ? Integer.parseInt(props.getProperty(key))
				: defaultValue;
	}

	@Override
	public int getInt(String key) {
		return Integer.parseInt(props.getProperty(key));
	}

	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = get(key);
		return value != null ? Boolean.TRUE.toString().equalsIgnoreCase(
				props.getProperty(key)) : defaultValue;
	}

	@Override
	public boolean getBoolean(String key) {
		return Boolean.TRUE.toString().equalsIgnoreCase(props.getProperty(key));
	}

	@Override
	public IConfiguration getScoped(String prefix,
			String suffix) {
		if (prefix != null && !prefix.startsWith(".")) {
			prefix += ".";
		}
		if (suffix != null && !suffix.startsWith(".")) {
			suffix = "." + suffix;
		}
		Properties sub = new Properties();
		for (String key : props.stringPropertyNames()) {
			if ((prefix == null || key.startsWith(prefix))
					&& (suffix == null || key.endsWith(suffix))) {
				String newKey = key;
				if (prefix != null) {
					newKey = newKey.substring(prefix.length());
				}
				if (suffix != null) {
					newKey = newKey.substring(0, newKey.length()
							- suffix.length());
				}
				sub.setProperty(newKey,
						props.getProperty(key));
			}
		}
		return new ConfigurationImpl(sub);
	}

	@Override
	public boolean isEmpty() {
		return props.isEmpty();
	}
}