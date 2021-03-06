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

package org.sirix.cache;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import org.sirix.page.PagePersistenter;
import org.sirix.page.UnorderedKeyValuePage;
import org.sirix.page.interfaces.KeyValuePage;
import org.sirix.utils.LogWrapper;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * <h1>PageContainer</h1>
 * 
 * <p>
 * This class acts as a container for revisioned {@link KeyValuePage}s. Each
 * {@link KeyValuePage} is stored in a versioned manner. If modifications occur,
 * the versioned {@link KeyValuePage}s are dereferenced and reconstructed.
 * Afterwards, this container is used to store a complete {@link KeyValuePage}
 * as well as one for upcoming modifications.
 * </p>
 * 
 * <p>
 * Both {@link KeyValuePage}s can differ since the complete one is mainly used
 * for read access and the modifying one for write access (and therefore mostly
 * lazy dereferenced).
 * </p>
 * 
 * @author Sebastian Graf, University of Konstanz
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public final class RecordPageContainer<T extends KeyValuePage<?, ?>> {

	/** Logger. */
	private static final LogWrapper LOGGER = new LogWrapper(
			LoggerFactory.getLogger(RecordPageContainer.class));

	/**
	 * {@link UnorderedKeyValuePage} reference, which references the complete
	 * key/value page.
	 */
	private final T mComplete;

	/**
	 * {@link UnorderedKeyValuePage} reference, which references the modified
	 * key/value page.
	 */
	private final T mModified;

	/** Empty instance. */
	public static final RecordPageContainer<? extends KeyValuePage<?, ?>> EMPTY_INSTANCE = new RecordPageContainer<>();

	/** Private constructor for empty instance. */
	private RecordPageContainer() {
		mComplete = null;
		mModified = null;
	}

	/**
	 * Get the empty instance (parameterized).
	 * 
	 * @return the empty instance
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends KeyValuePage<?, ?>> RecordPageContainer<T> emptyInstance() {
		return (RecordPageContainer<T>) EMPTY_INSTANCE;
	}

	/**
	 * Constructor with complete page and lazy instantiated modifying page.
	 * 
	 * @param complete
	 *          page to clone
	 * @param revision
	 *          the new revision
	 */
	@SuppressWarnings("unchecked")
	public RecordPageContainer(final T complete) {
		this(complete, (T) complete.newInstance(complete.getPageKey(),
				complete.getPageKind(), complete.getPreviousReference(),
				complete.getPageReadTrx()));
	}

	/**
	 * Constructor with both, complete and modifying page.
	 * 
	 * @param complete
	 *          to be used as a base for this container
	 * @param modifying
	 *          to be used as a base for this container
	 */
	public RecordPageContainer(final T complete, final T modifying) {
		// Assertions as it's not part of the public API.
		assert complete != null;
		assert modifying != null;
		mComplete = complete;
		mModified = modifying;
	}

	/**
	 * Getting the complete page.
	 * 
	 * @return the complete page
	 */
	public T getComplete() {
		return mComplete;
	}

	/**
	 * Getting the modified page.
	 * 
	 * @return the modified page
	 */
	public T getModified() {
		return mModified;
	}

	/**
	 * Serializing the container to the cache.
	 * 
	 * @param out
	 *          for serialization
	 */
	public void serialize(final TupleOutput out) {
		final ByteArrayOutputStream sink = new ByteArrayOutputStream();
		final DataOutputStream dataOut = new DataOutputStream(sink);
		try {
			PagePersistenter.serializePage(dataOut, mComplete);
			PagePersistenter.serializePage(dataOut, mModified);
		} catch (final IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		out.write(sink.toByteArray());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(mComplete, mModified);
	}

	@Override
	public boolean equals(final @Nullable Object obj) {
		if (obj instanceof RecordPageContainer) {
			final RecordPageContainer<?> other = (RecordPageContainer<?>) obj;
			return Objects.equal(mComplete, other.mComplete)
					&& Objects.equal(mModified, other.mModified);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("complete page", mComplete)
				.add("modified page", mModified).toString();
	}
}
