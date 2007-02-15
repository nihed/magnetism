package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.annotations.Index;

@Entity
@org.hibernate.annotations.Table(appliesTo = "ApplicationUsage", indexes={ 
		@Index(name="userApplication_index", columnNames = { "user_id", "application_id", "date" } ) 
})
// We need to use native SQL when making queries across all 
// TrackHistory objects for a particular track, because of combinations
// of limitations of MySQL 4 and of HQL/EJBQL. You can't use aggregates
// in HAVING / ORDER BY clauses in MySQL 4 without naming them in
// in the SELECT clause, and you can't use named aggregates in those
// locations in HQL/EJBQL.	      
@SqlResultSetMapping(name="applicationUsageAggregateMapping", 
		entities={
			// We don't need to specify field by field mappings,
		    // using FieldResult since we return only one entity, and
		    // the default name for each returned field
			@EntityResult(entityClass=Application.class)
		},
		columns={
			@ColumnResult(name="count")
		}
)
// Naming the queries allows us to keep them close to the result
// set mapping. We can also turn on caching for named queries,
// which could be helpful here, though we are't doing that yet.
@NamedNativeQueries({
	@NamedNativeQuery(name="applicationsPopularSince",
			query=
				"SELECT  " +
				"  app.id as id, " +
				"  app.name as name, " +
				"  app.description as description, " +
				"  app.category as category, " +
				"  app.rawCategories as rawCategories, " +
				"  app.titlePatterns as titlePatterns, " +
				"  COUNT(app.id) as count " + 
				"FROM ApplicationUsage au LEFT JOIN Application app on au.application_id = app.id " +
				"WHERE au.date > :since " +
				"GROUP by app.id " +
				"ORDER by count DESC",
			resultSetMapping="applicationUsageAggregateMapping"
		)
})
public class ApplicationUsage extends DBUnique {
	private Application application;
	private User user;
	private long date;
	
	public ApplicationUsage() {
	}
	
	public ApplicationUsage(User user, Application application, Date date) {
		this.user = user;
		this.application = application;
		this.date = date.getTime();
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}

	@Column(nullable = false)
	public Date getDate() {
		return new Date(date);
	}
	
	public void setDate(Date date) {
		this.date = date.getTime();
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
}
