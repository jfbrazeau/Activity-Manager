package org.activitymgr.ui.web.view.impl.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONException;
import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

import elemental.json.JsonArray;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonString;

@JavaScript({ "SignWithGoogleButton.js" })
public class SignWithGoogleButton extends AbstractJavaScriptComponent {

	static interface Listener {
		void onGoogleProfileAuthorized(String idToken);
	}

	private List<Listener> listeners = new ArrayList<SignWithGoogleButton.Listener>();

	public SignWithGoogleButton(String clientId) {
		getState().clientId = clientId;
		addFunction("onGoogleProfileAuthorized", new JavaScriptFunction() {
			public void call(JsonArray arguments) throws JSONException {
				JsonValue jsonValue = arguments.get(0);
				if (jsonValue instanceof JreJsonString) {
					String idToken = ((JreJsonString) jsonValue).getString();
					for (Listener listener : listeners) {
						listener.onGoogleProfileAuthorized(idToken);
					}
				}
			}
		});
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	@Override
	protected SignWithGoogleButtonState getState() {
		return (SignWithGoogleButtonState) super.getState();
	}

}

