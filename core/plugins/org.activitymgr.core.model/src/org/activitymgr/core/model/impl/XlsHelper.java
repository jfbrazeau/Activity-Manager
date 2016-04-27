package org.activitymgr.core.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.model.XLSModelException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel helper methods.
 * @author jbrazeau
 */
public class XlsHelper {
	
	public static class XLSCell {
		private final String columnName;
		private final Cell cell;
		private final Object value;
		public XLSCell(String columnName, Cell cell, Object value) {
			this.columnName = columnName;
			this.cell = cell;
			this.value = value;
		}
	
		public String getColumnName() {
			return columnName;
		}
		
		public Cell getCell() {
			return cell;
		}
		
		public Object getValue() {
			return value;
		}
	
	}
	
	public static interface IXLSHandler {
		
		void handleRow(Map<String, XLSCell> cells) throws ModelException;
		
	}

	public static <T> void visit(InputStream xls, IXLSHandler handler) throws IOException, ModelException {
		BeanUtilsBean.setInstance(new BeanUtilsBean2());
		
		Workbook wbk = new HSSFWorkbook(xls);
		if (wbk.getNumberOfSheets() == 0) {
			throw new ModelException("Workbook must contain at least one sheet");
		}
		Sheet sheet = wbk.getSheetAt(0);
		if (sheet.getLastRowNum() == 0) {
			throw new ModelException("Sheet must contain a header row and at least a content row");
		}

		// Process header row
		Map<Integer, String> columnNamesIndex = new LinkedHashMap<Integer, String>();
		Row headerRow = sheet.getRow(0);
		for (int i = 0; i<headerRow.getLastCellNum(); i++) {
			Cell cell = headerRow.getCell(i);
			if (cell != null) {
				String colmunName = cell.getStringCellValue().trim();
				if (!"".equals(colmunName)) {
					columnNamesIndex.put(i, colmunName);
				}
			}
		}
		
		// Process each row
		Map<String, XLSCell> map = new LinkedHashMap<String, XlsHelper.XLSCell>();
		for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
			map.clear();
			Row row = sheet.getRow(rowIdx);
			for (int colIdx : columnNamesIndex.keySet()) {
				String columnName = columnNamesIndex.get(colIdx);
				Cell cell = row.getCell(colIdx);
				if (cell != null) {
					Object value = null;
					// Retrieve type
					int cellType = cell.getCellType();
					if (cellType == Cell.CELL_TYPE_FORMULA) {
						cellType = cell.getCachedFormulaResultType();
					}
					switch (cellType) {
					case Cell.CELL_TYPE_BLANK :
						// Do nothing
						break;
					case Cell.CELL_TYPE_BOOLEAN :
						value = cell.getBooleanCellValue();
						break;
					case Cell.CELL_TYPE_STRING :
						value = cell.getStringCellValue();
						break;
					case Cell.CELL_TYPE_NUMERIC :
						value = cell.getNumericCellValue();
						break;
					case Cell.CELL_TYPE_ERROR :
						throw new XLSModelException(cell, "Cell contains an error");
					}
					map.put(columnName, new XLSCell(columnName, cell, value));
				}
			}
			// Handle the new Object
			handler.handleRow(map);
		}
	}

}
