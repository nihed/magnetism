package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.SharedFile;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.WebEJBUtil;

public class PersonFilesPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PersonFilesPage.class);

	private static final int INITIAL_PER_PAGE = 10;
	
	private SharedFileSystem sharedFileSystem;
	
	private Pageable<SharedFile> publicFiles;
	private Pageable<SharedFile> sharedFiles;
	private Pageable<SharedFile> privateFiles;
	private String quotaRemaining;
	
	public PersonFilesPage() {
		sharedFileSystem = WebEJBUtil.defaultLookup(SharedFileSystem.class);
	}
	
	public Pageable<SharedFile> getPublicFiles() {
		if (publicFiles == null) {
			publicFiles = pagePositions.createPageable("publicFiles", INITIAL_PER_PAGE);
			sharedFileSystem.pagePublicFilesForCreator(getSignin().getViewpoint(),
					getViewedUser(), publicFiles);
		}
		return publicFiles;
	}
	
	public Pageable<SharedFile> getSharedFiles() {
		if (sharedFiles == null) {
			sharedFiles = pagePositions.createPageable("sharedFiles", INITIAL_PER_PAGE);
			Viewpoint v = getSignin().getViewpoint();
			if (v instanceof UserViewpoint) {
				sharedFileSystem.pageSharedFilesForCreator((UserViewpoint) v,
						getViewedUser(), sharedFiles);
			}
		}
		return sharedFiles;
	}
	
	public Pageable<SharedFile> getPrivateFiles() {
		if (privateFiles == null) {
			privateFiles = pagePositions.createPageable("privateFiles", INITIAL_PER_PAGE);
			Viewpoint v = getSignin().getViewpoint();
			if (v instanceof UserViewpoint) {
				UserViewpoint uv = (UserViewpoint) v;
				if (uv.getViewer().equals(getViewedUser()))
					sharedFileSystem.pagePrivateFiles(uv, privateFiles);
			}
		}
		return privateFiles;
	}
	
	// only callable if logged in
	public String getQuotaRemaining() {
		if (quotaRemaining == null)
			quotaRemaining = sharedFileSystem.getQuotaRemainingString(getUserSignin().getViewpoint());
		return quotaRemaining;
	}
}
