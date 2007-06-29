package com.dumbhippo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** 
 * Not sure this is really different from the PipeInputStream/PipeOutputStream in the 
 * JDK, except I'm worried those two really use a pipe instead of just a memory 
 * queue, but don't know. This has the same issue that it can block if you read and write from the 
 * same thread.
 * 
 * @author Havoc Pennington
 */
public class Pipe {
	private ReadStream inputStream;
	private WriteStream outputStream; 
	private BlockingQueue<Chunk> queue;
	private long totalBytes;
	
	public Pipe() {
		queue = new LinkedBlockingQueue<Chunk>();
		inputStream = new ReadStream();
		outputStream = new WriteStream();
	}
	
	private class Chunk {
		byte[] data;
		
		// EOF marker
		Chunk() {
			data = null;
		}
		
		Chunk(byte b) {
			data = new byte[1];
			data[0] = b;
		}
		
		Chunk(byte[] bytes, int off, int len) {
			if (len == 0)
				return;
			data = new byte[len];
			System.arraycopy(bytes, off, data, 0, len);
		}
	}
	
	private class WriteStream extends OutputStream {

		static final private int MAX_CHUNK = 1024 * 16; 
		
		private boolean closed;
		
		@Override
		public synchronized void close() throws IOException {
			closed = true;
			try {
				queue.put(new Chunk()); // eof marker
			} catch (InterruptedException e) {
				throw new IOException("thread interrupted");
			}
			super.close();
		}
		
		@Override
		public synchronized void write(int b) throws IOException {
			if (closed)
				throw new IOException("stream is closed");
			try {
				queue.put(new Chunk((byte) b));
			} catch (InterruptedException e) {
				throw new IOException("thread interrupted");
			}
			totalBytes += 1;
		}
		
		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (closed)
				throw new IOException("stream is closed");
			while (len > 0) {
				int chunkSize;
				if (len > MAX_CHUNK)
					chunkSize = MAX_CHUNK;
				else
					chunkSize = len;
				
				try {
					queue.put(new Chunk(b, off, chunkSize));
				} catch (InterruptedException e) {
					throw new IOException("thread interrupted");
				}
				
				totalBytes += chunkSize;
				off += chunkSize;
				len -= chunkSize;
			}
		}
	}
	
	private class ReadStream extends InputStream {

		private Chunk current;
		private int currentPos;
		private boolean eof;
		
		private boolean getChunk() throws IOException {
			if (eof)
				return false;
			
			if (current != null && currentPos >= current.data.length) {
				current = null;
				currentPos = 0;
			}
			
			if (current == null) {
				try {
					current = queue.take();
				} catch (InterruptedException e) {
					throw new IOException("thread interrupted");
				}
			}
			
			assert current != null;

			if (current.data == null) {
				eof = true;
				current = null;
				return false;
			} else {
				return true;
			}
		}
		
		@Override
		public synchronized int read() throws IOException {
			if (!getChunk()) {
				return -1; // eof
			} 
			int b = current.data[currentPos];
			currentPos += 1;
			return b;
		}

		@Override
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			if (b == null)
				throw new NullPointerException("null byte array passed to read()");
			if (off < 0 || len < 0 || (off + len) > b.length)
				throw new IndexOutOfBoundsException("bad offset and length passed to read()");
			if (!getChunk()) {
				return -1; // eof
			}
			int avail = current.data.length - currentPos;
			assert avail >= 0;
			int toRead = Math.min(len, avail);
			GlobalSetup.getLogger(Pipe.class).debug("b.length = " + b.length + " off = " + off + " len = " + len +
					" current.data.length = " + current.data.length + 
					" currentPos = " + currentPos + 
					" avail = " + avail +
					" toRead = " + toRead);
			System.arraycopy(current.data, currentPos, b, off, toRead);
			currentPos += toRead;
			return toRead;
		}
	}
	
	public long getTotalBytes() {
		return totalBytes;
	}
	
	public OutputStream getWriteEnd() {
		return outputStream;
	}
	
	public InputStream getReadEnd() {
		return inputStream;
	}
}
