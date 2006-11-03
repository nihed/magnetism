package com.dumbhippo.server.blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.ejb.EJB;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

public abstract class AbstractBlockHandlerBean<BlockViewSubType extends BlockView> implements BlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(AbstractBlockHandlerBean.class);
	
	@EJB
	protected IdentitySpider identitySpider;
	
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
}
