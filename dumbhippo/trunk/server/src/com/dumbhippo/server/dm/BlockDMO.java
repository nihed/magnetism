package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMFeed;
import com.dumbhippo.dm.DMFeedItem;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMInit;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.MetaConstruct;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.persistence.BlockType.BlockVisibility;
import com.dumbhippo.server.ChatSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.EntitySourceBlockView;
import com.dumbhippo.server.blocks.MusicPersonBlockView;
import com.dumbhippo.server.blocks.TitleBlockView;
import com.dumbhippo.server.blocks.TitleDescriptionBlockView;
import com.dumbhippo.server.views.EntityView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@DMO(classId="http://mugshot.org/p/o/block", resourceBase="/o/block")
@DMFilter("viewer.canSeeBlock(this)")
public abstract class BlockDMO extends DMObject<BlockDMOKey> {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(BlockDMO.class);
	
	static final int USER_BLOCK_DATA_GROUP = 1;

	protected BlockView blockView;
	private UserBlockData userBlockData;

	@Inject
	Viewpoint viewpoint;
	
	@Inject
	DMSession session;
	
	@EJB
	Stacker stacker;
	
	@EJB
	ChatSystem chatSystem;

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
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getIcon() {
		return blockView.getIcon();
	}
	
	@DMProperty(defaultInclude=true)
	public long getTimestamp() {
		return blockView.getBlock().getTimestampAsLong();
	}
	
	@DMProperty(defaultInclude=true)
	public String getTitle() {
		if (blockView instanceof TitleBlockView)
			return ((TitleBlockView)blockView).getTitle();
		else
			return null;
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getTitleLink() {
		if (blockView instanceof TitleBlockView)
			return ((TitleBlockView)blockView).getLink();
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
	
	@DMProperty(defaultInclude=true)
	public String getChatId() {
		return blockView.getChatId();
	}
	
	@DMProperty(defaultInclude=true)
	public boolean isPublic() {
		return blockView.getBlockType().getBlockVisibility() == BlockVisibility.PUBLIC;
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public UserDMO getSourceUser() {
		if (blockView instanceof EntitySourceBlockView) {
			/* User sources might be returned even for EntitySourceBlockView's that aren't
			 * PersonSourceView; for example, PostBlockView, when posted by a user, instead
			 * of a feed. 
			 */
			EntityView entityView = ((EntitySourceBlockView)blockView).getEntitySource();
			if (entityView instanceof PersonView) {
				User user = ((PersonView)entityView).getUser();
				if (user != null)
					return session.findUnchecked(UserDMO.class, user.getGuid());
			}
		}
		
		return null;
	}

	
	@DMProperty(defaultInclude=true, defaultChildren="+", defaultMaxFetch=5)
	public DMFeed<ChatMessageDMO> getChatMessages() {
		return new MessagesFeed();
	}
	
	private class MessagesFeed implements DMFeed<ChatMessageDMO> {
		public Iterator<DMFeedItem<ChatMessageDMO>> iterator(int start, int max, long minTimestamp) {
			List<? extends ChatMessage> messages;
			
			if (blockView instanceof MusicPersonBlockView)
				messages = chatSystem.getTrackMessages(((MusicPersonBlockView)blockView).getTrack().getTrackHistory(), start, max, minTimestamp);
			else
				messages = chatSystem.getMessages(blockView.getBlock(), start, max, minTimestamp);
			
			List<DMFeedItem<ChatMessageDMO>> items = new ArrayList<DMFeedItem<ChatMessageDMO>>(); 
			for (ChatMessage message : messages) {
				ChatMessageDMO messageDMO = session.findUnchecked(ChatMessageDMO.class, new ChatMessageKey(message));
				items.add(new DMFeedItem<ChatMessageDMO>(messageDMO, message.getTimestampAsLong()));
			}
			
			return items.iterator();
		}
	}

	@DMProperty(defaultInclude=true)
	public int getChatMessageCount() {
		if (blockView instanceof MusicPersonBlockView)
			return chatSystem.getTrackMessageCount(((MusicPersonBlockView)blockView).getTrack().getTrackHistory());
		else
			return chatSystem.getMessageCount(blockView.getBlock());
	}
	
	//////////////////////////////////////////////////////////////////////
	
	@DMInit(group=USER_BLOCK_DATA_GROUP, initMain=false) 
	public void initUserBlockData() {
		if (viewpoint instanceof UserViewpoint) {
			try {
				userBlockData = stacker.lookupUserBlockData((UserViewpoint)viewpoint, getKey().getBlockId());
			} catch (NotFoundException e) {
			}
		}
	}
	
	@DMProperty(defaultInclude=true, group=USER_BLOCK_DATA_GROUP, cached=false)
	public long getClickedTimestamp() {
		if (userBlockData == null)
			return -1;
		else {
			long clickedTimestamp = userBlockData.getClickedTimestampAsLong();
			if (clickedTimestamp < 0) // Prettification, not to return -1000, which is what is stored in the DB
				return -1;
			else
				return clickedTimestamp;
		}
	}

	@DMProperty(defaultInclude=true, group=USER_BLOCK_DATA_GROUP, cached=false)
	public long getIgnoredTimestamp() {
		if (userBlockData == null)
			return -1;
		else {
			if (userBlockData.isIgnored())
				return userBlockData.getIgnoredTimestampAsLong();
			else
				return -1;
		}
	}
	
	// This isn't 101% right ... the stack reason should be the stack reason of
	// the block in the stack (and is thus stack dependent), not the stack reason 
	// of the viewer. But for right now, the data model stack is only being used
	// to view the user's own stack, for which the two things are the same.
	//
	// I'm not sure that the stack dependence of the stack reason is ever used,
	// so it may be possible to move the reason from UserBlockData to Block to use
	// the data model for the web, where we view blocks for which there is no
	// UserBlockData for the viewer.
	//
	@DMProperty(defaultInclude=true, group=USER_BLOCK_DATA_GROUP, cached=false)
	public String getStackReason() {
		if (userBlockData == null || 
			userBlockData.getStackReason() == StackReason.NEW_BLOCK)
			return null;
		else
			return userBlockData.getStackReason().name();
	}
	
	//////////////////////////////////////////////////////////////////////
	
	// These properties are used for the implementation of Viewpoint.canSeePrivateBlock(), 
	// Viewpoint.canSeeBlockDelegate()
	
	// This is also used to determine "isMine" by the client; since the owner is also
	// always returned by some other property, the expense of this in the protocol
	// over a boolean isn't big	
	@DMProperty(defaultInclude=true, defaultChildren="+") 
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
