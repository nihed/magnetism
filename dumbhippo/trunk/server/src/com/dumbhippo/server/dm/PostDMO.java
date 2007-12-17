package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@DMO(classId="http://mugshot.org/p/o/post", resourceBase="/o/post")
@DMFilter("viewer.canSeePost(this)")
public abstract class PostDMO extends DMObject<Guid> {
	@EJB
	PostingBoard postingBoard; 
	
	@Inject
	DMSession session;

	@Inject
	Viewpoint viewpoint;

	private Post post;
	
	protected PostDMO(Guid key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), getKey());
	}
	
	@DMProperty(defaultInclude=true)
	public UserDMO getPoster() {
		User poster = post.getPoster();
		if (poster != null)
			return session.findUnchecked(UserDMO.class, post.getPoster().getGuid());
		else
			return null;
	}
	
	@DMProperty(defaultInclude=true)
	public String getTitle() {
		return post.getTitle();
	}

	@DMProperty(defaultInclude=true)
	public String getText() {
		return post.getText();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getLink() {
		return post.getUrl().toString();
	}
	
	@DMProperty(defaultInclude=true)
	public long getDate() {
		return post.getPostDate().getTime();
	}

	@DMProperty(defaultInclude=true, defaultChildren="+")
	@DMFilter("viewer.canSeePrivate(any)")
	public List<UserDMO> getUserRecipients() {
		List<UserDMO> result = new ArrayList<UserDMO>();
		
		for (Resource resource : post.getPersonRecipients()) {
			AccountClaim accountClaim = resource.getAccountClaim();
			if (accountClaim != null)
				result.add(session.findUnchecked(UserDMO.class, accountClaim.getOwner().getGuid()));
		}
		
		return result;
	}

	@DMProperty
	public boolean isPublic() {
		return post.getVisibility() == PostVisibility.ATTRIBUTED_PUBLIC;
	}

	@DMProperty
	public Set<UserDMO> getExpandedRecipients() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (Resource resource : post.getExpandedRecipients()) {
			AccountClaim accountClaim = resource.getAccountClaim();
			if (accountClaim != null)
				result.add(session.findUnchecked(UserDMO.class, accountClaim.getOwner().getGuid()));
		}
		
		return result;
	}
}
