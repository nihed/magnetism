package com.dumbhippo.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.util.EJBUtil;

class LocalStorage implements Storage {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(LocalStorage.class);
	
	private Configuration config;
	private File saveDir;
	
	public LocalStorage(String repositoryName) {
		config = EJBUtil.defaultLookup(Configuration.class);
		
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		String saveUrl = filesUrl + File.separator + repositoryName;
		
		URI saveUri;
		try {
			saveUri = new URI(saveUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("save url busted", e);
		}
		saveDir = new File(saveUri);
	}
	
	/** 
	 * We split the guid into pieces to avoid too many files in 
	 * a single directory listing, so we don't need btrees or reiserfs 
	 * or whatever it is. Also, you could split the file storage among
	 * volumes. Premature optimization, woot!
	 * 
	 * The theory is that guids are in base-53, and we have a directory 
	 * for each of the first four chars in the guid, so that is 53^4 
	 * directories (around 8 million). So for example if there were 80 million
	 * files there would be on average 10 files per directory. We'll see I guess.
	 * Just don't run "ls -R" if we ever do have 80 million files ;-) 
	 * 
	 * @param guid
	 * @return a File object
	 */
	private File fileFromGuid(Guid guid) {
		String guidStr = guid.toString();
		StringBuilder sb = new StringBuilder();
		sb.append(guidStr.charAt(0));
		sb.append(File.separatorChar);
		sb.append(guidStr.charAt(1));
		sb.append(File.separatorChar);
		sb.append(guidStr.charAt(2));
		sb.append(File.separatorChar);
		sb.append(guidStr.charAt(3));
		sb.append(File.separatorChar);
		// then do a single 10 char filename
		sb.append(guidStr.substring(4,Guid.STRING_LENGTH));
		String relative = sb.toString();
		if (relative.length() != (Guid.STRING_LENGTH + 4))
			throw new RuntimeException("guid relative path should be guid length plus 4 separator chars");
		return new File(saveDir, relative);
	}
	
	public long store(Guid guid, String contentType, InputStream stream, long maxSize) throws StorageException {
		File f = fileFromGuid(guid);
		
		StorageLocks.getInstance().lock(guid);
		try {
			// The semantics of the various operations on File are 
			// VERY poorly-defined, in terms of whether they are atomic,
			// whether they overwrite existing stuff, etc. - i.e. it's not 
			// undocumented, it's documented as undefined. 
			// We're kind of hoping for UNIX semantics which would make this
			// fairly robust, but it shouldn't break too badly without them.
			
			if (f.exists())
				throw new StorageException("file already exists " + f);
			if (!f.getParentFile().mkdirs())
				throw new StorageException("unable to create directories " + f);
			
			File temp = new File(f.getPath() + ".tmp");
			
			OutputStream out;
			try {
				out = new FileOutputStream(temp);
			} catch (FileNotFoundException e) {
				throw new StorageException("unable to open file " + temp, e);
			}
			
			long storedBytes;
			
			try {
				storedBytes = StreamUtils.copy(stream, out, maxSize);
				if (storedBytes == maxSize) {
					int b = stream.read(); // check for EOF
					if (b >= 0)
						throw new TooBigException("This file is too big, max size " + maxSize);
				}
				out.flush();
				out.close();
			} catch (IOException e) {
				throw new StorageException("unable to copy from input stream to file " + temp, e);
			} finally {
				// be sure we close the output file in any case
				try {
					out.close();
				} catch (Exception e) {
				}
				out = null;
			}
			
			if (!temp.renameTo(f)) {
				if (!temp.delete()) {
					logger.error("Left stale file " + temp);
				}
				throw new StorageException("Failed to rename file " + temp + " to " + f);
			}
			
			logger.debug("Successfully stored guid " + guid + " in file " + f);
		
			return storedBytes;
		} finally {
			StorageLocks.getInstance().unlock(guid);
			try {
				stream.close();
			} catch (IOException e) {
				logger.warn("Failed to close input stream", e);
			}
		}
	}


	public void store(Guid guid, String contentType, byte[] content) throws StorageException {
		long stored = store(guid, contentType, new ByteArrayInputStream(content), content.length);
		if (stored != content.length)
			throw new RuntimeException("stored " + stored + " bytes but should have stored " + content.length);
	}
	
	public StoredData load(Guid guid) throws StorageException {
		File f = fileFromGuid(guid);
		
		StorageLocks.getInstance().lock(guid);
		try {
			// if the file doesn't exist, f.length just returns 0
			long fileSize = f.length();
			try {
				InputStream stream = new FileInputStream(f);
				logger.debug("Opened file " + f);
				return new StoredData(stream, fileSize);
			} catch (FileNotFoundException e) {
				throw new NotStoredException("Guid " + guid + " not saved in local storage", e);
			}
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}
	}

	public void delete(Guid guid) throws StorageException {
		File f = fileFromGuid(guid);
		
		StorageLocks.getInstance().lock(guid);
		try {
			if (!f.exists()) {
				logger.debug("Local storage had no file to delete " + f);
			} else {
				if (!f.delete()) {
					throw new StorageException("Failed to delete file " + f);
				}
				logger.debug("Deleted file " + f);
			}
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}		
	}
}
