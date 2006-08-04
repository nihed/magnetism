package com.dumbhippo.server.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class StackerBean implements Stacker {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackerBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	IdentitySpider identitySpider;
	
	@EJB
	TransactionRunner runner;
	
	private Block queryBlock(BlockType type, Guid data1, long data2) throws NotFoundException {
		Query q;
		if (data1 != null && data2 >= 0) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data1=:data1 AND block.data2=:data2");
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2);
		} else if (data1 != null) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data1=:data1");
			q.setParameter("data1", data1.toString());
		} else if (data2 >= 0) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data2=:data2");
			q.setParameter("data2", data2);
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + type);
		}
		q.setParameter("type", type);
		try {
			return (Block) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("no block with type " + type + " data1 " + data1 + " data2 " + data2, e);
		}
	} 
	
	// this doesn't retry on constraint violation since we do that for a larger transaction,
	// see below
	private Block getOrCreateUpdatingTimestamp(BlockType type, Guid data1, long data2, long activity) {
		Block block;
		try {
			block = queryBlock(type, data1, data2);
		} catch (NotFoundException e) {
			block = new Block(type, data1, data2);
			em.persist(block);
		}
		if (block.getTimestampAsLong() < activity) // never "roll back"
			block.setTimestampAsLong(activity);
		
		return block;
	}
	
	private List<UserBlockData> queryUserBlockDatas(Block block) {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd WHERE ubd.block = :block");
		q.setParameter("block", block);
		return TypeUtils.castList(UserBlockData.class, q.getResultList());
	}
	
	public void stackMusicPerson(final Guid userId, final long activity) {
		runner.runTaskRetryingOnConstraintViolation(new Runnable() {
			public void run() {
				Block block = getOrCreateUpdatingTimestamp(BlockType.MUSIC_PERSON, userId, -1, activity);
				
				// be sure we have the right UserBlockData. This would be a lot saner to do
				// when people add/remove friends, instead of here. But it would not retroactively
				// fix the existing db or let us change our rules for who gets what...
				
				List<UserBlockData> userDatas = queryUserBlockDatas(block);
				
				Map<User,UserBlockData> existing = new HashMap<User,UserBlockData>();
				for (UserBlockData ubd : userDatas) {
					existing.put(ubd.getUser(), ubd);
				}
				
				User user;
				try {
					user = EJBUtil.lookupGuid(em, User.class, userId);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
				Set<User> peopleWhoCare = identitySpider.getUsersWhoHaveUserAsContact(SystemViewpoint.getInstance(), user);
				for (User u : peopleWhoCare) {
					UserBlockData old = existing.get(u);
					if (old != null) {
						existing.remove(u);
					} else {
						UserBlockData data = new UserBlockData(u, block);
						em.persist(data);
					}
				}
				// the rest of "existing" is users who no longer are friends of the user in question
				for (User u : existing.keySet()) {
					UserBlockData old = existing.get(u);
					em.remove(old);
				}
			}
		});
	}
	
	public List<UserBlockData> getStack(Viewpoint viewpoint, User user, int start, int count) {
		if (!(viewpoint.isOfUser(user) || viewpoint instanceof SystemViewpoint))
			throw new RuntimeException("Supporting non-self viewpoints is hard here since you need to check permissions on the posts, etc.");
		
		// FIXME this is not exactly the sort order; we want to use ubd.ignoredDate in the sort if the 
		// user has ignored a block, instead of block.timestamp. However, EJBQL doesn't know how to do that.
		// maybe a native sql query or some other solution is required.
		
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE " + 
				" ubd.user = :user AND ubd.block = block ORDER BY block.timestamp DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("user", user);
		return TypeUtils.castList(UserBlockData.class, q.getResultList());
	}
}
