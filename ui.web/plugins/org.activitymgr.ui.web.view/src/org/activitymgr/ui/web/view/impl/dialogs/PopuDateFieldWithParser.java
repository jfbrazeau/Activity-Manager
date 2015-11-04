package org.activitymgr.ui.web.view.impl.dialogs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.PopupDateField;

@SuppressWarnings("serial")
public class PopuDateFieldWithParser extends PopupDateField {
	@Override
	protected Date handleUnparsableDateString(String dateString)
	        throws Converter.ConversionException {
		try {
			int idx = dateString.indexOf(' ');
			if (idx > 0) {
				dateString = dateString.substring(idx);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			return sdf.parse(dateString);
		} catch (ParseException ignore) {
			return new Date();
		}
	}
}