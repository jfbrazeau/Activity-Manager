/*
 * Copyright (c) 2004-2006, Jean-François Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import jfb.tools.activitymgr.ui.images.ImagesDatas;
import jfb.tools.activitymgr.ui.util.SafeRunner;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabItem;

/**
 * IHM contenant des informations générales sur le projet.
 */
public class AboutUI implements SelectionListener {

	/** Logger */
	private static Logger log = Logger.getLogger(AboutUI.class);

	/** Composant parent */
	private Composite parent;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public AboutUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 * @param parentComposite composant parent.
	 */
	public AboutUI(Composite parentComposite) {
		GridData data = null;
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		
		// Création du composite centré
		Composite centeredPanel = new Composite(parent, SWT.NONE);
		centeredPanel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
		centeredPanel.setLayout(new GridLayout(1, false));
		log.debug("About UI initialized");
		
		// Logo
		Label logo = new Label(centeredPanel, SWT.NONE);
		logo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		logo.setImage(new Image(parentComposite.getDisplay(), ImagesDatas.APPLICATION_LOGO));
		
		// Contact
		Link contactText = new Link(centeredPanel, SWT.NONE);
		contactText.setText("To report a bug, request a new feature, ask a question or anything else,\n" +
				"please write to <a>jfbraz@yahoo.fr</a>.");
		contactText.addSelectionListener(this);

		// Documentation
		Link documentationText = new Link(centeredPanel, SWT.NONE);
		documentationText.setText("For more information about Activity Manager, please refer to the documentation\n" +
				"at : <a>http://www.jfbrazeau.fr</a>");
		documentationText.addSelectionListener(this);

		// Apache license
		Link apacheText = new Link(centeredPanel, SWT.NONE);
		apacheText.setText("This product includes software developed by The Apache Software Foundation\n" +
				"(<a>http://www.apache.org/</a>).");
		apacheText.addSelectionListener(this);

		// Third parties licenses
		Link thirdPartiesText = new Link(centeredPanel, SWT.NONE);
		thirdPartiesText.setText("This product also includes software developped by \n" +
				"   - The Eclipse foundation (<a>http://www.eclipse.org</a>)\n" +
				"   - MySQL organization (<a>http://www.mysql.com</a>)\n" +
				"   - The HSQLDB Development Group (<a>http://www.hsqldb.org</a>)");
		thirdPartiesText.addSelectionListener(this);

		// Ajout de la license
		Label label = new Label(centeredPanel, SWT.NONE);
		label.setText("Activity Manager is distributed under a BSD License :");
		List list = new List(centeredPanel, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.MULTI);
        data = new GridData(SWT.NONE, SWT.FILL, false, true);
        list.setLayoutData(data);
		
        // Ajout de la license
        try {
        	InputStream in = AboutUI.class.getResourceAsStream("LICENSE.txt");
	        LineNumberReader lin = new LineNumberReader(new InputStreamReader(in));
	        String line = null;
	        while ((line = lin.readLine())!=null) {
	        	list.add(line);
	        }
        }
        catch (IOException e) {
        	log.error("Unexpected I/O error while printing stack trace.", e);
        	list.add("Unexpected I/O error while printing stack trace.");
        	list.add("See logs for more details.");
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e) {
		String osName = System.getProperty("os.name");
		String link = e.text;
		if (!link.startsWith("http://"))
			link = "mailto:" + link;
		final String url = link;
		boolean windows = osName!=null && osName.toLowerCase().indexOf("windows")>=0;
		boolean copyURLToClipboard = true;
		// Sous windows lancement du brower ou du client de mail
		if (windows) {
			SafeRunner runner = new SafeRunner() {
				protected Object runUnsafe() throws Exception {
					Process proc = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
					return proc;
				}
			};
			// Si le lancement échoue, on effectuera le copier/coller
			if (runner.run(parent.getShell())!=null)
				copyURLToClipboard = false;
		}
		// Sur les autres plateformes que linux, dépot dans le clipboard
		if (copyURLToClipboard) {
			Clipboard clipBoard = new Clipboard(parent.getDisplay());
			clipBoard.setContents(new Object[] { e.text },
		              new Transfer[] { TextTransfer.getInstance() });
			clipBoard.dispose();
			MessageDialog.openInformation(
					parent.getShell(), 
					"Information", 
					"Couldn't open default mail/browser desktop application.\n" +
					"'" + e.text + "' has been copied to your clipboard.\n" +
					"You may paste it in your mail/browser to proceed.");
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

}
