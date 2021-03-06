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

package org.sirix.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.brackit.xquery.atomic.QNm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sirix.Holder;
import org.sirix.TestHelper;
import org.sirix.api.Axis;
import org.sirix.api.NodeReadTrx;
import org.sirix.api.NodeWriteTrx;
import org.sirix.axis.DescendantAxis;
import org.sirix.exception.SirixException;
import org.sirix.index.path.summary.PathSummaryReader;
import org.sirix.node.Kind;
import org.sirix.utils.DocumentCreater;

/**
 * Test the {@link PathSummaryReader}.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 *         TODO: Provide a method for all assertions with parameters.
 */
public class PathSummaryTest {

	/** {@link Holder} reference. */
	private Holder holder;

	/** {@link IsummaryWriteTrx} implementation. */
	private NodeWriteTrx mWtx;

	@Before
	public void setUp() throws SirixException {
		TestHelper.deleteEverything();
		holder = Holder.generatePathSummarySession();
		mWtx = holder.getSession().beginNodeWriteTrx();
		DocumentCreater.create(mWtx);
	}

	@After
	public void tearDown() throws SirixException {
		holder.close();
		TestHelper.closeEverything();
	}

	/**
	 * Test insert on test document.
	 * 
	 * @throws SirixException
	 *           if Sirix fails
	 */
	@Test
	public void testInsert() throws SirixException {
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testInsertHelper(pathSummary);
		mWtx.commit();
		mWtx.close();
		pathSummary = holder.getSession().openPathSummary();
		testInsertHelper(pathSummary);
		pathSummary.close();
	}

