/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package net.jumperz.ext.org.jrobin.core;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashSet;

/**
 * JRobin backend which is used to store RRD data to ordinary files on the disk. This was the
 * default factory before 1.4.0 version<p>
 *
 * This backend is based on the RandomAccessFile class (java.io.* package).
 */
public class RrdFileBackend extends RrdBackend {
	static final long LOCK_DELAY = 100; // 0.1sec

	private static HashSet openFiles = new HashSet();

	protected RandomAccessFile file;
	protected FileChannel channel;
	protected FileLock fileLock;

	protected RrdFileBackend(String path, boolean readOnly, int lockMode) throws IOException {
		super(path);
		file = new RandomAccessFile(path, readOnly? "r": "rw");
		channel = file.getChannel();
		if(!readOnly) {
			lockFile(lockMode);
			// We'll try to lock the file only in "rw" mode
			registerWriter(path);
		}
	}

	private static synchronized void registerWriter(String path) throws IOException {
		String canonicalPath = getCanonicalPath(path);
		if(openFiles.contains(canonicalPath)) {
			throw new IOException("File \"" + path + "\" already open for R/W access. " +
					"You cannot open the same file for R/W access twice");
		}
		else {
			openFiles.add(canonicalPath);
		}
	}

	private void lockFile(int lockMode) throws IOException {
		if(lockMode == RrdDb.WAIT_IF_LOCKED || lockMode == RrdDb.EXCEPTION_IF_LOCKED) {
			do {
				fileLock = channel.tryLock();
				if(fileLock == null) {
					// could not obtain lock
					if(lockMode == RrdDb.WAIT_IF_LOCKED) {
						// wait a little, than try again
						try {
							Thread.sleep(LOCK_DELAY);
						} catch (InterruptedException e) {
							// NOP
						}
					}
					else {
						throw new IOException("Access denied. " +
							"File [" + getPath() + "] already locked");
					}
				}
			} while(fileLock == null);
		}
	}

	/**
	 * Closes the underlying RRD file.
	 * @throws IOException Thrown in case of I/O error
	 */
	public void close() throws IOException {
		super.close(); // calls sync()
		unregisterWriter(getPath());
		unlockFile();
		channel.close();
		file.close();
	}

	private static synchronized void unregisterWriter(String path) throws IOException {
		String canonicalPath = getCanonicalPath(path);
		openFiles.remove(canonicalPath);
	}

	private void unlockFile() throws IOException {
		if(fileLock != null) {
			fileLock.release();
			fileLock = null;
		}
	}

	/**
	 * Closes the underlying RRD file if not already closed
	 * @throws IOException Thrown in case of I/O error
	 */
	protected void finalize() throws IOException {
		close();
	}

	/**
	 * Returns canonical path to the file on the disk.
	 * @param path File path
	 * @return Canonical file path
	 * @throws IOException Thrown in case of I/O error
	 */
	public static String getCanonicalPath(String path) throws IOException {
		return new File(path).getCanonicalPath();
	}

	/**
	 * Returns canonical path to the file on the disk.
	 * @return Canonical file path
	 * @throws IOException Thrown in case of I/O error
	 */
	public String getCanonicalPath() throws IOException {
		return RrdFileBackend.getCanonicalPath(getPath());
	}

	/**
	 * Writes bytes to the underlying RRD file on the disk
	 * @param offset Starting file offset
	 * @param b Bytes to be written.
	 * @throws IOException Thrown in case of I/O error
	 */
	protected void write(long offset, byte[] b) throws IOException {
		file.seek(offset);
		file.write(b);
	}

	/**
	 * Reads a number of bytes from the RRD file on the disk
	 * @param offset Starting file offset
	 * @param b Buffer which receives bytes read from the file.
	 * @throws IOException Thrown in case of I/O error.
	 */
	protected void read(long offset, byte[] b) throws IOException {
		file.seek(offset);
		if(file.read(b) != b.length) {
			throw new IOException("Not enough bytes available in file " + getPath());
		}
	}

	/**
	 * Returns RRD file length.
	 * @return File length.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public long getLength() throws IOException {
		return file.length();
	}

	/**
	 * Sets length of the underlying RRD file. This method is called only once, immediately
	 * after a new RRD file gets created.
	 * @param length Length of the RRD file
	 * @throws IOException Thrown in case of I/O error.
	 */
	protected void setLength(long length) throws IOException {
		file.setLength(length);
	}
}
