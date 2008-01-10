package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.NowPlayingThemeSystem;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class NowPlayingThemeSystemBean implements NowPlayingThemeSystem {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(NowPlayingThemeSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;	
	
	public NowPlayingTheme getCurrentNowPlayingTheme(User user) throws NotFoundException {
		NowPlayingTheme theme = user.getAccount().getNowPlayingTheme();
		
		if (theme != null && theme.isDraft()) {
			logger.warn("User {} got a draft theme {} as their current one", user, theme);
			theme = null;
		}
		
		// try to pick a default FIXME put it in the config or something, not just "oldest theme"
		if (theme == null) {
			Query q = em.createQuery("FROM NowPlayingTheme WHERE draft=0 ORDER BY creationDate ASC");
			q.setMaxResults(1);
			try {
				theme = (NowPlayingTheme) q.getSingleResult();
			} catch (NoResultException e) {
				theme = null;
			}
		}

		if (theme == null)
			throw new NotFoundException("user has no now playing theme set and no default in the db");
		
		return theme;
	}
	
	public void setCurrentNowPlayingTheme(UserViewpoint viewpoint, User user, NowPlayingTheme theme) {
		if (!viewpoint.getViewer().equals(user))
			throw new RuntimeException("not allowed to set someone else's now playing theme");
		if (!em.contains(user))
			throw new RuntimeException("user is detached");
		if (!em.contains(theme))
			throw new RuntimeException("theme is detached");

		user.getAccount().setNowPlayingTheme(theme);
	}
	
	private NowPlayingTheme internalLookupNowPlayingTheme(String id) throws NotFoundException {
		NowPlayingTheme obj = em.find(NowPlayingTheme.class, id);
		if (obj == null)
			throw new NotFoundException("no now playing theme " + id);
		return obj;		
	}
	
	public NowPlayingTheme lookupNowPlayingTheme(String id) throws ParseException, NotFoundException {
		Guid.validate(id); // so we throw Parse instead of GuidNotFound if invalid
		return internalLookupNowPlayingTheme(id);
	}
	
	public NowPlayingTheme lookupNowPlayingTheme(Guid id) throws NotFoundException {
		return internalLookupNowPlayingTheme(id.toString());
	}
	
	public void setNowPlayingThemeImage(UserViewpoint viewpoint, String id, String type, String shaSum) throws NotFoundException, ParseException {
		User user = viewpoint.getViewer();
		NowPlayingTheme theme = lookupNowPlayingTheme(id);
		if (!theme.getCreator().equals(user))
			throw new NotFoundException("unauthorized user editing theme");
		if (type.equals("active"))
			theme.setActiveImage(shaSum);
		else if (type.equals("inactive"))
			theme.setInactiveImage(shaSum);
		else
			throw new RuntimeException("unknown theme image type '" + type + "'");
	} 
	
	public List<NowPlayingTheme> getExampleNowPlayingThemes(Viewpoint viewpoint, int maxResults) {
		// FIXME pick certain good ones or something
		// FIXME EJBQL syntax is wrong, but don't have docs handy, will fix with the others
		// FIXME ascending order for now ensures our good default themes are visible, but prevents 
		// new good themes from ever showing up - need some kind of rating system or something else dynamic
		Query q = em.createQuery("FROM NowPlayingTheme t WHERE t.draft=0 ORDER BY t.creationDate ASC");
		q.setMaxResults(maxResults); 
		return TypeUtils.castList(NowPlayingTheme.class, q.getResultList());
	}
	
	public NowPlayingTheme createNewNowPlayingTheme(UserViewpoint viewpoint, NowPlayingTheme basedOn) {
		if (basedOn != null && !em.contains(basedOn))
			basedOn = em.find(NowPlayingTheme.class, basedOn.getId()); // reattach
		NowPlayingTheme theme = new NowPlayingTheme(basedOn, viewpoint.getViewer());
		em.persist(theme);
		return theme;
	}
	
	public NowPlayingTheme getCurrentTheme(Viewpoint viewpoint, User user) {
		NowPlayingTheme current;
		try {
			current = getCurrentNowPlayingTheme(user);
		} catch (NotFoundException e) {
			current = null;
		}		
		return current;
	}
	
	private String buildGetFriendsThemesQuery(Viewpoint viewpoint, User user, Set<User> friends, boolean forCount) {
		
		if (friends.isEmpty())
			throw new RuntimeException("Trying to query for friends theme but have no friends " + user);
		
		String draftClause;
		if (viewpoint instanceof SystemViewpoint)
			draftClause = null;
		else if (viewpoint instanceof UserViewpoint)
			draftClause = "(t.draft=0 OR t.creator=:viewer)";
		else
			draftClause = "(t.draft=0)";

		StringBuilder sb = new StringBuilder("SELECT ");
		
		if (forCount) {
			sb.append("count(t)");
		} else {
			sb.append("t");
		}
		sb.append(" FROM NowPlayingTheme t ");
		/*
		 * FIXME convert to a join - this will break when a user has more than
		 * 200 contacts or so
		 */
		sb.append("WHERE t.creator.id IN (");
		for (User u : friends) {
			sb.append("'");
			sb.append(u.getId());
			sb.append("'");
			sb.append(",");
		}
		if (sb.charAt(sb.length() - 1) == ',') {
			sb.setLength(sb.length() - 1);
		}
		sb.append(") ");

		if (draftClause != null) {
			sb.append("AND ");
			sb.append(draftClause);
		}
		sb.append(" ORDER BY creationDate DESC");
		return sb.toString();
	}

	public void getFriendsThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable) {
		// this will return no friends if we can't see this person's contacts
		// from our viewpoint
		Set<User> friends = identitySpider.getRawUserContacts(viewpoint, user);
		
		if (friends.isEmpty()) {
			pageable.setResults(new ArrayList<NowPlayingTheme>());
			pageable.setTotalCount(0);
			return;
		}
		
		Query q = em.createQuery(buildGetFriendsThemesQuery(viewpoint, user, friends, false));
		if (viewpoint instanceof UserViewpoint) {			
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());
		}
			
		q.setMaxResults(pageable.getCount());
		q.setFirstResult(pageable.getStart());
		pageable.setResults(TypeUtils.castList(NowPlayingTheme.class, q.getResultList()));
		q = em.createQuery(buildGetFriendsThemesQuery(viewpoint, user, friends, true));
		if (viewpoint instanceof UserViewpoint) {			
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());
		}		
		pageable.setTotalCount(((Number) q.getSingleResult()).intValue());			
	}

	public void getMyThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable) {
		// this query will include our draft themes
		assert(viewpoint.isOfUser(user));
		Query q = em.createQuery(buildGetThemesQuery(viewpoint, user, false));
		q.setParameter("creator", user);
		q.setMaxResults(pageable.getCount());
		q.setFirstResult(pageable.getStart());
		pageable.setResults(TypeUtils.castList(NowPlayingTheme.class, q.getResultList()));
		q = em.createQuery(buildGetThemesQuery(viewpoint, user, true));
		q.setParameter("creator", user);		
		pageable.setTotalCount(((Number) q.getSingleResult()).intValue());		
	}
	
	private String buildGetThemesQuery(Viewpoint viewpoint, User creator, boolean forCount) {
		StringBuilder sb = new StringBuilder("SELECT ");
		if (forCount)
			sb.append("count(t)");
		else
			sb.append("t");
		sb.append(" FROM NowPlayingTheme t WHERE t.draft=0 ");
		if (creator != null)
			sb.append(" AND t.creator=:creator ");
		sb.append(" ORDER BY creationDate DESC");
		return sb.toString();
	}

	public void getAllThemes(Viewpoint viewpoint, Pageable<NowPlayingTheme> pageable) {
		Query q = em.createQuery(buildGetThemesQuery(viewpoint, null, false));
		q.setMaxResults(pageable.getCount());
		q.setFirstResult(pageable.getStart());
		pageable.setResults(TypeUtils.castList(NowPlayingTheme.class, q.getResultList()));
		q = em.createQuery(buildGetThemesQuery(viewpoint, null, true));
		pageable.setTotalCount(((Number) q.getSingleResult()).intValue());
	}
	
}
