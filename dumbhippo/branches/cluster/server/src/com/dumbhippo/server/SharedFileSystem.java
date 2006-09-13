package com.dumbhippo.server;

import java.io.InputStream;
import java.util.Collection;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.SharedFile;
import com.dumbhippo.persistence.StorageState;
import com.dumbhippo.persistence.User;
import com.dumbhippo.storage.StorageException;
import com.dumbhippo.storage.StoredData;

@Local
public interface SharedFileSystem {
	/**
	 * Page public (viewable by whole Internet) files created by a given user.
	 * 
	 * @param viewpoint
	 * @param creator
	 * @param pageable
	 */
	public void pagePublicFilesForCreator(Viewpoint viewpoint, User creator, Pageable<SharedFile> pageable);
	
	/**
	 * Page files for a given user that are not considered either public or private.
	 * 
	 * @param viewpoint
	 * @param creator
	 * @param pageable
	 */
	public void pageSharedFilesForCreator(UserViewpoint viewpoint, User creator, Pageable<SharedFile> pageable);
	
	/**
	 * Page files for a given user that are only visible to that user.
	 * 
	 * @param viewpoint
	 * @param pageable
	 */
	public void pagePrivateFiles(UserViewpoint viewpoint, Pageable<SharedFile> pageable);
	
	/** 
	 * Page files shared with a particular group that are readable by the Internet.
	 * 
	 * @param viewpoint
	 * @param group
	 * @param pageable
	 */
	public void pagePublicFilesForGroup(Viewpoint viewpoint, Group group, Pageable<SharedFile> pageable);

	/** 
	 * Page files shared with a particular group that are not readable by the Internet.
	 * Never returns anything for public groups.
	 * 
	 * @param viewpoint
	 * @param group
	 * @param pageable
	 */
	public void pageSharedFilesForGroup(UserViewpoint viewpoint, Group group, Pageable<SharedFile> pageable);

	/**
	 * Page all world-readable files globally. 
	 * 
	 * @param viewpoint
	 * @param pageable
	 */
	public void pagePublicFiles(Viewpoint viewpoint, Pageable<SharedFile> pageable);
	
	public String getQuotaRemainingString(UserViewpoint viewpoint);
	
	public User openUserDirectory(Viewpoint viewpoint, Guid userId) throws NotFoundException;
	
	public Collection<User> listUserDirectoriesWithPublicShares(Viewpoint viewpoint);
	
	/** 
	 * Creates a new SharedFile but does not yet save it in non-database storage.
	 * The SharedFile is in state StorageState.NOT_STORED and has the entire
	 * remaining quota as its size. You need to commit
	 * this transaction with it in that state, so it will count in quota checks.
	 * Only then (without a transaction open) can you call storeFileOutsideDatabase(). 
	 * 
	 * @param viewpoint
	 * @param name
	 * @param mimeType
	 * @param worldReadable
	 * @param groups
	 * @param users
	 * @return
	 * @throws HumanVisibleException
	 */
	public SharedFile createUnstoredFile(UserViewpoint viewpoint, 
			String name, String mimeType,
			boolean worldReadable, Collection<Group> groups,
			Collection<User> users) throws HumanVisibleException;
	
	/**
	 * Tries to store the given InputStream in a local or remote file storage
	 * facility, but does not do anything with the database (in fact this 
	 * should not be called with a transaction open). If this succeeds, then
	 * you should set the state of the SharedFile to STORED and commit that.
	 * The previous state of the SharedFile would normally be NOT_STORED 
	 * but could be DELETING or DELETED I suppose. The size of the file 
	 * should also be set to the returned stored size if this succeeds. 
	 */
	public long storeFileOutsideDatabase(SharedFile file, InputStream contents) 
		throws HumanVisibleException;	
	
	/**
	 * Sets the state on a file of the given guid, throwing NotFoundException if 
	 * the viewpoint can't see any file with this id.
	 * 
	 * @param fileGuid
	 * @param state
	 * @param sizeInBytes -1 to do nothing, anything else to set the file's size
	 */
	public void setFileState(Viewpoint viewpoint, Guid fileGuid, StorageState state, long sizeInBytes)
		throws NotFoundException, PermissionDeniedException;
	
	/**
	 * Tries to delete the SharedFile from local or remote file storage, but 
	 * does not do anything with the database. In fact, don't have a 
	 * transaction open when calling this. You should commit a change
	 * of the SharedFile state to DELETING prior to calling this, though.
	 * 
	 * If this fails, you should try to change the state back to STORED,
	 * otherwise we'll have to manually mop up DELETING files that aren't 
	 * truly deleted.
	 * 
	 * @param fileGuid id of a file in state DELETING
	 * @throws StorageException if we didn't really remove from backing store
	 */
	public void deleteFileOutsideDatabase(Guid fileGuid)
		throws StorageException;
	
	/** 
	 * Throw NotFoundException if file is nonexistent or 
	 * viewpoint can't see it. Otherwise return the file.
	 * @param viewpoint
	 * @param guid
	 * @param onlyIfInStoredState false to return the file even if state != STORED
	 * @throws NotFoundException
	 */
	public SharedFile lookupFile(Viewpoint viewpoint, Guid guid, boolean onlyIfInStoredState)
		throws NotFoundException;
	
	/** 
	 * Loads a file from storage, checking permissions for the viewpoint.
	 * Should be called from outside a transaction.
	 * 
	 * @param viewpoint viewpoint of the logged-in user or anonymous viewpoint
	 * @param guid
	 * @return
	 * @throws NotFoundException if file isn't stored or isn't visible
	 */
	public StoredData load(Viewpoint viewpoint, Guid guid)
		throws NotFoundException;
}
