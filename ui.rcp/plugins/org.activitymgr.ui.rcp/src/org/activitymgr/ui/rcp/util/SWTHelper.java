/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.ui.rcp.util;

import java.io.FileOutputStream;
import java.io.IOException;

import org.activitymgr.core.util.Strings;
import org.activitymgr.core.util.WorkbookBuilder;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Classe offrant des services d'aide à l'utilisation de l'API SWT.
 */
public class SWTHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(SWTHelper.class);

	/**
	 * Centre un popup par rapport à sa fenêtre parent.
	 * 
	 * @param popupShell
	 *            le popup.
	 */
	public static void centerPopup(Shell popupShell) {
		// Définition de la positio du popup
		Point parentShellLocation = popupShell.getParent().getLocation();
		Point parentShellSize = popupShell.getParent().getSize();
		Point popupShellSize = popupShell.getSize();
		log.debug("parentShellSize = " + parentShellSize); //$NON-NLS-1$
		log.debug("popupShellSize = " + popupShellSize); //$NON-NLS-1$
		int x = parentShellLocation.x + (parentShellSize.x - popupShellSize.x)
				/ 2;
		int y = parentShellLocation.y + (parentShellSize.y - popupShellSize.y)
				/ 2;
		if (x < 0)
			x = 0;
		if (y < 0)
			y = 0;
		log.debug("x = " + x); //$NON-NLS-1$
		log.debug("y = " + y); //$NON-NLS-1$
		popupShell.setLocation(x, y);
		popupShell.setVisible(true);
	}

	/**
	 * Exporte un arbre SWT en fichier EXCEL.
	 * 
	 * @param tree
	 *            l'arbre à exporter.
	 * @throws UITechException
	 *             levé en cas de pb I/O lors de la sauvegarde du fichier EXCEL.
	 */
	public static void exportToWorkBook(Tree tree) throws UITechException {
		// Demande du nom de fichier
		FileDialog fd = new FileDialog(tree.getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
		fd.setFilterExtensions(new String[] { "*.xls", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setOverwrite(true);
		String fileName = fd.open();
		// Si le nom est spécifié
		if (fileName != null) {
			try {
				// Correction du nom du fichier si besoin
				if (!fileName.endsWith(".xls")) //$NON-NLS-1$
					fileName += ".xls"; //$NON-NLS-1$
				// Sauvegarde du document
				Workbook wb = toWorkBook(tree);
				FileOutputStream out = new FileOutputStream(fileName);
				wb.write(out);
				out.close();
			} catch (IOException e) {
				log.error("I/O exception", e); //$NON-NLS-1$
				throw new UITechException(
						Strings.getString("SWTHelper.errors.IO_EXCEPTION_WHILE_EXPORTING"), e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Convertit un arbre en classeur EXCEL.
	 * 
	 * @param tree
	 *            l'arbre à convertir.
	 * @return le classeur EXCEL.
	 */
	public static Workbook toWorkBook(Tree tree) {
		// Création du fichier EXCEL et du feuillet
		WorkbookBuilder wb = new WorkbookBuilder();
		Workbook workbook = wb.getWorkbook();
		Sheet sheet = workbook.createSheet(Strings
				.getString("SWTHelper.excelsheet.TAB_NAME")); //$NON-NLS-1$
		sheet.createFreezePane(0, 1, 0, 1);
		sheet.setColumnWidth(0, 10000);
		// Création de l'entête
		Row row = sheet.createRow(0);
		TreeColumn[] columns = tree.getColumns();
		for (int i = 0; i < columns.length; i++) {
			TreeColumn column = columns[i];
			sheet.setColumnWidth(i, (column.getWidth() * 50));
			Cell cell = row.createCell(i);
			wb.asHeaderCellStyl(cell).setCellValue(column.getText());
		}
		// Exportation des lignes du tableau
		TreeItem[] items = tree.getItems();
		appendToWorkbook("", wb, sheet, items, columns.length); //$NON-NLS-1$
		// Retour du résultat
		return workbook;
	}

	/**
	 * Génère des lignes dans le classeur EXCEL récursivement pour les éléments
	 * d'arbres spécifiés et leurs éléments fils.
	 * 
	 * @param indent
	 *            l'indentiation à appliquer (plus la profondeur dans l'arbre
	 *            est élevée, plus l'indentation est longue).
	 * @param wb
	 *            the workbook builder.
	 * @param sheet
	 *            le feuillet EXCEL.
	 * @param treeItems
	 *            les élements.
	 * @param columnsNb
	 *            le nombre de colonnes à exporter dans le feuillet.
	 */
	private static void appendToWorkbook(String indent, WorkbookBuilder wb, Sheet sheet,
			TreeItem[] treeItems, int columnsNb) {
		log.debug("sheet.getLastRowNum() : " + sheet.getLastRowNum()); //$NON-NLS-1$
		int startRowNum = sheet.getLastRowNum() + 1;
		for (int i = 0; i < treeItems.length; i++) {
			TreeItem treeItem = treeItems[i];
			log.debug(" +-> TreeItem : " + i + ", expanded=" + treeItem.getExpanded() + ", data='" + treeItem.getData() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (treeItem.getData() != null) {
				Row row = sheet
						.createRow(sheet.getLastRowNum() + 1);
				String rowName = treeItem.getText(0);
				log.debug("  +-> Row : '" + rowName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				Cell cell = row.createCell(0);
				wb.asBodyCellStyl(cell).setCellValue(indent + rowName);
				for (int j = 1; j < columnsNb; j++) {
					log.debug("  +-> Cell : " + j + ", '" + treeItem.getText(j) + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					cell = row.createCell(j);
					wb.asBodyCellStyl(cell);
					String cellValue = treeItem.getText(j);
					try {
						cell.setCellValue(Integer.parseInt(cellValue));
					} catch (NumberFormatException e0) {
						try {
							cell.setCellValue(Double.parseDouble(cellValue));
						} catch (NumberFormatException e1) {
							cell.setCellValue(cellValue);
						}
					}
				}
				if (treeItem.getExpanded())
					appendToWorkbook(
							indent + "    ", wb, sheet, treeItem.getItems(), columnsNb); //$NON-NLS-1$
			}
		}
		int endRowNum = sheet.getLastRowNum();
		log.debug("startRowNum=" + startRowNum + ", endRowNum=" + endRowNum); //$NON-NLS-1$ //$NON-NLS-2$
		if (!"".equals(indent) && (endRowNum - startRowNum >= 1)) { //$NON-NLS-1$
			log.debug(" -> grouped!"); //$NON-NLS-1$
			sheet.groupRow(startRowNum, endRowNum);
		}
	}

	/**
	 * Exporte un tableau SWT en fichier EXCEL.
	 * 
	 * @param table
	 *            le tableau à exporter.
	 * @throws UITechException
	 *             levé en cas de pb I/O lors de la sauvegarde du fichier EXCEL.
	 */
	public static void exportToWorkBook(Table table) throws UITechException {
		// Demande du nom de fichier
		FileDialog fd = new FileDialog(table.getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
		fd.setFilterExtensions(new String[] { "*.xls", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setOverwrite(true);
		String fileName = fd.open();
		// Si le nom est spécifié
		if (fileName != null) {
			try {
				// Correction du nom du fichier si besoin
				if (!fileName.endsWith(".xls")) //$NON-NLS-1$
					fileName += ".xls"; //$NON-NLS-1$
				// Sauvegarde du document
				Workbook wb = toWorkBook(table);
				FileOutputStream out = new FileOutputStream(fileName);
				wb.write(out);
				out.close();
			} catch (IOException e) {
				log.error("I/O exception", e); //$NON-NLS-1$
				throw new UITechException(
						Strings.getString("SWTHelper.errors.IO_EXCEPTION_WHILE_EXPORTING"), e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Convertit un tableau en classeur EXCEL.
	 * 
	 * @param table
	 *            le tableau à convertir.
	 * @return le classeur EXCEL.
	 */
	public static Workbook toWorkBook(Table table) {
		// Création du fichier EXCEL et du feuillet
		WorkbookBuilder wb = new WorkbookBuilder();
		Workbook workbook = wb.getWorkbook();
		Sheet sheet = workbook.createSheet(Strings
				.getString("SWTHelper.excelsheet.TAB_NAME")); //$NON-NLS-1$
		sheet.createFreezePane(0, 1, 0, 1);
		// Création de l'entête
		Row row = sheet.createRow(0);
		TableColumn[] columns = table.getColumns();
		for (int i = 0; i < columns.length; i++) {
			TableColumn column = columns[i];
			sheet.setColumnWidth(i, column.getWidth() * 50);
			Cell cell = row.createCell(i);
			wb.asHeaderCellStyl(cell).setCellValue(column.getText());
		}
		// Exportation des lignes du tableau
		TableItem[] items = table.getItems();
		appendToWorkbook(wb, sheet, items, columns.length);
		// Retour du résultat
		return workbook;
	}

	/**
	 * Génère des lignes dans le classeur EXCEL pour les éléments d'un tableau.
	 * 
	 * @param sheet
	 *            le feuillet EXCEL.
	 * @param cellStyle
	 *            le style de la cellule.
	 * @param tableItems
	 *            les élements.
	 * @param columnsNb
	 *            le nombre de colonnes à exporter dans le feuillet.
	 */
	private static void appendToWorkbook(WorkbookBuilder wb, Sheet sheet,
			TableItem[] tableItems, int columnsNb) {
		log.debug("sheet.getLastRowNum() : " + sheet.getLastRowNum()); //$NON-NLS-1$
		int startRowNum = sheet.getLastRowNum() + 1;
		for (int i = 0; i < tableItems.length; i++) {
			TableItem tableItem = tableItems[i];
			log.debug(" +-> TreeItem : " + i + ", data='" + tableItem.getData() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (tableItem.getData() != null) {
				Row row = sheet
						.createRow(sheet.getLastRowNum() + 1);
				String rowName = tableItem.getText(0);
				log.debug("  +-> Row : '" + rowName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				Cell cell = row.createCell(0);
				wb.asBodyCellStyl(cell).setCellValue(rowName);
				for (int j = 1; j < columnsNb; j++) {
					log.debug("  +-> Cell : " + j + ", '" + tableItem.getText(j) + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					cell = row.createCell(j);
					wb.asBodyCellStyl(cell);
					String cellValue = tableItem.getText(j);
					try {
						cell.setCellValue(Integer.parseInt(cellValue));
					} catch (NumberFormatException e0) {
						try {
							cell.setCellValue(Double.parseDouble(cellValue));
						} catch (NumberFormatException e1) {
							cell.setCellValue(cellValue);
						}
					}
				}
			}
		}
		int endRowNum = sheet.getLastRowNum();
		log.debug("startRowNum=" + startRowNum + ", endRowNum=" + endRowNum); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
