package com.dumbhippo.server.syndication;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleGenerator;

/**
 * Generator for the Rhapsody RSS module for Rome.
 */
public class RhapModuleGenerator  implements ModuleGenerator {
	private static final Namespace RHAP_NS  = Namespace.getNamespace(RhapModule.PREFIX, RhapModule.URI);
	
	public String getNamespaceUri() {
		return RhapModule.URI;
	}
	
	private static final Set NAMESPACES;
	
	static {
		Set<Namespace> nss = new HashSet<Namespace>();
		nss.add(RHAP_NS);
		NAMESPACES = Collections.unmodifiableSet(nss);
	}
	
	public Set getNamespaces() {
		return NAMESPACES;
	}
	
	public void generate(Module module, Element element) {
		
		// this is not necessary, it is done to avoid the namespace definition in every item.
		Element root = element;
		while (root.getParent()!=null && root.getParent() instanceof Element) {
			root = (Element) element.getParent();
		}
		root.addNamespaceDeclaration(RHAP_NS);
		
		RhapModule rm = (RhapModule)module;
		
		if (rm.getArtist() != null) {
			element.addContent(generateSimpleElement("artist", rm.getArtist()));
		}
		if (rm.getArtistRCID() != null) {
			element.addContent(generateSimpleElement("artist-rcid", rm.getArtistRCID()));
		}
		if (rm.getAlbum() != null) {
			element.addContent(generateSimpleElement("album", rm.getAlbum()));
		}
		if (rm.getAlbumRCID() != null) {
			element.addContent(generateSimpleElement("album-rcid", rm.getAlbumRCID()));
		}
		if (rm.getTrack() != null) {
			element.addContent(generateSimpleElement("track", rm.getTrack()));
		}
		if (rm.getTrackRCID() != null) {
			element.addContent(generateSimpleElement("track-rcid", rm.getTrackRCID()));
		}
		if (rm.getDuration() != null) {
			element.addContent(generateSimpleElement("duration", rm.getDuration()));
		}
		if (rm.getPlayHref() != null) {
			element.addContent(generateSimpleElement("play-href", rm.getPlayHref()));
		}
		if (rm.getDataHref() != null) {
			element.addContent(generateSimpleElement("data-href", rm.getDataHref()));
		}
		if (rm.getAlbumArt() != null) {
			element.addContent(generateSimpleElement("album-art", rm.getAlbumArt()));
		}
	}
	
	protected Element generateSimpleElement(String name, String value)  {
		Element element = new Element(name, RHAP_NS);
		element.addContent(value);
		return element;
	}
	
}
