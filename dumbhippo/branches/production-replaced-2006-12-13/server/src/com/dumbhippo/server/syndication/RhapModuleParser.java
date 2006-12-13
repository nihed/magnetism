
package com.dumbhippo.server.syndication;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

/**
 * Parser for the Rhapsody RSS module for Rome.
 */
public class RhapModuleParser implements ModuleParser {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(RhapModuleParser.class);
	
	private static final Namespace RHAP_NS  = Namespace.getNamespace(RhapModule.PREFIX, RhapModule.URI);
	
	public String getNamespaceUri() {
		return RhapModule.URI;
	}
	
	public RhapModuleParser() {
		super();
	}
	
	/*
	 * Format of rhapsody module content:
	 * 
	 * <rhap:artist xmlns:rhap="rhap">Sleater-Kinney</rhap:artist>
	 * <rhap:artist-rcid xmlns:rhap="rhap">art.42625</rhap:artist-rcid>
	 * <rhap:album xmlns:rhap="rhap">Call The Doctor</rhap:album>
	 * <rhap:album-rcid xmlns:rhap="rhap">alb.213012</rhap:album-rcid>
	 * <rhap:track xmlns:rhap="rhap">Call The Doctor</rhap:track>
	 * <rhap:track-rcid xmlns:rhap="rhap">tra.2025382</rhap:track-rcid>
	 * <rhap:duration xmlns:rhap="rhap">150000</rhap:duration>
	 * <rhap:play-href xmlns:rhap="rhap">
	 *     http://play.rhapsody.com/sleaterkinney/callthedoctor/track-1
	 * </rhap:play-href>
	 * <rhap:data-href xmlns:rhap="rhap">
	 *     http://feeds.rhapsody.com/track-data.xml?trackId=tra.2025382
	 * </rhap:data-href>
	 * <rhap:album-art xmlns:rhap="rhap">
	 *     http://image.listen.com/img/170x170/4/9/3/0/790394_170x170.jpg
	 * </rhap:album-art>
	 */
	
	public Module parse(Element root) {
		boolean foundSomething = false;
		RhapModule rm = new RhapModuleImpl();
		
		List eList = root.getChildren("artist", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setArtist(getElementFromList(eList));
		}
		eList = root.getChildren("artist-rcid", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setArtistRCID(getElementFromList(eList));
		}
		eList = root.getChildren("album", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setAlbum(getElementFromList(eList));
		}
		eList = root.getChildren("album-rcid", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setAlbumRCID(getElementFromList(eList));
		}
		eList = root.getChildren("track", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setTrack(getElementFromList(eList));
		}
		eList = root.getChildren("track-rcid", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setTrackRCID(getElementFromList(eList));
		}
		eList = root.getChildren("duration", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setDuration(getElementFromList(eList));
		}
		eList = root.getChildren("play-href", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setPlayHref(getElementFromList(eList));
		}
		eList = root.getChildren("data-href", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setDataHref(getElementFromList(eList));
		}
		eList = root.getChildren("album-art", RHAP_NS);
		if (eList.size() > 0) {
			foundSomething = true;
			rm.setAlbumArt(getElementFromList(eList));
		}
		
		return (foundSomething) ? rm : null;
	}
	
	protected final String getElementFromList(List eList) {
		for (Iterator i = eList.iterator(); i.hasNext();) {
			Element e = (Element) i.next();
			return e.getText();
		}
		return null;
	}
	
}
