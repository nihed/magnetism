package com.dumbhippo.server.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.SharedFile;
import com.dumbhippo.persistence.SharedFileGroup;
import com.dumbhippo.persistence.SharedFileUser;
import com.dumbhippo.persistence.StorageState;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PermissionDeniedException;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.storage.NotStoredException;
import com.dumbhippo.storage.Storage;
import com.dumbhippo.storage.StorageException;
import com.dumbhippo.storage.StorageFactory;
import com.dumbhippo.storage.StoredData;
import com.dumbhippo.storage.TooBigException;

@Stateless
public class SharedFileSystemBean implements SharedFileSystem {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(SharedFileSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private GroupSystem groupSystem;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	private Storage storage;
	
	@PostConstruct
	public void init() {
		storage = StorageFactory.newStorage();
	}
	
	public void pagePublicFilesForCreator(Viewpoint viewpoint, User creator, Pageable<SharedFile> pageable) {
		Query q = em.createQuery("SELECT sf FROM SharedFile sf WHERE sf.creator = :creator AND " +
				" sf.state = " + StorageState.STORED.ordinal() + " AND sf.worldReadable = TRUE " +
				" ORDER BY sf.creationDate DESC");
		q.setParameter("creator", creator);
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<SharedFile> list = TypeUtils.castList(SharedFile.class, q.getResultList());
		pageable.setResults(list);
	}

	public void pageSharedFilesForCreator(UserViewpoint viewpoint, User creator, Pageable<SharedFile> pageable) {
		
		if (!viewpoint.isOfUser(creator)) {
			// FIXME - need to put "viewer is in a receiving group or is a receiving user"
			// into the query below
			pageable.setResults(TypeUtils.castList(SharedFile.class, Collections.emptyList()));
			logger.warn("Not implemented - query files by creator with viewer other than creator");
			return;
		}
		
		Query q = em.createQuery("SELECT sf FROM SharedFile sf WHERE sf.creator = :creator AND " +
		" sf.state = " + StorageState.STORED.ordinal() + " AND sf.worldReadable = FALSE AND (sf.groups IS NOT EMPTY OR EXISTS " + 
		" (SELECT notCreator FROM SharedFileUser notCreator WHERE notCreator.file = sf AND notCreator.user != :creator)) " +   
		" ORDER BY sf.creationDate DESC");
		q.setParameter("creator", creator);
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<SharedFile> list = TypeUtils.castList(SharedFile.class, q.getResultList());
		pageable.setResults(list);
	}

	public void pagePrivateFiles(UserViewpoint viewpoint, Pageable<SharedFile> pageable) {
		Query q = em.createQuery("SELECT sf FROM SharedFile sf WHERE sf.creator = :creator AND " +
				" sf.state = " + StorageState.STORED.ordinal() + " AND sf.worldReadable = FALSE AND sf.groups IS EMPTY AND NOT EXISTS " + 
				" (SELECT notCreator FROM SharedFileUser notCreator WHERE notCreator.file = sf AND notCreator.user != :creator) " +   
				" ORDER BY sf.creationDate DESC");
		q.setParameter("creator", viewpoint.getViewer());
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<SharedFile> list = TypeUtils.castList(SharedFile.class, q.getResultList());
		pageable.setResults(list);
	}

	public void pagePublicFilesForGroup(Viewpoint viewpoint, Group group, Pageable<SharedFile> pageable) {
		// TODO Auto-generated method stub
		
	}

	public void pageSharedFilesForGroup(UserViewpoint viewpoint, Group group, Pageable<SharedFile> pageable) {
		// TODO Auto-generated method stub
		
	}

	public void pagePublicFiles(Viewpoint viewpoint, Pageable<SharedFile> pageable) {
		// TODO Auto-generated method stub
		
	}
	
	/** 
	 * This is different from IdentitySpider.lookupUser because we might eventually 
	 * have a separate setting to enable/disable browsing of files. 
	 * Also, SharedFileDavFactory has a ref to SharedFileSystem but not to 
	 * IdentitySpider.
	 */
	public User openUserDirectory(Viewpoint viewpoint, Guid userId) throws NotFoundException {
		// note that this throws NotFoundException while lookupUser does not.
		return identitySpider.lookupGuid(User.class, userId);
	}
	
	public Collection<User> listUserDirectoriesWithPublicShares(Viewpoint viewpoint) {
		Query q = em.createQuery("SELECT user FROM User user WHERE EXISTS " +
				"(SELECT sf FROM SharedFile sf WHERE sf.creator = user AND " +
				" sf.state = " + StorageState.STORED.ordinal() + " AND sf.worldReadable = TRUE)");
		List<User> list = TypeUtils.castList(User.class, q.getResultList());
		return list;
	}
	
	private static String friendlySize(double bytes) {
		if (bytes < 1024 * 100) {
			return ((int) Math.round(bytes / 1024)) + "K";
		} else if (bytes < (1024 * 1024 * 100)) {
			return String.format("%.1fM", (bytes / (1024 * 1024)));
		} else {
			return String.format("%.1fG", (bytes / (1024 * 1024 * 1024)));
		}
	}
	
	private static final long QUOTA = 1024 * 1024 * 10; 
	
	private long getQuotaRemaining(UserViewpoint viewpoint) {
		User user = viewpoint.getViewer();
		
		// Note that we count all of NOT_STORED, STORED, DELETING which is to avoid races.
		// Stuff is in the quota if it _might_ be in storage.
		
		Query q = em.createQuery("SELECT SUM(sf.sizeInBytes) FROM SharedFile sf WHERE sf.creator = :creator AND " +
				" sf.state != " + StorageState.DELETED.ordinal());
		q.setParameter("creator", user);
		long used;
		try {
			used = ((Number) q.getSingleResult()).longValue();
		} catch (NoResultException e) {
			used = 0;
		}
		return QUOTA - used;
	}
	
	public String getQuotaRemainingString(UserViewpoint viewpoint) {
		return friendlySize(getQuotaRemaining(viewpoint));
	}
		
	public SharedFile createUnstoredFile(UserViewpoint viewpoint, 
			String name, String mimeType, 
			boolean worldReadable, Collection<Group> groups,
			Collection<User> users) throws HumanVisibleException {
		
		// originally this was supposed to print a nice friendly "over quota" 
		// message at the start of file upload, but it turns out we don't know
		// the file size at the start of the upload, only as we read it...
		// so the quota error happens later when we try storing the file
		long availableSpace = getQuotaRemaining(viewpoint);
		
		SharedFile file = new SharedFile(viewpoint.getViewer(), name, mimeType,
				availableSpace,
				worldReadable, groups, users);
		em.persist(file);
		return file;
	}
	
	/* This should be called sans transaction since it's going to do unknown IO 
	 * and web services goo possibly uploading a large file that takes a long time.
	 */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public long storeFileOutsideDatabase(SharedFile file, InputStream contents) 
		throws HumanVisibleException {
		
		// First we save in storage; then we persist to the database only if that succeeds.
		long stored;
		try {
			stored = storage.store(file.getGuid(), contents, file.getSizeInBytes());
		} catch (TooBigException e) {
			throw new HumanVisibleException("This file is too big - " +
					" you only have " + friendlySize(file.getSizeInBytes()) + " remaining");			
		} catch (StorageException e) {
			logger.warn("Failed to store file {}", file);
			logger.warn("exception was", e);
			throw new HumanVisibleException("Could not store your file right now; it might help to wait a little while and try again");
		}

		return stored;
	}

	public void setFileState(Viewpoint viewpoint, Guid fileGuid, StorageState state, long sizeInBytes)
		throws NotFoundException, PermissionDeniedException {
		
		// note that we throw NotFound if you can't see the file, and PermissionDenied
		// only if you can't modify its state
		
		SharedFile file = lookupFile(viewpoint, fileGuid, false);
		
		if (!(viewpoint instanceof UserViewpoint))
			throw new PermissionDeniedException("must be logged in to change file state");
		
		UserViewpoint userViewpoint = (UserViewpoint) viewpoint;
		
		if (!userViewpoint.isOfUser(file.getCreator())) {
			throw new PermissionDeniedException("for now files can only be modified by their creator");
		}
		
		file.setState(state);
		if (sizeInBytes >= 0)
			file.setSizeInBytes(sizeInBytes);
	}
	
	/* This should be called sans transaction since it's going to do unknown IO 
	 * and web services goo.
	 */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void deleteFileOutsideDatabase(Guid fileGuid) throws StorageException {
		storage.delete(fileGuid);
	}
	
	public SharedFile lookupFile(Viewpoint viewpoint, Guid guid, boolean onlyIfInStoredState)
		throws NotFoundException {
		SharedFile file = em.find(SharedFile.class, guid.toString());
		if (file == null)
			throw new NotFoundException("No such file guid " + guid);
		
		if (onlyIfInStoredState && file.getState() != StorageState.STORED)
			throw new NotFoundException("File was deleted or never successfully saved");
		
		if (file.isWorldReadable())
			return file;
		
		if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint) viewpoint).getViewer();
			if (file.getCreator().equals(viewer))
				return file;
			
