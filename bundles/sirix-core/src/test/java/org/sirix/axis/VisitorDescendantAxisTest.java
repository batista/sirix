package org.sirix.axis;

import java.util.Collections;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sirix.Holder;
import org.sirix.TestHelper;
import org.sirix.api.NodeReadTrx;
import org.sirix.axis.visitor.VisitorDescendantAxis;
import org.sirix.exception.SirixException;
import org.sirix.settings.Fixed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;

public class VisitorDescendantAxisTest {

	private static final int ITERATIONS = 5;

	private Holder holder;

	@Before
	public void setUp() throws SirixException {
		TestHelper.deleteEverything();
		TestHelper.createTestDocument();
		holder = Holder.generateRtx();
	}

	@After
	public void tearDown() throws SirixException {
		holder.close();
		TestHelper.closeEverything();
	}

	@Test
	public void testIterateVisitor() throws SirixException {
		final NodeReadTrx rtx = holder.getRtx();

		rtx.moveToDocumentRoot();
		AbsAxisTest.testIAxisConventions(VisitorDescendantAxis.newBuilder(rtx)
				.build(), new long[] { 1L, 4L, 5L, 6L, 7L, 8L, 9L, 11L, 12L, 13L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(1L, 4L, 5L, 6L, 7L, 8L, 9L, 11L, 12L, 13L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveToDocumentRoot();
				return VisitorDescendantAxis.newBuilder(rtx).build();
			}
		}.test();

		rtx.moveTo(1L);
		AbsAxisTest.testIAxisConventions(VisitorDescendantAxis.newBuilder(rtx)
				.build(), new long[] { 4L, 5L, 6L, 7L, 8L, 9L, 11L, 12L, 13L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(4L, 5L, 6L, 7L, 8L, 9L, 11L, 12L, 13L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveTo(1L);
				return VisitorDescendantAxis.newBuilder(rtx).build();
			}
		}.test();

		rtx.moveTo(9L);
		AbsAxisTest.testIAxisConventions(VisitorDescendantAxis.newBuilder(rtx)
				.build(), new long[] { 11L, 12L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(11L, 12L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveTo(9L);
				return VisitorDescendantAxis.newBuilder(rtx).build();
			}
		}.test();

		rtx.moveTo(13L);
		AbsAxisTest.testIAxisConventions(VisitorDescendantAxis.newBuilder(rtx)
				.build(), new long[] {});
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				Collections.<Long> emptyList(), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveTo(13L);
				return VisitorDescendantAxis.newBuilder(rtx).build();
			}
		}.test();
	}

	@Test
	public void testIterateIncludingSelfVisitor() throws SirixException {
		final NodeReadTrx rtx = holder.getRtx();
		rtx.moveToDocumentRoot();
		AbsAxisTest.testIAxisConventions(VisitorDescendantAxis.newBuilder(rtx)
				.includeSelf().build(),
				new long[] { Fixed.DOCUMENT_NODE_KEY.getStandardProperty(), 1L, 4L, 5L,
						6L, 7L, 8L, 9L, 11L, 12L, 13L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(Fixed.DOCUMENT_NODE_KEY.getStandardProperty(), 1L, 4L,
						5L, 6L, 7L, 8L, 9L, 11L, 12L, 13L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveToDocumentRoot();
				return VisitorDescendantAxis.newBuilder(rtx).includeSelf().build();
			}
		}.test();

		rtx.moveTo(1L);
		AbsAxisTest.testIAxisConventions(new VisitorDescendantAxis.Builder(rtx)
				.includeSelf().build(), new long[] { 1L, 4L, 5L, 6L, 7L, 8L, 9L, 11L,
				12L, 13L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(1L, 4L, 5L, 6L, 7L, 8L, 9L, 11L, 12L, 13L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveTo(1L);
				return VisitorDescendantAxis.newBuilder(rtx).includeSelf().build();
			}
		}.test();

		rtx.moveTo(9L);
		AbsAxisTest.testIAxisConventions(new VisitorDescendantAxis.Builder(rtx)
				.includeSelf().build(), new long[] { 9L, 11L, 12L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(9L, 11L, 12L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveTo(9L);
				return VisitorDescendantAxis.newBuilder(rtx).includeSelf().build();
			}
		}.test();

		rtx.moveTo(13L);
		AbsAxisTest.testIAxisConventions(new VisitorDescendantAxis.Builder(rtx)
				.includeSelf().build(), new long[] { 13L });
		new IteratorTester<Long>(ITERATIONS, IteratorFeature.UNMODIFIABLE,
				ImmutableList.of(13L), null) {
			{
				ignoreSunJavaBug6529795();
			}

			@Override
			protected Iterator<Long> newTargetIterator() {
				final NodeReadTrx rtx = holder.getRtx();
				rtx.moveTo(13L);
				return VisitorDescendantAxis.newBuilder(rtx).includeSelf().build();
			}
		}.test();
	}
}
