package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pipe;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.SharedFile;
import com.dumbhippo.persistence.StorageState;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PermissionDeniedException;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.storage.StoredData;
import com.dumbhippo.web.WebEJBUtil;

public class FileStorageServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(FileStorageServlet.class);

	private SharedFileSystem sharedFileSystem;
	
	/** 
	 * This doesn't have as much point as one might hope; the theoretical idea is 
	 * to stick to streams and avoid slurping a whole huge file into memory, but 
	 * in practice commons-fileupload just dumps the entire incoming http stream
	 * into the output stream as soon as a file item is created. Bad commons-fileupload!
	 * In fact I think there's no way to avoid this with the FileItem API since 
	 * FileItem returns an OutputStream to write to from http, instead of taking an InputStream 
	 * to read from.
	 * 
	 * @author Havoc Pennington
	 *
	 */
	private static class PassthroughFileItemFactory implements FileItemFactory {

		private static class PassthroughFileItem implements FileItem {
			private static final long serialVersionUID = 1L;
			
			private String fieldName;
			private String contentType;
			private boolean formField;
			private String fileName;
			private transient Pipe pipe;
			private transient byte[] content;
			private transient String stringContentDefaultEncoding;

			PassthroughFileItem(String fieldName, String contentType, boolean isFormField, String fileName) {
				this.fieldName = fieldName;
				this.contentType = contentType;
				this.formField = isFormField;
				this.fileName = fileName;
				this.pipe = new Pipe();
			}

			public InputStream getInputStream() throws IOException {
				return pipe.getReadEnd();
			}

			public String getContentType() {
				return contentType;
			}

			public String getName() {
				return fileName;
			}

			public boolean isInMemory() {
				return true;
			}

			public long getSize() {
				logger.warn("Forcing input stream read to get size of FileItem");
				byte[] bytes = get();
				if (bytes != null)
					return bytes.length;
				else
					return 0;
			}

			public byte[] get() {
				if (!formField)
					logger.warn("get() called on FileItem that isn't a form field, not cheap, and "
							+ "probably breaks things since input stream will be already used");
				
				if (content == null) {
					try {
						InputStream in = getInputStream();
						content = StreamUtils.readStreamBytes(in, Long.MAX_VALUE);
						in.close();
					} catch (IOException e) {
						logger.warn("Failed to read FileItem", e);
						return null;
					}
				}
				return content;
			}

			public String getString(String encoding) throws UnsupportedEncodingException {
				byte[] bytes = get();
				if (bytes != null)
					return new String(bytes, encoding);
				else
					return null;
			}

			public String getString() {
				if (stringContentDefaultEncoding == null) {
					byte[] bytes = get();
					if (bytes != null)
						stringContentDefaultEncoding = new String(bytes);
				}
				return stringContentDefaultEncoding;
			}

			public void write(File file) throws Exception {
				throw new UnsupportedOperationException("not implemented");
			}

			public void delete() {
				throw new UnsupportedOperationException("not implemented");
			}

			public String getFieldName() {
				return fieldName;
			}

			public void setFieldName(String name) {
				this.fieldName = name;
			}

			public boolean isFormField() {
				return formField;
			}

			public void setFormField(boolean formField) {
				this.formField = formField;
			}

			// this gets the stream that the fileupload library should 
			// push the file data into
			public OutputStream getOutputStream() throws IOException {
				return pipe.getWriteEnd();
			}
		}
		
		public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
			return new PassthroughFileItem(fieldName, contentType, isFormField, fileName);
		}		
	}
	
	@Override
	public void init() {
		sharedFileSystem = WebEJBUtil.defaultLookup(SharedFileSystem.class);
	}
	
	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}

	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException, HumanVisibleException {
		
		if (!request.getRequestURI().equals("/files/user"))
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Not a valid POST uri");
		
		ServletFileUpload upload = new ServletFileUpload(new PassthroughFileItemFactory());
		
		List<FileItem> items;
		try {
			items = TypeUtils.castList(FileItem.class, upload.parseRequest(request));
		} catch (FileUploadException e) {
			logger.debug("Bad file upload request: {}", e.getMessage());
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Could not understand file upload request");
		}
		FileItem theFile = null;
		String reloadTo = null;
		for (FileItem item : items) {
			if (item.isFormField()) {
				if (item.getFieldName().equals("reloadTo"))
					reloadTo = item.getString();
			} else {
				if (theFile == null)
					theFile = item;
				else
					throw new HttpException(HttpResponseCode.BAD_REQUEST, "can't upload multiple files at once");
			}
		}
		
		if (theFile == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "no file uploaded in request");
		String mimeType = theFile.getContentType();
		if (mimeType == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "no mime type given for file");

		// We break on backslash if both slash and backslash are present, 
		// the effect here is that on unix if you have backslash in 
		// a filename we'll chop at it. But that seems a lot less likely
		// than having a slash in a filename on Windows.
		String rawName = theFile.getName();
		int lastSlash = rawName.lastIndexOf('/');
		int lastBackslash = rawName.lastIndexOf('\\');
		String relativeName;
		if (lastSlash <= 0 && lastBackslash <= 0)
			relativeName = rawName;
		else if (lastBackslash > 0)
			relativeName = rawName.substring(lastBackslash+1);
		else
			relativeName = rawName.substring(lastSlash+1);
		
		if (relativeName.length() == 0)
			relativeName = rawName;
		
		Viewpoint viewpoint = getViewpoint(request);
		if (!(viewpoint instanceof UserViewpoint))
			throw new HttpException(HttpResponseCode.FORBIDDEN, "you must be logged in to upload files");		
		
		UserViewpoint userViewpoint = (UserViewpoint) viewpoint;
		
		// Transaction 1 - store a SharedFile in state NOT_STORED with size equal to entire quota
		// FIXME right now it's always public
		SharedFile sf = sharedFileSystem.createUnstoredFile(userViewpoint, relativeName,
				mimeType, true, null, null);
		
		// outside transaction - stuff the file contents somewhere. Max size is the 
		// remaining quota set on the SharedFile.
		long storedSize = -1;
		try {
			storedSize = sharedFileSystem.storeFileOutsideDatabase(sf, theFile.getInputStream());
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
		assert storedSize >= 0;
		
		// Transaction 2 - set the correct state and size on the SharedFile if we didn't
		// throw an exception storing it
		try {
			sharedFileSystem.setFileState(userViewpoint, sf.getGuid(), StorageState.STORED, storedSize);
		} catch (NotFoundException e) {
			logger.error("Should never happen, file we just created not found", e);
		} catch (PermissionDeniedException e) {
			logger.error("Should never happen, permission denied to change file we created", e);
		}
	
		if (reloadTo != null)
			response.sendRedirect(reloadTo);
		
		return null;
	}

	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException {
		
		/* The web request is for something like 
		 *  /files/user/guid, getPathInfo() returns just the 
		 *  guid part (the part that isn't our servlet root).
		 *  
		 *  We also allow /files/user/ANYTHING&id=GUID, the idea of this
		 *  is that ANYTHING is our filename and the browser will have the
		 *  right default download link.
		 */
		String guidStr = request.getParameter("id");
		if (guidStr == null)
			guidStr = request.getPathInfo().substring(1);
		
		Guid guid;
		try {
			guid = new Guid(guidStr);
		} catch (ParseException e) {
			logger.debug("Invalid guid string '{}' in file request", guidStr);
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Invalid file id");
		}

		StoredData stored;
		try {
			stored = sharedFileSystem.load(getViewpoint(request), guid);
		} catch (NotFoundException e) {
			logger.debug("File guid {} not found: {}", guid, e.getMessage());
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No such file");
		}
		
		long size = stored.getSizeInBytes();
		if (size > Integer.MAX_VALUE) {
			logger.warn("Have a too-large file {} guid {}", stored, guid);
			throw new HttpException(HttpResponseCode.SERVICE_UNAVAILABLE, "File is too big to serve");
		}
		response.setContentLength((int) size);
		response.setContentType(stored.getMimeType());

		InputStream in = stored.getInputStream();
		try {
			OutputStream out = response.getOutputStream();
			try {
				StreamUtils.copy(in, out);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
		
		logger.debug("Served file {} {}", guid, stored);
		
		return null;
	}
}
