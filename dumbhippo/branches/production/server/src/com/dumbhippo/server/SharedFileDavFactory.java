package com.dumbhippo.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.dav.DavNode;
import com.dumbhippo.dav.DavProperty;
import com.dumbhippo.dav.DavResourceType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.SharedFile;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.storage.StoredData;

/**
 * Generates DAV nodes based on shared files.
 * 
 * @author Havoc Pennington
 *
 */
public class SharedFileDavFactory {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(SharedFileDavFactory.class);	
	
	static public DavNode newRoot(Viewpoint viewpoint, SharedFileSystem sharedFileSystem) {
		return new RootNode(viewpoint, sharedFileSystem);
	}
	
	static private abstract class AbstractNode implements DavNode {
		private AbstractNode parent;
		
		protected AbstractNode(AbstractNode parent) {
			this.parent = parent;
		}
		
		protected Map<DavProperty,Object> newProperties() {
			Map<DavProperty,Object> properties = new EnumMap<DavProperty,Object>(DavProperty.class);
			if (getResourceType() != DavResourceType.COLLECTION) {
				properties.put(DavProperty.CONTENT_LENGTH, getContentLength());
				properties.put(DavProperty.CONTENT_TYPE, getContentType());
			}
			properties.put(DavProperty.DISPLAY_NAME, getDisplayName());
			properties.put(DavProperty.RESOURCE_TYPE, getResourceType());
			
			if (getLastModified() != 0)
				properties.put(DavProperty.LAST_MODIFIED, getLastModified());
			
			return properties;
		}
		
		protected SharedFileSystem getFileSystem() {
			return parent.getFileSystem();
		}
		
		protected Viewpoint getViewpoint() {
			return parent.getViewpoint();
		}
		
		protected User getViewedUser() {
			return parent.getViewedUser();
		}
		
		public AbstractNode getParent() {
			return parent;
		}
		
		public void replaceContent(String mimeType, InputStream contents) throws IOException {
			throw new IOException("Not a writable file");
		}
		
		public void delete() throws IOException {
			throw new IOException("Not a writable folder");
		}
		
		public void createChild(String name, String mimeType, InputStream contents) throws IOException {
			throw new IOException("Not a writable folder");
		}
		
		protected void checkIsOurOwnFile() throws IOException {
			if (!getViewpoint().isOfUser(getViewedUser()))
				throw new IOException("Can't write to other people's files, or not logged in");
		}
	}
	
	static private abstract class AbstractCollectionNode extends AbstractNode {

		private String name;
		private Map<String,DavNode> children;
		private Map<DavProperty,Object> properties;
		
		protected AbstractCollectionNode(AbstractNode parent, String name) {
			super(parent);
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Map<DavProperty, Object> getProperties() {
			if (properties == null) {
				properties = newProperties();
			}
			return properties;
		}

		protected abstract Map<String,DavNode> newChildrenMap();
		
		protected void invalidateChildren() {
			children = null;
		}
		
		private Map<String,DavNode> getChildrenMap() {
			if (children == null) {
				children = newChildrenMap();
			}
			return children;
		}
		
		public Collection<DavNode> getChildren() {
			return getChildrenMap().values();
		}

		public DavNode getChild(String name) throws NotFoundException {
			DavNode child = getChildrenMap().get(name);
			if (child == null)
				throw new NotFoundException("no file '" + name + "' under this directory");
			else
				return child;
		}

		public void write(OutputStream out) throws IOException {
			throw new IOException("is a collection, can't write its contents");
		}

		public int getContentLength() {
			throw new RuntimeException("no content length, is a collection"); 
		}

		public DavResourceType getResourceType() {
			return DavResourceType.COLLECTION;
		}

		public String getContentType() {
			throw new RuntimeException("no content type, is a collection");
		}

		public String getDisplayName() {
			if (name == null)
				throw new RuntimeException(this.getClass().getName() + " needs to override getDisplayName");
			return name;
		}

		public long getLastModified() {
			// FIXME, not sure what to put here.
			return 0;
		}
	}
	
	static private class RootNode extends AbstractCollectionNode {
		final private Viewpoint viewpoint;
		final private SharedFileSystem fileSystem;
		final private AllUsersNode byIdNode;
		final private AllUsersNode byNameNode;
		
		RootNode(Viewpoint viewpoint, SharedFileSystem fileSystem) {
			super(null, null);
			this.viewpoint = viewpoint; 
			this.fileSystem = fileSystem;
			this.byIdNode = new AllUsersNode(this, true);
			this.byNameNode = new AllUsersNode(this, false);
		}
		
		@Override
		protected SharedFileSystem getFileSystem() {
			return fileSystem;
		}
		
		@Override
		protected Viewpoint getViewpoint() {
			return viewpoint;
		}

		@Override
		protected Map<String, DavNode> newChildrenMap() {
			Map<String,DavNode> children = new HashMap<String,DavNode>();
			children.put(byIdNode.getName(), byIdNode);
			children.put(byNameNode.getName(), byNameNode);
			return children;
		}
		
