package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="ApplicationWmClass", 
        uniqueConstraints = 
           {@UniqueConstraint(columnNames={"application_id", "wmClass"})}
   )
public class ApplicationWmClass extends DBUnique {
	private Application application;
	private String wmClass;
	
	public ApplicationWmClass() {
	}
	
	public ApplicationWmClass(Application application, String wmClass) {
		this.application = application;
		this.wmClass = wmClass;
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	@Column(nullable = false, length=127)
	public String getWmClass() {
		return wmClass;
	}
	
	public void setWmClass(String wmClass) {
		this.wmClass = wmClass;
	}
}
