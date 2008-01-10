/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;


/**
 * @author hp
 *
 */
@Entity
public class LinkResource extends Resource {
	
	private static final long serialVersionUID = 0L;

	private String url;
	
	protected LinkResource() {}

	public LinkResource(String url) {
		this.url = url;
	}
	
	@Column(unique=true, nullable=false)
	public String getUrl() {
		return url;
	}
	
	/**
	 * This is protected so only the container calls it. 
	 * This is because LinkResource is treated as immutable,
	 * i.e. once a GUID-URL pair exists, we never 
	 * change the URL text associated with that GUID. 
	 * So you don't want to setUrl(). Instead, create
	 * a new LinkResource with the new url.
	 * 
	 * @param url the url
	 */
	protected void setUrl(String url) {
		this.url = url;
	}

	@Override
	@Transient
	public String getHumanReadableString() {
		return getUrl();
	}
	
	@Override
	@Transient
	public String getDerivedNickname() {
		return getHumanReadableString();
	}
}