		/** 
		 * Maybe we should be sending back a "Forbidden" error instead of 
		 * an empty directory ? For testing convenience, we also 
		 * include the logged-in user if any.
		 */
		@Override
		public Collection<DavNode> getChildren() {
			List<DavNode> list = new ArrayList<DavNode>();
			list.add(byIdNode);
			list.add(byNameNode);
			return list;
		}
		
		@Override
		public String getDisplayName() {
			return "Mugshot";
		}
	}
	
	static private class AllUsersNode extends AbstractCollectionNode {
		static final private String USER_FILES_BY_ID_FOLDER = "users";    // by-id browsing assumes working display names, so this can be machine-readable
		static final private String USER_FILES_BY_NAME_FOLDER = "People"; // using a nice display name as actual name
		
		final private boolean byId;
		private Map<String,DavNode> cachedUserNodes;
		
		AllUsersNode(AbstractNode parent, boolean byId) {
			super(parent, byId ? USER_FILES_BY_ID_FOLDER : USER_FILES_BY_NAME_FOLDER);
			this.byId = byId;
		}		
		
		@Override
		protected Map<String, DavNode> newChildrenMap() {
			throw new RuntimeException("listing on all-users node not good");
		}
		
		/** 
		 * Maybe we should be sending back a "Forbidden" error instead of 
		 * an empty directory ? For testing convenience, we also 
		 * include the logged-in user if any.
		 */
		@Override
		public Collection<DavNode> getChildren() {
			Viewpoint viewpoint = getViewpoint();
			if (viewpoint instanceof UserViewpoint) {
				UserViewpoint uv = (UserViewpoint) viewpoint;
				try {
					return Collections.singleton(getChild(uv.getViewer().getId()));
				} catch (NotFoundException e) {
					logger.error("Failed to get logged-in user for root node listing", e);
				}
			}
			
			// FIXME In real life we need to return empty list here
			//return TypeUtils.emptyList(DavNode.class);
			
			// For debugging, return full list (expensive!)
			Collection<User> allUsers = getFileSystem().listUserDirectoriesWithPublicShares(getViewpoint());
			if (cachedUserNodes == null)
				cachedUserNodes = new HashMap<String,DavNode>();
			for (User u : allUsers) {
				if (cachedUserNodes.get(u.getId()) == null) {
					cachedUserNodes.put(u.getId(), new UserNode(this, u, byId));
				}
			}
			return cachedUserNodes.values();
		}
		
		@Override
		public DavNode getChild(String name) throws NotFoundException {
			if (cachedUserNodes != null) {
				DavNode node = cachedUserNodes.get(name);
				if (node != null)
					return node;
			}
			User user;
			try {
				user = getFileSystem().openUserDirectory(getViewpoint(),
						new Guid(name));
			} catch (ParseException e) {
				throw new NotFoundException("'" + name + "' is not a user id");
			}
			DavNode node = new UserNode(this, user, byId);
			if (cachedUserNodes == null)
				cachedUserNodes = new HashMap<String,DavNode>();
			cachedUserNodes.put(name, node);
			return node;
		}
		
		@Override
		public String getDisplayName() {
			// We're expecting the user's own folder to be 
			// their dav root really, so this should not be visible most 
			// of the time. Having the name the same for both the byId and !byId
			// versions is confusing, but if the names were different, what would they 
			// be... possibly we should have multiple dav roots and not allow browsing
			// at this level, which would let us leave this node anonymous
			return "People";
		}
	}
	
	static private class UserNode extends AbstractCollectionNode {

		final private boolean byId;
		private User viewedUser;

		protected UserNode(AbstractNode parent, User viewedUser, boolean byId) {
			super(parent, viewedUser.getId());
			this.viewedUser = viewedUser;
			this.byId = byId;
		}
		
		@Override
		protected User getViewedUser() {
			return viewedUser;
		}
		
		@Override
		protected Map<String, DavNode> newChildrenMap() {
			Map<String, DavNode> children = new HashMap<String, DavNode>();
			DavNode node = new PublicNode(this, byId);
			children.put(node.getName(), node);
			
			Viewpoint viewpoint = getViewpoint();
			if (viewpoint.isOfUser(getViewedUser())) {
				node = new PrivateNode(this, byId);
				children.put(node.getName(), node);
			}
			
			return children;
		}
		
		@Override
		public String getDisplayName() {
			return getViewedUser().getNickname();
		}
	}
	
	static abstract private class AbstractQueryNode extends AbstractCollectionNode {

		final private boolean byId;
		
		protected AbstractQueryNode(AbstractNode parent, String name, boolean byId) {
			super(parent, name);
			this.byId = byId;
		}

		protected abstract void pageCollectionContents(Pageable<SharedFile> pageable);
		
		protected DavNode createNode(SharedFile sf) {
			DavNode node;
			if (byId)
				node = new SharedFileByIdNode(this, sf);
			else
				node = new SharedFileByNameNode(this, sf);
			return node;
		}
		
