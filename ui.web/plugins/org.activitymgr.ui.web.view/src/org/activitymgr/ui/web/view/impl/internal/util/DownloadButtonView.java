package org.activitymgr.ui.web.view.impl.internal.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IDownloadButtonLogic.View;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;

@SuppressWarnings("serial")
public class DownloadButtonView extends Button implements View {

	@SuppressWarnings("unused")
	private IDownloadButtonLogic logic;
	
	@Inject
	private IResourceCache resourceCache;

	@Override
	public void setIcon(String iconId) {
		setIcon(resourceCache.getResource(iconId + ".gif"));
	}

	@Override
	public void registerLogic(final IDownloadButtonLogic logic) {
		this.logic = logic;
		StreamResource streamResource = new StreamResource(new StreamResource.StreamSource() {
			@Override
			public InputStream getStream() {
				return new ByteArrayInputStream(logic.getContent());
			}
		}, logic.getFileName()) {
			/**
			 * Override getFileName in order not to get a static name (the one that is given in the constructor).
			 */
			@Override
			public String getFilename() {
				return logic.getFileName();
			}
		};
		FileDownloader downloader = new FileDownloader(streamResource);
		downloader.extend(this);
	}


}
