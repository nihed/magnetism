package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.EmbeddedGuidPersistable;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.ViewStreamBuilder;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.FeedView;
import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.ViewStream;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class ViewStreamBuilderBean implements ViewStreamBuilder {
	@EJB
	private GroupSystem groupSystem;

	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private PostingBoard postingBoard;
	
	private Guid getObjectGuid(Object object) {
		if (object instanceof ObjectView)
			return ((ObjectView)object).getIdentifyingGuid();
		else if (object instanceof GuidPersistable)
			return ((GuidPersistable)object).getGuid();
		else if (object instanceof EmbeddedGuidPersistable)
			return ((EmbeddedGuidPersistable)object).getGuid();
		else
			throw new RuntimeException("Don't know how to get a GUID for " + object);
	}
	
	private ObjectView getObjectView(Viewpoint viewpoint, Object object) {
		if (object instanceof ObjectView)
			return (ObjectView)object;
		else if (object instanceof GroupFeed)
			return new FeedView((GroupFeed)object);
		else if (object instanceof Group)
			return groupSystem.getGroupView(viewpoint, (Group)object);
		else if (object instanceof Person)
			return personViewer.getPersonView(viewpoint, (Person)object, PersonViewExtra.PRIMARY_RESOURCE);
		else if (object instanceof Resource)
			return personViewer.getPersonView(viewpoint, (Resource)object, PersonViewExtra.PRIMARY_RESOURCE);
		else if (object instanceof Post)
			return postingBoard.getPostView(viewpoint, (Post)object);
		else
			throw new RuntimeException("Don't know how to get a ObjectView for " + object);
	}
	
	private void addViewToStream(ViewStream stream, Viewpoint viewpoint, ObjectView objectView, Set<Guid> visited) {
		Guid id = objectView.getIdentifyingGuid();
		
		if (stream.hasObjectView(id))
			return;
		
		visited.add(id);
		
		for (Object referencedObject : objectView.getReferencedObjects()) {
			Guid referencedId = getObjectGuid(referencedObject);
			
			if (stream.hasObjectView(referencedId))
				continue;
			else if (visited.contains(referencedId))
				throw new RuntimeException("Encountered cycle when building object stream");
			
			addViewToStream(stream, viewpoint, getObjectView(viewpoint, referencedObject), visited);
		}
		
		stream.addObjectView(objectView);
	}
	
	public ViewStream buildStream(Viewpoint viewpoint, ObjectView objectView) {
		ViewStream stream = new ViewStream();
		Set<Guid> visited = new HashSet<Guid>();
		
		addViewToStream(stream, viewpoint, objectView, visited);
		
		return stream;
	}

	public ViewStream buildStream(Viewpoint viewpoint, List<ObjectView> objectViews) {
		ViewStream stream = new ViewStream();
		Set<Guid> visited = new HashSet<Guid>();
		
		for (ObjectView objectView : objectViews)
			addViewToStream(stream, viewpoint, objectView, visited);
		
		return stream;
	}
}