		@Override
		protected Map<String, DavNode> newChildrenMap() {
			Map<String, DavNode> children = new HashMap<String, DavNode>();
			Pageable<SharedFile> pageable = new Pageable<SharedFile>(getName());
			pageable.setInitialPerPage(Integer.MAX_VALUE);
			pageable.setSubsequentPerPage(0);
			pageCollectionContents(pageable);
			for (SharedFile sf : pageable.getResults()) {
				DavNode node = createNode(sf);
				children.put(node.getName(), node);
			}
			
			return children;
		}
		
		protected void createChild(String name, String mimeType, InputStream contents, boolean worldReadable) throws IOException {
			checkIsOurOwnFile();
			
			try {
				getFileSystem().storeFile((UserViewpoint)getViewpoint(), name, mimeType, contents, worldReadable, null, null);
				invalidateChildren();
			} catch (HumanVisibleException e) {
				throw new IOException(e.getMessage());
			}			
		}
	}
	
	static private class PublicNode extends AbstractQueryNode {

		protected PublicNode(AbstractNode parent, boolean byId) {
			super(parent, "Public", byId);
		}

		@Override
		protected void pageCollectionContents(Pageable<SharedFile> pageable) {
			getFileSystem().pagePublicFilesForCreator(getViewpoint(), getViewedUser(), pageable);
		}
		
		@Override
		public void createChild(String name, String mimeType, InputStream contents) throws IOException {
			createChild(name, mimeType, contents, true);
		}
	}

	static private class PrivateNode extends AbstractQueryNode {

		protected PrivateNode(AbstractNode parent, boolean byId) {
			super(parent, "Private", byId);
		}

		@Override
		protected void pageCollectionContents(Pageable<SharedFile> pageable) {
			Viewpoint v = getViewpoint();
			if ((v instanceof UserViewpoint))
				getFileSystem().pagePrivateFiles((UserViewpoint) v, pageable);
			else
				pageable.setResults(TypeUtils.emptyList(SharedFile.class));
		}
		
		@Override
		public void createChild(String name, String mimeType, InputStream contents) throws IOException {
			createChild(name, mimeType, contents, false);
		}
	}
	
	static abstract private class AbstractSharedFileNode extends AbstractNode {
		protected SharedFile file; // assume this is detached
		private Map<DavProperty,Object> properties;
		private StoredData loaded;
		
		AbstractSharedFileNode(AbstractQueryNode parent, SharedFile file) {
			super(parent);
			this.file = file;
		}
		
		abstract public String getName();

		@Override
		public AbstractQueryNode getParent() {
			return (AbstractQueryNode) super.getParent();
		}
		
		public Map<DavProperty, Object> getProperties() {
			if (properties == null) {
				properties = newProperties();
			}
			return properties;
		}

		public Collection<DavNode> getChildren() {
			return Collections.emptyList();
		}

		public DavNode getChild(String name) throws NotFoundException {
			throw new NotFoundException("No children, this is a file");
		}

		private StoredData load() {
			if (loaded == null) {
				try {
					loaded = getFileSystem().load(getViewpoint(), file.getGuid());
				} catch (NotFoundException e) {
					logger.error("Failed to load an already-created SharedFileByIdNode");
					throw new RuntimeException(e);
				}
			}
			return loaded;
		}
		
		public void write(OutputStream out) throws IOException {
			StoredData loaded = load();
			InputStream in = loaded.getInputStream();
			StreamUtils.copy(in, out);
			out.flush();
		}

		public int getContentLength() {
			StoredData loaded = load();
			long sizeBytes = loaded.getSizeInBytes();
			if (sizeBytes > Integer.MAX_VALUE)
				throw new RuntimeException("too large file " + sizeBytes);
			return (int) sizeBytes;
		}

		public DavResourceType getResourceType() {
			return DavResourceType.FILE;
		}

		public String getContentType() {
			StoredData loaded = load();
			return loaded.getMimeType();
		}

		public String getDisplayName() {
			return file.getName();
		}

		public long getLastModified() {
			return file.getCreationDate().getTime();
		}
		
		@Override
		public void replaceContent(String mimeType, InputStream contents) throws IOException {
			throw new IOException("Replacing files isn't supported right now - try deleting it then moving the new file over");
		}
		
		@Override
		public void delete() throws IOException {
			checkIsOurOwnFile();
			try {
				getFileSystem().deleteFile((UserViewpoint) getViewpoint(), file.getGuid());
			} catch (HumanVisibleException e) {
				throw new IOException(e.getMessage());
			}
			getParent().invalidateChildren();
		}
	}
	
	static private class SharedFileByIdNode extends AbstractSharedFileNode {
		
		SharedFileByIdNode(AbstractQueryNode parent, SharedFile file) {
			super(parent, file);
		}

		@Override
		public String getName() {
			return file.getId();
		}
	}
	
	static private class SharedFileByNameNode extends AbstractSharedFileNode {
		
		SharedFileByNameNode(AbstractQueryNode parent, SharedFile file) {
			super(parent, file);
		}

		@Override
		public String getName() {
			return file.getName();
		}
	}
}
