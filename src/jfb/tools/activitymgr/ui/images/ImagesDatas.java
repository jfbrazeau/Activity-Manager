/*
 * Copyright (c) 2004-2010, Jean-François Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.ui.images;

import org.eclipse.swt.graphics.ImageData;

/**
 * Classe de gestion des images de l'application.
 */
public interface ImagesDatas {

	/** Logo de l'application */
	public static final ImageData APPLICATION_LOGO = new ImageData(ImagesDatas.class.getResourceAsStream("logo-385x100.png")); //$NON-NLS-1$
	
	/** Icone de l'application */
	public static final ImageData APPLICATION_ICON = new ImageData(ImagesDatas.class.getResourceAsStream("logo-16x16.ico")); //$NON-NLS-1$
	
	/** Icone case cochée */
	public static final ImageData CHECKED_ICON = new ImageData(ImagesDatas.class.getResourceAsStream("checkedIcon.gif")); //$NON-NLS-1$

	/** Icone case décochée */
	public static final ImageData UNCHECKED_ICON = new ImageData(ImagesDatas.class.getResourceAsStream("uncheckedIcon.gif")); //$NON-NLS-1$

	/** Icone item sélectionné */
	public static final ImageData SELECTED_ITEM_ICON = new ImageData(ImagesDatas.class.getResourceAsStream("selectedItemIcon.gif")); //$NON-NLS-1$

	/** Icone item non sélectionné */
	public static final ImageData UNSELECTED_ITEM_ICON = new ImageData(ImagesDatas.class.getResourceAsStream("unselectedItemIcon.gif")); //$NON-NLS-1$

}
