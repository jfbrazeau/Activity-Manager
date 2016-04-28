package org.activitymgr.core.model;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;

/**
 * EXCEL processing exception.
 * 
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
	 * @param cell
	 *            the cell associated to the errror.
	 * @param message
	 *            error message.
	 */
	public XLSModelException(Cell cell, String message) {
		super(msgPrefix(cell) + message);
		this.cell = cell;
	}

	/**
	 * Default constructor.
	 * 
	 * @param cell
	 *            the cell associated to the errror.
	 * @param cause
	 *            the error cause.
	 */
	public XLSModelException(Cell cell, ModelException cause) {
		super(msgPrefix(cell) + cause.getMessage(), cause);
		this.cell = cell;
	}

	/**
	 * Return the invalid cell.
	 * 
	 * @return the invalid cell.
	 */
	public Cell getCell() {
		return cell;
	}

	/**
	 * Computes a prefix for each error message giving the cell position.
	 * @param cell the cell.
	 * @return the message prefix.
	 */
	private static String msgPrefix(Cell cell) {
		return "Cell " + CellReference.convertNumToColString(cell.getColumnIndex()) + cell.getRowIndex() + " : ";
	}
}