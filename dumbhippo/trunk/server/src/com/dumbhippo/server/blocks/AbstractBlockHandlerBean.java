package com.dumbhippo.server.blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

public abstract class AbstractBlockHandlerBean<BlockViewSubType extends BlockView> implements BlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(AbstractBlockHandlerBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	protected EntityManager em;
	
	@EJB
	protected IdentitySpider identitySpider;
	
	@EJB
	protected GroupSystem groupSystem;
	
	@EJB
	@IgnoreDependency
	protected Stacker stacker;
	
	private Class<BlockViewSubType> viewClass;
	private Constructor<BlockViewSubType> viewClassConstructor;
	
	protected AbstractBlockHandlerBean(Class<BlockViewSubType> viewClass) {
		this.viewClass = viewClass;
		try {
			this.viewClassConstructor = viewClass.getConstructor(Viewpoint.class, Block.class, UserBlockData.class);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		if (this.viewClassConstructor == null)
			logger.debug("{} must override createBlockView since it lacks the expected constructor for its view class", this.getClass().getName());
	}
	
	public BlockViewSubType getUnpopulatedBlockView(Viewpoint viewpoint, Block block,
			UserBlockData ubd) throws BlockNotVisibleException {
		UserViewpoint userview = null;
		if (viewpoint instanceof UserViewpoint)
			userview = (UserViewpoint) viewpoint;
		
		if (block.getInclusion() == StackInclusion.ONLY_WHEN_VIEWING_SELF) {
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			if (userview == null || !userview.getViewer().equals(user)) {
				// FIXME should this be a RuntimeException? logger.warn is here so we can investigate
				// that if the exception ever really happens
				logger.warn("Trying to view an ONLY_WHEN_VIEWING_SELF block from a different viewpoint: {} block={}", userview, block);
				throw new BlockNotVisibleException("ONLY_WHEN_VIEWING_SELF block is not visible to non-self viewpoint");
			}
		}
		
		BlockViewSubType blockView = createBlockView(viewpoint, block, ubd);
		
		checkBlockViewVisible(blockView);
		
		return blockView;
	}

	/**
	 * Creates a new block view. This should just create the object, not fill it in at all.
	 * There are two later stages for filling it in; checkBlockViewVisible() will be called
	 * to see if the viewpoint can view the block view, and then populateBlockViewImpl() 
	 * will be called to initialize any additional details on the block view if it is
	 * visible.
	 * 
	 * The default implementation just constructs an instance of viewClass.
	 * 
	 * @param viewpoint
	 * @param block
	 * @param ubd
	 * @return new block view
	 */
	protected BlockViewSubType createBlockView(Viewpoint viewpoint, Block block,
			UserBlockData ubd) {
		if (viewClassConstructor == null)
			throw new IllegalStateException("Must override createBlockView if your block view type doesn't have the right constructor");

		try {
			return viewClassConstructor.newInstance(viewpoint, block, ubd);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The default implementation of this returns immediately if block.isPublicBlock(), otherwise
	 * tries to call populateBlockViewImpl() and lets that throw BlockNotVisibleException if appropriate.
	 * Subclasses may be able to do something more efficient than a full populateBlockViewImpl().
	 * 
	 * @param blockView the block view which may or may not be visible to its viewpoint
	 * @throws BlockNotVisibleException if blockView's viewpoint can't see this blockView
	 */
	protected void checkBlockViewVisible(BlockViewSubType blockView) throws BlockNotVisibleException {
		Block block = blockView.getBlock();
	    if (!block.isPublicBlock()) {
			populateBlockViewImpl(blockView); // throws if we can't see the block contents
	    }
	}
	
	/**
	 * Fill in a block view. Called by the default implementation of checkBlockViewVisible() and also
	 * by the default implementation of populateBlockView() - thus may be called twice. The second
	 * call will be skipped if blockView.isPopulated(), so set that flag once you fully populate.
	 *   
	 * @param blockView a block view
	 * @throws BlockNotVisibleException if the block view is not visible to its viewpoint
	 */
	protected abstract void populateBlockViewImpl(BlockViewSubType blockView)
			throws BlockNotVisibleException;

	// this is final because you need to override populateBlockViewImpl instead.
	public final void populateBlockView(BlockView blockView) {
		try {
			if (!blockView.isPopulated())
				populateBlockViewImpl(viewClass.cast(blockView));
		} catch (BlockNotVisibleException e) {
			logger.warn("populateBlockView() should not run into a BlockNotVisibleException because prepareBlockView() should have done it {}",
					blockView);
			throw new RuntimeException("prepareBlockView missed a visibility check", e);
		}
	}
	
	/**
	 * Utility helper for implementing getInterestedUsers() that gets everyone with 
	 * the user id in data1 listed in their contacts.
	 * 
	 * Returns only the person themselves in the set if block.getInclusion() == ONLY_WHEN_VIEWING_SELF.
	 * 
	 * @param block
	 * @return
	 */
	protected final Set<User> getUsersWhoCareAboutData1User(Block block) {
		User user;
		try {
			user = EJBUtil.lookupGuid(em, User.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException("invalid user in data1 of " + block, e);
		}
		
		Set<User> peopleWhoCare = null;
		
		switch (block.getInclusion()) {
		case IN_ALL_STACKS:
		case ONLY_WHEN_VIEWED_BY_OTHERS:
			peopleWhoCare = identitySpider.getUsersWhoHaveUserAsContact(SystemViewpoint.getInstance(), user);
		
			// we also show each block to its "owner"
			peopleWhoCare.add(user);
			return peopleWhoCare;
		case ONLY_WHEN_VIEWING_SELF:
			peopleWhoCare = Collections.singleton(user);
			return peopleWhoCare;
			// no default, it hides bugs
		}
		
		throw new RuntimeException("invalid inclusion " + block);
	}

	/**
	 * Utility helper for implementing getInterestedUsers() that gets everyone in 
	 * the group identified by the group id in data1.
	 * 
	 * @param block
	 * @return
	 */
	protected final Set<User> getUsersWhoCareAboutData1Group(Block block) {
		Group group;
		try {
			group = EJBUtil.lookupGuid(em, Group.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException("invalid group in data1 of " + block, e);
		}
		Set<User> groupMembers = groupSystem.getUserMembers(SystemViewpoint.getInstance(), group);
		return groupMembers;
	}
	
	/**
	 * Utility helper for implementing getInterestedGroups() that returns 
	 * a single-member set with the group stored in data1.
	 * 
	 * @param block
	 * @return
	 */
	protected Set<Group> getData1GroupAsSet(Block block) {
		Group group;
		try {
			group = EJBUtil.lookupGuid(em, Group.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		return Collections.singleton(group);
	}
	
	/** 
	 * Utility helper for implementing getInterestedGroups() that returns
	 * the set of groups the user id stored in data1 is a member of.
	 * 
	 * Returns an empty set of groups if the block's inclusion is 
	 * ONLY_WHEN_VIEWING_SELF.
	 * 
	 * @param block
	 * @return
	 */
	protected Set<Group> getGroupsData1UserIsIn(Block block) {
		User user;
		try {
			user = EJBUtil.lookupGuid(em, User.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		
		switch (block.getInclusion()) {
		case IN_ALL_STACKS:
		case ONLY_WHEN_VIEWED_BY_OTHERS:
			return groupSystem.findRawGroups(SystemViewpoint.getInstance(), user);
		case ONLY_WHEN_VIEWING_SELF:
			return Collections.emptySet();
		// no default, hides bugs
		}
		
		throw new RuntimeException("invalid inclusion " + block);
	}
}
