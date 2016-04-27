package org.activitymgr.core.model;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * EXCEL processing exception.
 * @author jbrazeau
 *
 */
public class XLSModelException extends ModelException {
	
	/** Fake serial version UID */
	private static final long serialVersionUID = 1L;
	
	/** Invalid cell */
	private Cell cell;

	/**
	 * Default constructor.
	 * 
	 * @param message
	 *            error message.
	 */
	public XLSModelException(Cell cell, String message) {
		super(message);
		this.cell = cell;
	}
	
	/**
	 * Return the invalid cell.
	 * @return the invalid cell.
	 */
	public Cell getCell() {
		return cell;
	}
	
}