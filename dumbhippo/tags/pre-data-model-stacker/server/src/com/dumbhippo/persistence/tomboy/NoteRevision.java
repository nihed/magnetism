package com.dumbhippo.persistence.tomboy;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

@Entity
// if uuids are truly globally unique then the repository_id here is redundant, I guess
@Table(name="NoteRevision", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"repository_id", "uuid", "revision"})}
		   )
public class NoteRevision extends EmbeddedUuidPersistable {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(NoteRevision.class);	
	
	private static final long serialVersionUID = 0L;
	
	private NoteRepository repository;
	private int revision;
	private long creationDate;
	private String title;
	
	protected NoteRevision() {
		this(null);
	}
	
	public NoteRevision(NoteRepository repository) {
		creationDate = -1;
		if (repository != null) {
			this.repository = repository;
		}
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public NoteRepository getRepository() {
		return repository;
	}
	
	protected void setRepository(NoteRepository repository) {
		this.repository = repository;
	}

	@Column(nullable=false)
	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}
	
	@Column(nullable=false)
	public Date getCreationDate() {
		if (creationDate < 0)
			creationDate = System.currentTimeMillis();
		return new Date(creationDate);
	}

	protected void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}

	@Column(nullable=false)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{NoteRevision " + getId());
		
		builder.append(" rev = " + getRevision());
		builder.append(" uuid = " + getUuid());
		
		builder.append("}");
		
		return builder.toString();
	}
}
