package com.dumbhippo.web.tags;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.jstl.core.LoopTagSupport;

import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.web.BrowserBean;

/**
 * Iterates over downloads for a track that we think should be displayed
 * according to the user's browser.
 * 
 * @author otaylor
 */
public class ForEachDownload extends LoopTagSupport {
	private static final long serialVersionUID = 1L;
	
	TrackView track;
	Iterator<Entry<SongDownloadSource, String>> iterator;
	DownloadInfo nextDownload = null;
	
	public static class DownloadInfo {
		private SongDownloadSource source;
		private String url;

		public DownloadInfo(SongDownloadSource source, String url) {
			this.source = source;
			this.url = url;
		}

		public SongDownloadSource getSource() {
			return source;
		}

		public String getUrl() {
			return url;
		}
	}
	
	private boolean checkVisible(SongDownloadSource source) {
		HttpServletRequest request = (HttpServletRequest)(pageContext.getRequest());
		BrowserBean browser = BrowserBean.getForRequest(request);
		
		switch (source) {
		case ITUNES:
			return browser.isWindows() || browser.isMac();
		case YAHOO:
			return browser.isWindows();
		case RHAPSODY:
			return true;
		default:
			return true;
		}
	}
	
	private boolean ensureNextDownload() {
		if (nextDownload != null)
			return true;
		
		while (iterator.hasNext()) {
			Entry<SongDownloadSource, String> entry = iterator.next();
			if (checkVisible(entry.getKey())) {
				nextDownload = new DownloadInfo(entry.getKey(), entry.getValue());
				return true;
			}
		}
		
		return false;
	}

	public void setTrack(TrackView track) {
		if (track == null)
			throw new NullPointerException("Track must not be null");
		this.track = track;
	}
	
	@Override
	protected boolean hasNext() {
		return ensureNextDownload();
	}

	@Override
	protected Object next() {
		if (!ensureNextDownload()) 
			throw new NoSuchElementException();

		Object result = nextDownload;
		nextDownload = null;
		
		return result;
	}

	@Override
	protected void prepare() {
		iterator = track.getDownloads().entrySet().iterator();
	}
}
