package com.dumbhippo.storage;

import java.io.InputStream;

public class StoredData {
	private InputStream inputStream;
	private long sizeInBytes;
	private String mimeType;
	
	public StoredData(InputStream inputStream, long sizeInBytes) {
		this.inputStream = inputStream;
		this.sizeInBytes = sizeInBytes;
	}

	public InputStream getInputStream() {
		return inputStream;
	}
	
	public long getSizeInBytes() {
		return sizeInBytes;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	// the mime type is normally set outside the "storage" package, it's a 
	// bit of a hack to put it in this object really, to avoid having to 
	// pass both a StoredData and a SharedFile out of a file load method.
	// I guess it may be useful to return it from the storage eventually
	// if we want to avoid a db query to return world readable files.
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	@Override
	public String toString() {
		return "{StoredData size=" + getSizeInBytes() + " type=" + getMimeType() + "}";
	}
}
