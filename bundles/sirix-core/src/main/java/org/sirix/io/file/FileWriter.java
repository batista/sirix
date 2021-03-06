/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sirix.io.file;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.sirix.exception.SirixIOException;
import org.sirix.io.AbstractForwardingReader;
import org.sirix.io.Reader;
import org.sirix.io.Writer;
import org.sirix.io.bytepipe.ByteHandler;
import org.sirix.page.PagePersistenter;
import org.sirix.page.PageReference;
import org.sirix.page.interfaces.Page;

/**
 * File Writer for providing read/write access for file as a Sirix backend.
 * 
 * @author Marc Kramis, Seabix
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public final class FileWriter extends AbstractForwardingReader implements
		Writer {

	/** Random access to work on. */
	private final RandomAccessFile mFile;

	/** {@link FileReader} reference for this writer. */
	private final FileReader mReader;

	/**
	 * Constructor.
	 * 
	 * @param storage
	 *          the concrete storage
	 * @param handler
	 *          the byte handler
	 * @throws SirixIOException
	 *           if an I/O error occurs
	 */
	public FileWriter(final File storage, final ByteHandler handler)
			throws SirixIOException {
		try {
			mFile = new RandomAccessFile(storage, "rw");
		} catch (final FileNotFoundException e) {
			throw new SirixIOException(e);
		}
		mReader = new FileReader(storage, handler);
	}

	/**
	 * Write page contained in page reference to storage.
	 * 
	 * @param pageReference
	 *          page reference to write
	 * @throws SirixIOException
	 *           if errors during writing occur
	 */
	@Override
	public void write(final PageReference pageReference) throws SirixIOException {
		// Perform byte operations.
		try {
			// Serialize page.
			final Page page = pageReference.getPage();
			assert page != null;
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			final DataOutputStream dataOutput = new DataOutputStream(
					mReader.mByteHandler.serialize(output));
			PagePersistenter.serializePage(dataOutput, page);
			output.close();
			dataOutput.close();

			final byte[] serializedPage = output.toByteArray();

			final byte[] writtenPage = new byte[serializedPage.length
					+ FileReader.OTHER_BEACON];
			final ByteBuffer buffer = ByteBuffer.allocate(writtenPage.length);
			buffer.putInt(serializedPage.length);
			buffer.put(serializedPage);
			buffer.position(0);
			buffer.get(writtenPage, 0, writtenPage.length);

			// Getting actual offset and appending to the end of the current
			// file.
			final long fileSize = mFile.length();
			final long offset = fileSize == 0 ? FileReader.FIRST_BEACON : fileSize;
			mFile.seek(offset);
			mFile.write(writtenPage);

			// Remember page coordinates.
			pageReference.setKey(offset);
		} catch (final IOException e) {
			throw new SirixIOException(e);
		}
	}

	@Override
	public void close() throws SirixIOException {
		try {
			if (mFile != null) {
				mFile.close();
			}
			if (mReader != null) {
				mReader.close();
			}
		} catch (final IOException e) {
			throw new SirixIOException(e);
		}
	}

	@Override
	public void writeUberPageReference(final PageReference pageReference)
			throws SirixIOException {
		try {
			write(pageReference);
			mFile.seek(0);
			mFile.writeLong(pageReference.getKey());
		} catch (final IOException e) {
			throw new SirixIOException(e);
		}
	}

	@Override
	protected Reader delegate() {
		return mReader;
	}
}
