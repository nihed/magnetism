package com.dumbhippo.persistence.tomboy;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;

@Entity
@Table(name="NoteRepository", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"uuid"})}
		   )
public class NoteRepository extends EmbeddedUuidPersistable {
		@SuppressWarnings("unused")
		private static final Logger logger = GlobalSetup.getLogger(NoteRepository.class);	
		
		private static final long serialVersionUID = 0L;
		
		private User owner;
		private int revision;
		private Set<NoteRevision> noteRevisions;
		
		protected NoteRepository() {
			this(null);
		}
		
		public NoteRepository(User owner) {
			revision = 0;
			noteRevisions = new HashSet<NoteRevision>();
			if (owner != null) {
				this.owner = owner;
				
				// we don't do a back-link since usually when loading a User we aren't 
				// doing anything with Tomboy
				//owner.setNoteRepository(this);
			}
		}
		
		@OneToOne
		@JoinColumn(nullable=false)
		public User getOwner() {
			return owner;
		}
		
		/**
		 * This is protected because calling it is probably 
		 * a bad idea. (Give someone else your repository?)
		 * 
		 * @param owner The owner to set.
		 */
		protected void setOwner(User owner) {
			this.owner = owner;
		}
		

		@Column(nullable=false)
		public int getRevision() {
			return revision;
		}

		public void setRevision(int revision) {
			this.revision = revision;
		}
		
		@OneToMany(mappedBy="repository", fetch=FetchType.EAGER)
		@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
		public Set<NoteRevision> getNoteRevisions() {
			if (noteRevisions == null)
				throw new RuntimeException("no note revisions set???");
			return noteRevisions;
		}
		
		/**
		 * This is protected because only Hibernate probably 
		 * needs to call it.
		 * 
		 * @param noteRevisions
		 */
		protected void setNoteRevisions(Set<NoteRevision> noteRevisions) {
			if (noteRevisions == null)
				throw new IllegalArgumentException("null note revisions");
			this.noteRevisions = noteRevisions;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("{NoteRepository " + getId() + " owner = ");
			if (owner != null)
				builder.append(owner.toString());
			else 
				builder.append("null");
			
			builder.append(" uuid = " + getUuid());
			
			builder.append("}");
			
			return builder.toString();
		}
}
