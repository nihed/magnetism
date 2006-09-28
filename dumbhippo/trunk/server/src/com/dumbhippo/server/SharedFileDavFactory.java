package com.dumbhippo.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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
		private Viewpoint viewpoint;
		private SharedFileSystem fileSystem;
		private Map<String,DavNode> cachedUserNodes;
		
		RootNode(Viewpoint viewpoint, SharedFileSystem fileSystem) {
			super(null, null);
			this.viewpoint = viewpoint; 
			this.fileSystem = fileSystem;
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
			throw new RuntimeException("listing on root node not good");
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
					cachedUserNodes.put(u.getId(), new UserNode(this, u));
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
			DavNode node = new UserNode(this, user);
			if (cachedUserNodes == null)
				cachedUserNodes = new HashMap<String,DavNode>();
			cachedUserNodes.put(name, node);
			return node;
		}
		
		@Override
		public String getDisplayName() {
			return "All Mugshot Users";
		}
	}
	
	static private class UserNode extends AbstractCollectionNode {

		private User viewedUser;

		protected UserNode(AbstractNode parent, User viewedUser) {
			super(parent, viewedUser.getId());
			this.viewedUser = viewedUser;
		}
		
		@Override
		protected User getViewedUser() {
			return viewedUser;
		}
		
		@Override
		protected Map<String, DavNode> newChildrenMap() {
			Map<String, DavNode> children = new HashMap<String, DavNode>();
			DavNode node = new PublicNode(this);
			children.put(node.getName(), node);
			
			Viewpoint viewpoint = getViewpoint();
			if (viewpoint.isOfUser(getViewedUser())) {
				node = new PrivateNode(this);
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

		protected AbstractQueryNode(AbstractNode parent, String name) {
			super(parent, name);
		}

		protected abstract void pageCollectionContents(Pageable<SharedFile> pageable);
		
		@Override
		protected Map<String, DavNode> newChildrenMap() {
			Map<String, DavNode> children = new HashMap<String, DavNode>();
			Pageable<SharedFile> pageable = new Pageable<SharedFile>(getName());
			pageable.setInitialPerPage(Integer.MAX_VALUE);
			pageable.setSubsequentPerPage(0);
			pageCollectionContents(pageable);
			for (SharedFile sf : pageable.getResults()) {
				DavNode node = new SharedFileNode(this, sf);
				children.put(node.getName(), node);
			}
			
			return children;
		}
	}
	
	static private class PublicNode extends AbstractQueryNode {

		protected PublicNode(AbstractNode parent) {
			super(parent, "Public");
		}

		@Override
		protected void pageCollectionContents(Pageable<SharedFile> pageable) {
			getFileSystem().pagePublicFilesForCreator(getViewpoint(), getViewedUser(), pageable);
		}
	}

	static private class PrivateNode extends AbstractQueryNode {

		protected PrivateNode(AbstractNode parent) {
			super(parent, "Private");
		}

		@Override
		protected void pageCollectionContents(Pageable<SharedFile> pageable) {
			Viewpoint v = getViewpoint();
			if ((v instanceof UserViewpoint))
				getFileSystem().pagePrivateFiles((UserViewpoint) v, pageable);
			else
				pageable.setResults(TypeUtils.emptyList(SharedFile.class));
		}
	}
	
	static private class SharedFileNode extends AbstractNode {
		private SharedFile file;
		private Map<DavProperty,Object> properties;
		private StoredData loaded;
		
		SharedFileNode(AbstractNode parent, SharedFile file) {
			super(parent);
			this.file = file;
		}
		
		public String getName() {
			return file.getId();
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
					logger.error("Failed to load an already-created SharedFileNode");
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
	}
}
