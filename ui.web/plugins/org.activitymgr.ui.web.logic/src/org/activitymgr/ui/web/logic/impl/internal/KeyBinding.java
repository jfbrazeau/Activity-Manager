package org.activitymgr.ui.web.logic.impl.internal;


public class KeyBinding {

	private static final String CTRL = "CTRL";
	private static final String SHIFT = "SHIFT";
	private static final String ALT = "ALT";

	private char key;
	private boolean ctrl;
	private boolean shift;
	private boolean alt;

	public KeyBinding(String keyBinding) {
		// TODO manage other shortcut keys like function keys
		String[] fragments = keyBinding.split("\\+");
		for (String fragment : fragments) {
			fragment = fragment.toUpperCase();
			if (CTRL.equals(fragment)) {
				ctrl = true;
			} else if (SHIFT.equals(fragment)) {
				shift = true;
			} else if (ALT.equals(fragment)) {
				alt = true;
			} else if (fragment.length() > 0) {
				key = fragment.charAt(0);
			}
		}
	}

	public char getKey() {
		return key;
	}

	public boolean isCtrl() {
		return ctrl;
	}

	public boolean isShift() {
		return shift;
	}

	public boolean isAlt() {
		return alt;
	}

	@Override
	public String toString() {
		return (ctrl ? "Ctrl+" : "") + (shift ? "Shift+" : "")
				+ (alt ? "Alt+" : "") + ((char) key);
	}
}