			// right now users/groups are lazy-loaded so this does 
			// a couple queries...
			Set<SharedFileUser> users = file.getUsers();
			for (SharedFileUser u : users) {
				if (u.getUser().equals(viewer))
					return file;
			}
			
			Set<SharedFileGroup> groups = file.getGroups();
			for (SharedFileGroup g : groups) {
				GroupMember member = null;
				try {
					member = groupSystem.getGroupMember(viewpoint, g.getGroup(), viewer);
				} catch (NotFoundException e) {
				}
				if (member != null && member.isParticipant())
					return file;
			}
		}
		
		throw new NotFoundException("Not allowed to view this file");
	}
	
	// right now this has to create an internal transaction to check permissions, but 
	// eventually we might keep a "world readable" flag in the backend storage, 
	// which would allow us to avoid querying the db for an Internet-visible file
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public StoredData load(final Viewpoint viewpoint, final Guid guid)
		throws NotFoundException {
		// look ourselves up to get a proxy that will create a transaction
		SharedFileSystem sfs = EJBUtil.defaultLookup(SharedFileSystem.class);
		SharedFile file = sfs.lookupFile(viewpoint, guid, true);
		
		// then this stuff is not in a transaction
		StoredData data;
		try {
			data = storage.load(file.getGuid());
		} catch (NotStoredException e) {
			throw new NotFoundException("File is not present in storage");
		} catch (StorageException e) {
			throw new RuntimeException(e);
		}
		if (data.getSizeInBytes() != file.getSizeInBytes()) {
			logger.error("Stored data size does not match file size {} {}", data, file);
			throw new NotFoundException("stored file has the wrong size");
		}
		data.setMimeType(file.getMimeType());
		return data;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public SharedFile storeFile(UserViewpoint userViewpoint, String relativeName, String mimeType, InputStream inputStream,
			boolean worldReadable, Collection<Group> groups,
			Collection<User> users) throws HumanVisibleException {
		EJBUtil.assertNoTransaction();
		
		// get a proxy
		SharedFileSystem sharedFileSystem = EJBUtil.defaultLookup(SharedFileSystem.class);
		
		// Transaction 1 - store a SharedFile in state NOT_STORED with size equal to entire quota
		SharedFile sf = sharedFileSystem.createUnstoredFile(userViewpoint, relativeName,
				mimeType, worldReadable, groups, users);
		
		// outside transaction - stuff the file contents somewhere. Max size is the 
		// remaining quota set on the SharedFile.
		long storedSize = -1;
		try {
			storedSize = sharedFileSystem.storeFileOutsideDatabase(sf, inputStream);
		} finally {
			// If something goes wrong, try to remove the reserved quota so the 
			// user isn't doomed
			if (storedSize < 0) {
				try {
					sharedFileSystem.setFileState(userViewpoint, sf.getGuid(), StorageState.NOT_STORED, 0);
				} catch (Exception e) {
					logger.warn("Exception removing reserved quota after failed file storage", e);
				}
			}
		}
		
		if (storedSize < 0)
			throw new RuntimeException("storedSize < 0 should not happen");
		
		// Transaction 2 - set the correct state and size on the SharedFile if we didn't
		// throw an exception storing it
		try {
			sharedFileSystem.setFileState(userViewpoint, sf.getGuid(), StorageState.STORED, storedSize);
		} catch (NotFoundException e) {
			logger.error("Should never happen, file we just created not found", e);
		} catch (PermissionDeniedException e) {
			logger.error("Should never happen, permission denied to change file we created", e);
		}
		
		return sf;
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
 	public void deleteFile(UserViewpoint viewpoint, Guid fileId) throws HumanVisibleException {
		EJBUtil.assertNoTransaction();

		// get a proxy
		SharedFileSystem sharedFileSystem = EJBUtil.defaultLookup(SharedFileSystem.class);

		// Transaction 1 - set state to DELETING
 		try {
			sharedFileSystem.setFileState(viewpoint, fileId, StorageState.DELETING, -1);
		} catch (NotFoundException e) {
			throw new HumanVisibleException("No such file");
		} catch (PermissionDeniedException e) {
			throw new HumanVisibleException("You aren't allowed to delete this file");
		}
		try {
			// Outside transaction - remove from local or remote file storage
			sharedFileSystem.deleteFileOutsideDatabase(fileId);
		} catch (Exception e) {
			logger.error("Failed to delete file", e);
			try {
				// Transaction 2 - emergency revert of DELETING state
				sharedFileSystem.setFileState(viewpoint, fileId, StorageState.STORED, -1);
			} catch (Exception e2) {
				logger.error("Failed in last-ditch effort to fix state of file {}, must be set back to STORED manually",
						fileId);
				logger.error("Exception was", e2);
			}
			throw new HumanVisibleException("Something went wrong while deleting this file");
		}
		// Transaction 3 - set the state to DELETED to indicate success
		try {
			sharedFileSystem.setFileState(viewpoint, fileId, StorageState.DELETED, -1);
		} catch (Exception e) {
			logger.error("Successfully deleted file {} but failed to set its state to DELETED", fileId);
			logger.error("Exception was", e);
			// don't return an error from the method, this will appear to have worked from user 
			// standpoint
		}
 	}
}
