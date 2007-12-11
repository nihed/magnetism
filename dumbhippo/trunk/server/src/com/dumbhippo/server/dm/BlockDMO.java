package com.dumbhippo.server.dm;

import javax.ejb.EJB;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.MetaConstruct;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.TitleBlockView;
import com.dumbhippo.server.blocks.TitleDescriptionBlockView;
import com.dumbhippo.server.views.SystemViewpoint;

@DMO(classId="http://mugshot.org/p/o/block", resourceBase="/o/block")
@DMFilter("viewer.canSeeBlock(this)")
public abstract class BlockDMO extends DMObject<BlockDMOKey> {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(BlockDMO.class);

	protected BlockView blockView;

	@Inject
	DMSession session;
	
	@EJB
	Stacker stacker;
	
	protected BlockDMO(BlockDMOKey key) {
		super(key);
	}
	
	public static Class<? extends BlockDMO> getDMOClass(BlockType blockType) {
		switch (blockType) {
		case FLICKR_PERSON:
		case YOUTUBE_PERSON:
		case MYSPACE_PERSON:
		case BLOG_ENTRY:
		case DELICIOUS_PUBLIC_BOOKMARK:
		case TWITTER_PERSON:
		case DIGG_DUGG_ENTRY:
		case REDDIT_ACTIVITY_ENTRY:
		case GOOGLE_READER_SHARED_ITEM:
			return BlockDMO.class;

		case POST:
			return PostBlockDMO.class;
			
		case PICASA_PERSON:
			return PicasaPersonBlockDMO.class;
			
		default:
		case GROUP_MEMBER: 
		case GROUP_CHAT:
		case MUSIC_PERSON:
		case FACEBOOK_EVENT:
		case FLICKR_PHOTOSET:
		case MUSIC_CHAT:
		case GROUP_REVISION:		
		case NETFLIX_MOVIE:
		case ACCOUNT_QUESTION:
		case AMAZON_REVIEW:
		case AMAZON_WISH_LIST_ITEM:
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE:
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF:
		case OBSOLETE_BLOG_PERSON:
		case FACEBOOK_PERSON:
			return null;
		}
	}
	
	@MetaConstruct
	public static Class<? extends BlockDMO> getDMOClass(BlockDMOKey key) {
		return getDMOClass(key.getType());
	}

	@Override
	protected void init() throws NotFoundException {
		BlockView view = stacker.loadBlock(SystemViewpoint.getInstance(), stacker.lookupBlock(getKey().getBlockId()));
		
		// This check is important because it prevents someone from bypassing visibility rules
		// by loading a block as the wrong type of DMO. this.getClass() is the wrapper class
		// created by the DataModel engine, so the superclass is the real DMO class.
		
		Class<? extends BlockDMO> dmoClass = getDMOClass(view.getBlockType());
		if (!dmoClass.equals(this.getClass().getSuperclass()))
			throw new NotFoundException("Mismatch between resource type " + this.getClass().getSuperclass() + " and block type " + dmoClass);
		
		blockView = view;
	}
	
	@DMProperty(defaultInclude=true)
	public String getTitle() {
		if (blockView instanceof TitleBlockView)
			return ((TitleBlockView)blockView).getTitle();
		else
			return null;
	}
	
	@DMProperty(defaultInclude=true)
	public String getDescription() {
		if (blockView instanceof TitleDescriptionBlockView)
			return ((TitleDescriptionBlockView)blockView).getDescription();
		else
			return null;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	// These properties are here for the implementation of Viewpoint.canSeePrivateBlock(), 
	// Viewpoint.canSeeBlockDelegate()
	
	@DMProperty 
	public UserDMO getOwner() {
		switch (blockView.getBlockType().getBlockOwnership()) {
		case DIRECT_DATA1:
		case INDIRECT_DATA1:
			Guid data1 = blockView.getBlock().getData1AsGuid();
			if (data1 != null)
				return session.findUnchecked(UserDMO.class, data1);
			else
				return null;
		case DIRECT_DATA2:
		case INDIRECT_DATA2:
			Guid data2 = blockView.getBlock().getData2AsGuid();
			if (data2 != null)
				return session.findUnchecked(UserDMO.class, data2);
			else
				return null;
		case NONE:
			return null;
		}
		
		return null;
	}
	
	@DMProperty 
	public StoreKey<?,?> getVisibilityDelegate() {
		return null;
	}
}
