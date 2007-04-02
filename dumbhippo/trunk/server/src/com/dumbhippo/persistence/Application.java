package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.util.EJBUtil;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class Application {
	private String id;
	private boolean deleted;
	private String name;
	private String genericName;
	private String tooltip;
	private String description1;
	private String description2;
	private String description3;
	private String description4;
	private ApplicationCategory category;
	private String titlePatterns;
	private String desktopNames;
	private Set<ApplicationIcon> icons;
	private String packageNames;
	private int rank;
	private int usageCount;
	
	// for hibernate
	public Application() {
	}
	
	public Application(String id) {
		this.id = id;
	}
	
	@Id
	@Column(nullable = false, length=64)
	public String getId() {
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	@Column(nullable = false)
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Column(nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(nullable = true)
	public String getGenericName() {
		return genericName;
	}

	public void setGenericName(String genericName) {
		this.genericName = genericName;
	}

	@Column(nullable = true)
	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	@Column(nullable = true)
	protected String getDescription1() {
		return description1;
	}

	protected void setDescription1(String description) {
		this.description1 = description;
	}

	@Column(nullable = true)
	protected String getDescription2() {
		return description2;
	}

	protected void setDescription2(String description) {
		this.description2 = description;
	}

	@Column(nullable = true)
	protected String getDescription3() {
		return description3;
	}

	protected void setDescription3(String description) {
		this.description3 = description;
	}

	@Column(nullable = true)
	protected String getDescription4() {
		return description4;
	}

	protected void setDescription4(String description) {
		this.description4 = description;
	}
	
	@Transient
	public String getDescription() {
		if (description1 == null)
			return null;
		
		StringBuilder sb = new StringBuilder(description1);
		if (description2 != null)
			sb.append(description2);
		if (description3 != null)
			sb.append(description3);
		if (description4 != null)
			sb.append(description4);
		
		return sb.toString();
	}

	@Transient
	public void setDescription(String description) {
		if (description == null) {
			description1 = null;
			description2 = null;
			description3 = null;
			description4 = null;
			
			return;
		}
		
		int len = description.length();
		
		description1 = description.substring(0, Math.min(255, len));
		if (len > 255)
			description2 = description.substring(255, Math.min(510, len));
		if (len > 510)
			description3 = description.substring(510, Math.min(765, len));
		if (len > 765)
			description4 = description.substring(765, Math.min(1020, len));
	}
	
	@Transient
	public String getDescriptionAsHtml() {
		String description = getDescription();
		if (description == null)
			return "";
		
        XmlBuilder xml = new XmlBuilder();
        xml.appendTextAsHtml(description, null);
        return xml.toString(); 
	}
	
	@Column(nullable = false)
	public ApplicationCategory getCategory() {
		return category;
	}

	public void setCategory(ApplicationCategory category) {
		this.category = category;
	}

	@Column(nullable = true)
	public String getTitlePatterns() {
		return titlePatterns;
	}

	public void setTitlePatterns(String titlePatterns) {
		this.titlePatterns = titlePatterns;
	}
	
	@Column(nullable = true)
	public String getDesktopNames() {
		return desktopNames;
	}

	public void setDesktopNames(String desktopNames) {
		this.desktopNames = desktopNames;
	}
	
	@Column(nullable = true)
	public String getPackageNames() {
		return packageNames;
	}
	
	public void setPackageNames(String packageNames) {
		this.packageNames = packageNames;
	}
	
	@OneToMany(mappedBy="application")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<ApplicationIcon> getIcons() {
		if (icons == null)
			icons = new HashSet<ApplicationIcon>();
		
		return icons;
	}
	
	protected void setIcons(Set<ApplicationIcon> icons) {
		if (icons == null)
			throw new NullPointerException();
		this.icons = icons;
	}
	
	@Column(nullable = false)
	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	@Column(nullable = false)
	public int getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(int usage) {
		this.usageCount = usage;
	}

	/**
	 * Bug work around, see docs for EJBUtil.forceInitialization()
	 */
	public void prepareToModifyIcons() {
		EJBUtil.forceInitialization(icons);
	}
}
