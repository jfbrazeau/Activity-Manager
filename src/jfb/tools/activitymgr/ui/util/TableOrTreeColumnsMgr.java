/*
 * Copyright (c) 2004-2010, Jean-Fran�ois Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.ui.util;

import java.util.ArrayList;
import java.util.List;

import jfb.tools.activitymgr.core.util.Strings;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Classe de base des gestionnaires de colonnes de tableau ou d'arbres.
 */
public class TableOrTreeColumnsMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(TableOrTreeColumnsMgr.class);

	/** Liste des colonnes */
	private List<Column> columns = new ArrayList<Column>();

	/**
	 * Configure les colonnes de l'arbre.
	 * 
	 * @param treeViewer
	 *            le viewer de l'arbre.
	 */
	public void configureTree(TreeViewer treeViewer) {
		log.debug("configureTree"); //$NON-NLS-1$
		treeViewer.setColumnProperties(getColumnCodes());
		Tree tree = treeViewer.getTree();
		Column[] columns = getColumns();
		for (int i = 0; i < columns.length; i++) {
			Column column = columns[i];
			TreeColumn treeColumn = new TreeColumn(tree, column.alignment);
			treeColumn.setText(column.name);
			treeColumn.setWidth(column.width);
		}
	}

	/**
	 * Configure les colonnes d'un tableau.
	 * 
	 * @param tableViewer
	 *            le viewer du tableau.
	 */
	public void configureTable(TableViewer tableViewer) {
		log.debug("configureTable"); //$NON-NLS-1$
		tableViewer.setColumnProperties(getColumnCodes());
		Table table = tableViewer.getTable();
		Column[] columns = getColumns();
		for (int i = 0; i < columns.length; i++) {
			Column column = columns[i];
			TableColumn tableColumn = new TableColumn(table, column.alignment);
			tableColumn.setText(column.name);
			tableColumn.setWidth(column.width);
		}
	}

	/**
	 * Ajoute une colonne.
	 * 
	 * @param columnCode
	 *            le code de colonne.
	 * @param columnName
	 *            le nom de la colonne.
	 * @param columnWidth
	 *            la largeur de la colonne.
	 * @param columnAlignment
	 *            l'alignement � appliquer � la colonne.
	 */
	public void addColumn(String columnCode, String columnName,
			int columnWidth, int columnAlignment) {
		Column newColumn = new Column();
		if (columnName == null)
			throw new NullPointerException(
					Strings.getString("TableOrTreeColumnsMgr.errors.COLUMN_NAME_REQUIRED")); //$NON-NLS-1$
		newColumn.code = columnCode;
		newColumn.name = columnName;
		newColumn.width = columnWidth;
		newColumn.alignment = columnAlignment;
		columns.add(newColumn);
	}

	/**
	 * @return le code associ� � la colonne dont le N� est sp�cifi�.
	 * @param columnIndex
	 *            num�ro de la colonne.
	 */
	public String getColumnCode(int columnIndex) {
		String code = ((Column) columns.get(columnIndex)).code;
		log.debug("getColumnCode(" + columnIndex + ")=" + code); //$NON-NLS-1$ //$NON-NLS-2$
		return code;
	}

	/**
	 * @return le num�ro associ� � la colonne dont le code est sp�cifi�.
	 * @param columnCode
	 *            code de la colonne.
	 */
	public int getColumnIndex(String columnCode) {
		int index = -1;
		for (int i = 0; i < columns.size() && index < 0; i++) {
			Column column = (Column) columns.get(i);
			if (column.code.equals(columnCode))
				index = i;
		}
		log.debug("getColumnIndex(" + columnCode + ")=" + index); //$NON-NLS-1$ //$NON-NLS-2$
		return index;
	}

	/**
	 * @return la liste des codes des colonnes.
	 */
	private String[] getColumnCodes() {
		int n = columns.size();
		String[] columnNames = new String[n];
		for (int i = 0; i < n; i++)
			columnNames[i] = getColumnCode(i);
		return columnNames;
	}

	/**
	 * @return Retourne la liste des colonnes.
	 */
	private Column[] getColumns() {
		return (Column[]) columns.toArray(new Column[columns.size()]);
	}

}

/**
 * Bean contenant les attributs dune colonne.
 */
class Column {
	String code;
	String name;
	int width;
	int alignment;
}