	private void testInsertHelper(final PathSummaryReader pSummary)
			throws SirixException {
		final Axis axis = new DescendantAxis(pSummary);
		PathSummaryReader summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(1L, summary.getNodeKey());
		assertEquals(4L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "a"), axis.getTrx().getName());
		assertEquals(1, summary.getLevel());
		assertEquals(3, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(4L, summary.getNodeKey());
		assertEquals(6L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(3L, summary.getRightSiblingKey());
		assertEquals(new QNm("b"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(2, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(6L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(5L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "x"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(5L, summary.getNodeKey());
		assertEquals(6L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(3L, summary.getNodeKey());
		assertEquals(4L, summary.getLeftSiblingKey());
		assertEquals(2L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("i"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.NAMESPACE, summary.getPathKind());
		assertEquals(2L, summary.getNodeKey());
		assertEquals(3L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", ""), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary == null);
	}

	/**
	 * Test delete on test document.
	 * 
	 * @throws SirixException
	 *           if Sirix fails
	 */
	@Test
	public void testDelete() throws SirixException {
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testInsertHelper(pathSummary);
		mWtx.commit();
		mWtx.moveTo(9);
		mWtx.remove();
		pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testDeleteHelper(pathSummary);
		mWtx.commit();
		mWtx.close();
		pathSummary = holder.getSession().openPathSummary();
		testDeleteHelper(pathSummary);
		pathSummary.close();
	}

	private void testDeleteHelper(final PathSummaryReader pSummary)
			throws SirixException {
		final Axis axis = new DescendantAxis(pSummary);
		PathSummaryReader summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(1L, summary.getNodeKey());
		assertEquals(4L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "a"), axis.getTrx().getName());
		assertEquals(1, summary.getLevel());
		assertEquals(3, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(4L, summary.getNodeKey());
		assertEquals(5L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(3L, summary.getRightSiblingKey());
		assertEquals(new QNm("b"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(1, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(5L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(3L, summary.getNodeKey());
		assertEquals(4L, summary.getLeftSiblingKey());
		assertEquals(2L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("i"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.NAMESPACE, summary.getPathKind());
		assertEquals(2L, summary.getNodeKey());
		assertEquals(3L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", ""), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		summary = next(axis);
		assertTrue(summary == null);
	}

	/**
	 * Test setQNm on test document (does not find a corresponding path summary
	 * after rename).
	 * 
	 * @throws SirixException
	 *           if Sirix fails
	 */
	@Test
	public void testSetQNmFirst() throws SirixException {
		mWtx.moveTo(9);
		mWtx.setName(new QNm("foo"));
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testSetQNmFirstHelper(pathSummary);
		mWtx.commit();
		mWtx.close();
		pathSummary = holder.getSession().openPathSummary();
		testSetQNmFirstHelper(pathSummary);
		pathSummary.close();
	}

	private void testSetQNmFirstHelper(final PathSummaryReader pSummary)
			throws SirixException {
		final Axis axis = new DescendantAxis(pSummary);
		PathSummaryReader summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(1L, summary.getNodeKey());
		assertEquals(7L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "a"), axis.getTrx().getName());
		assertEquals(1, summary.getLevel());
		assertEquals(4, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(7L, summary.getNodeKey());
		assertEquals(9L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(4L, summary.getRightSiblingKey());
		assertEquals(new QNm("foo"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(2, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(9L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(8L, summary.getRightSiblingKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(8L, summary.getNodeKey());
		assertEquals(9L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", "x"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(4L, summary.getNodeKey());
		assertEquals(7L, summary.getLeftSiblingKey());
		assertEquals(3L, summary.getRightSiblingKey());
		assertEquals(5L, summary.getFirstChildKey());
		assertEquals(new QNm("b"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(1, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(5L, summary.getNodeKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(3L, summary.getNodeKey());
		assertEquals(4L, summary.getLeftSiblingKey());
		assertEquals(2L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("i"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.NAMESPACE, summary.getPathKind());
		assertEquals(2L, summary.getNodeKey());
		assertEquals(3L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", ""), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary == null);
	}

	/**
	 * Test setQNm on test document (finds a corresponding path summary after
	 * rename).
	 * 
	 * @throws SirixException
	 *           if Sirix fails
	 */
	@Test
	public void testSetQNmSecond() throws SirixException {
		mWtx.moveTo(9);
		mWtx.setName(new QNm("d"));
		mWtx.setName(new QNm("b"));
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testSetQNmSecondHelper(pathSummary);
		mWtx.commit();
		mWtx.close();
		pathSummary = holder.getSession().openPathSummary();
		testSetQNmSecondHelper(pathSummary);
		pathSummary.close();
	}

	private void testSetQNmSecondHelper(final PathSummaryReader pSummary)
			throws SirixException {
		final Axis axis = new DescendantAxis(pSummary);
		PathSummaryReader summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(1L, summary.getNodeKey());
		assertEquals(4L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "a"), axis.getTrx().getName());
		assertEquals(1, summary.getLevel());
		assertEquals(3, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(4L, summary.getNodeKey());
		assertEquals(10L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(3L, summary.getRightSiblingKey());
		assertEquals(new QNm("b"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(2, summary.getChildCount());
		assertEquals(2, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(10L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(5L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "x"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(5L, summary.getNodeKey());
		assertEquals(10L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(2, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(3L, summary.getNodeKey());
		assertEquals(4L, summary.getLeftSiblingKey());
		assertEquals(2L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("i"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.NAMESPACE, summary.getPathKind());
		assertEquals(2L, summary.getNodeKey());
		assertEquals(3L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", ""), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary == null);
	}

	/**
	 * Test setQNm on test document (finds no corresponding path summary after
	 * rename -- after references dropped to 0).
	 * 
	 * @throws SirixException
	 *           if Sirix fails
	 */
	@Test
	public void testSetQNmThird() throws SirixException {
		mWtx.moveTo(9);
		mWtx.setName(new QNm("d"));
		mWtx.moveTo(5);
		mWtx.setName(new QNm("t"));
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testSetQNmThirdHelper(pathSummary);
		mWtx.commit();
		mWtx.close();
		pathSummary = holder.getSession().openPathSummary();
		testSetQNmThirdHelper(pathSummary);
		pathSummary.close();
	}

	private void testSetQNmThirdHelper(final PathSummaryReader pSummary)
			throws SirixException {
		final Axis axis = new DescendantAxis(pSummary);
		PathSummaryReader summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(1L, summary.getNodeKey());
		assertEquals(7L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "a"), axis.getTrx().getName());
		assertEquals(1, summary.getLevel());
		assertEquals(4, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(7L, summary.getNodeKey());
		assertEquals(9L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(4L, summary.getRightSiblingKey());
		assertEquals(new QNm("d"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(2, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(9L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(8L, summary.getRightSiblingKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(8L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(9L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "", "x"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(4L, summary.getNodeKey());
		assertEquals(7L, summary.getLeftSiblingKey());
		assertEquals(3L, summary.getRightSiblingKey());
		assertEquals(5L, summary.getFirstChildKey());
		assertEquals(new QNm("t"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(1, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(5L, summary.getNodeKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(3L, summary.getNodeKey());
		assertEquals(4L, summary.getLeftSiblingKey());
		assertEquals(2L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("i"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.NAMESPACE, summary.getPathKind());
		assertEquals(2L, summary.getNodeKey());
		assertEquals(3L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", ""), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary == null);
	}

	/**
	 * Test setQNm on test document (finds no corresponding path summary after
	 * rename -- after references dropped to 0).
	 * 
	 * @throws SirixException
	 *           if Sirix fails
	 */
	@Test
	public void testSetQNmFourth() throws SirixException {
		mWtx.moveTo(1);
		mWtx.insertElementAsFirstChild(new QNm("b"));
		mWtx.moveTo(5);
		mWtx.setName(new QNm("d"));
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		testSetQNmFourthHelper(pathSummary);
		mWtx.commit();
		mWtx.close();
		pathSummary = holder.getSession().openPathSummary();
		testSetQNmFourthHelper(pathSummary);
		pathSummary.close();
	}

	private void testSetQNmFourthHelper(final PathSummaryReader pSummary)
			throws SirixException {
		final Axis axis = new DescendantAxis(pSummary);
		PathSummaryReader summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(1L, summary.getNodeKey());
		assertEquals(7L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("ns", "p", "a"), axis.getTrx().getName());
		assertEquals(1, summary.getLevel());
		assertEquals(4, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(7L, summary.getNodeKey());
		assertEquals(8L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(4L, summary.getRightSiblingKey());
		assertEquals(new QNm("d"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(1, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(8L, summary.getNodeKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(4L, summary.getNodeKey());
		assertEquals(7L, summary.getLeftSiblingKey());
		assertEquals(3L, summary.getRightSiblingKey());
		assertEquals(6L, summary.getFirstChildKey());
		assertEquals(new QNm("b"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(2, summary.getChildCount());
		assertEquals(2, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(6L, summary.getNodeKey());
		assertEquals(-1L, summary.getLeftSiblingKey());
		assertEquals(5L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "", "x"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ELEMENT, summary.getPathKind());
		assertEquals(5L, summary.getNodeKey());
		assertEquals(6L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("c"), axis.getTrx().getName());
		assertEquals(3, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.ATTRIBUTE, summary.getPathKind());
		assertEquals(3L, summary.getNodeKey());
		assertEquals(4L, summary.getLeftSiblingKey());
		assertEquals(2L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("i"), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary != null);
		assertEquals(Kind.NAMESPACE, summary.getPathKind());
		assertEquals(2L, summary.getNodeKey());
		assertEquals(3L, summary.getLeftSiblingKey());
		assertEquals(-1L, summary.getRightSiblingKey());
		assertEquals(-1L, summary.getFirstChildKey());
		assertEquals(new QNm("ns", "p", ""), axis.getTrx().getName());
		assertEquals(2, summary.getLevel());
		assertEquals(0, summary.getChildCount());
		assertEquals(1, summary.getReferences());
		summary = next(axis);
		assertTrue(summary == null);
	}

	@Test
	public void testFirstMoveToFirstChild() throws SirixException {
		mWtx.moveTo(5);
		mWtx.moveSubtreeToFirstChild(9);
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		mWtx.commit();
		mWtx.close();
		final NodeReadTrx rtx = holder.getSession().beginNodeReadTrx();
		rtx.close();
	}

	@Test
	public void testSecondMoveToFirstChild() throws SirixException {
		mWtx.moveTo(9);
		mWtx.insertElementAsFirstChild(new QNm("foo"));
		mWtx.insertElementAsFirstChild(new QNm("bar"));
		PathSummaryReader pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		mWtx.moveTo(5);
		mWtx.moveSubtreeToRightSibling(9);
		pathSummary = mWtx.getPathSummary();
		pathSummary.moveToDocumentRoot();
		mWtx.commit();
		mWtx.close();
		final NodeReadTrx rtx = holder.getSession().beginNodeReadTrx();
		rtx.close();
	}

	/**
	 * Get the next summary.
	 * 
	 * @param axis
	 *          the axis to use
	 * @return the next path summary
	 */
	private PathSummaryReader next(final Axis axis) {
		if (axis.hasNext()) {
			axis.next();
			return (PathSummaryReader) axis.getTrx();
		}
		return null;
	}
}
