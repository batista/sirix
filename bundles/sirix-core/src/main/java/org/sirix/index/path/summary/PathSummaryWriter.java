package org.sirix.index.path.summary;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.xml.namespace.QName;

import org.brackit.xquery.atomic.QNm;
import org.sirix.access.AbstractForwardingNodeReadTrx;
import org.sirix.access.InsertPos;
import org.sirix.access.NodeReadTrxImpl;
import org.sirix.access.Utils;
import org.sirix.api.Axis;
import org.sirix.api.NodeFactory;
import org.sirix.api.NodeReadTrx;
import org.sirix.api.PageWriteTrx;
import org.sirix.api.Session;
import org.sirix.axis.ChildAxis;
import org.sirix.axis.DescendantAxis;
import org.sirix.axis.IncludeSelf;
import org.sirix.axis.LevelOrderAxis;
import org.sirix.axis.filter.FilterAxis;
import org.sirix.axis.filter.NameFilter;
import org.sirix.axis.filter.PathKindFilter;
import org.sirix.axis.filter.PathLevelFilter;
import org.sirix.exception.SirixException;
import org.sirix.exception.SirixIOException;
import org.sirix.node.ElementNode;
import org.sirix.node.Kind;
import org.sirix.node.interfaces.NameNode;
import org.sirix.node.interfaces.Node;
import org.sirix.node.interfaces.Record;
import org.sirix.node.interfaces.StructNode;
import org.sirix.node.interfaces.immutable.ImmutableNameNode;
import org.sirix.node.interfaces.immutable.ImmutableNode;
import org.sirix.page.NamePage;
import org.sirix.page.PageKind;
import org.sirix.page.UnorderedKeyValuePage;
import org.sirix.settings.Fixed;
import org.sirix.utils.XMLToken;

/**
 * Path summary writer organizing the path classes of a resource.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public final class PathSummaryWriter extends AbstractForwardingNodeReadTrx {

	/**
	 * Operation type to determine behavior of path summary updates during
	 * {@code setQName(QName)} and the move-operations.
	 */
	public enum OPType {
		/**
		 * Move from and to is on the same level (before and after the move, the
		 * node has the same parent).
		 */
		MOVEDSAMELEVEL,

		/**
		 * Move from and to is not on the same level (before and after the move, the
		 * node has a different parent).
		 */
		MOVED,

		/** A new {@link QName} is set. */
		SETNAME,
	}

	/** Determines if a path subtree must be deleted or not. */
	private enum Remove {
		/** Yes, it must be deleted. */
		YES,

		/** No, it must not be deleted. */
		NO
	}

	/** Sirix {@link PageWriteTrx}. */
	private final PageWriteTrx<Long, Record, UnorderedKeyValuePage> mPageWriteTrx;

	/** Sirix {@link PathSummaryReader}. */
	private final PathSummaryReader mPathSummaryReader;

	/** Sirix {@link NodeFactory} to create new nodes. */
	private final NodeFactory mNodeFactory;

	/** Sirix {@link NodeReadTrxImpl} shared with the write transaction. */
	private final NodeReadTrxImpl mNodeRtx;

	/**
	 * Constructor.
	 * 
	 * @param pageWriteTrx
	 *          Sirix {@link PageWriteTrx}
	 * @param session
	 *          Sirix {@link Session}
	 * @param nodeFactory
	 *          Sirix {@link NodeFactory}
	 * @param rtx
	 *          Sirix {@link NodeReadTrxImpl}
	 */
	private PathSummaryWriter(
			final PageWriteTrx<Long, Record, UnorderedKeyValuePage> pageWriteTrx,
			final Session session, final NodeFactory nodeFactory,
			final NodeReadTrxImpl rtx) {
		mPageWriteTrx = pageWriteTrx;
		mPathSummaryReader = PathSummaryReader.getInstance(pageWriteTrx, session);
		mNodeRtx = rtx;
		mNodeFactory = nodeFactory;
	}

	/**
	 * Get a new path summary writer instance.
	 * 
	 * @param pageWriteTrx
	 *          Sirix {@link PageWriteTrx}
	 * @param session
	 *          Sirix {@link Session}
	 * @param nodeFactory
	 *          Sirix {@link NodeFactory} to create {@link PathNode} instances if
	 *          needed
	 * @param rtx
	 *          Sirix {@link NodeReadTrx}
	 * @return new path summary writer instance
	 */
	public static final PathSummaryWriter getInstance(
			final PageWriteTrx<Long, Record, UnorderedKeyValuePage> pageWriteTrx,
			final Session session, final NodeFactory nodeFactory,
			final NodeReadTrxImpl rtx) {
		// Uses the implementation of NodeReadTrx rather than the interface,
		// otherwise nodes are wrapped in immutable nodes because only getNode() is
		// available
		return new PathSummaryWriter(checkNotNull(pageWriteTrx),
				checkNotNull(session), checkNotNull(nodeFactory), checkNotNull(rtx));
	}

	/**
	 * Get the path summary reader.
	 * 
	 * @return {@link PathSummaryReader} instance
	 */
	public PathSummaryReader getPathSummary() {
		return mPathSummaryReader;
	}

	/**
	 * Insert a new path node or increment the counter of an existing node and
	 * return the path node key.
	 * 
	 * @param name
	 *          the name of the path node to search for
	 * @param pathKind
	 *          the kind of the path node to search for
	 * @return a path node key of the found node, or the path node key of a new
	 *         inserted node
	 * @throws SirixException
	 *           if anything went wrong
	 */
	public long getPathNodeKey(final QNm name, final Kind pathKind)
			throws SirixException {
		final Kind kind = mNodeRtx.getNode().getKind();
		int level = 0;
		if (kind == Kind.DOCUMENT) {
			mPathSummaryReader.moveTo(Fixed.DOCUMENT_NODE_KEY.getStandardProperty());
		} else {
			movePathSummary();
			level = mPathSummaryReader.getLevel();
		}

		final long nodeKey = mPathSummaryReader.getNodeKey();
		final Axis axis = new FilterAxis(new ChildAxis(mPathSummaryReader),
				new NameFilter(mPathSummaryReader,
						pathKind == Kind.NAMESPACE ? name.getPrefix()
								: Utils.buildName(name)), new PathKindFilter(
						mPathSummaryReader, pathKind));
		long retVal = nodeKey;
		if (axis.hasNext()) {
			axis.next();
			retVal = mPathSummaryReader.getNodeKey();
			final PathNode pathNode = (PathNode) mPageWriteTrx
					.prepareEntryForModification(retVal, PageKind.PATHSUMMARYPAGE, 0,
							Optional.<UnorderedKeyValuePage> empty());
			pathNode.incrementReferenceCount();
		} else {
			assert nodeKey == mPathSummaryReader.getNodeKey();
			insertPathAsFirstChild(name, pathKind, level + 1);
			retVal = mPathSummaryReader.getNodeKey();
		}
		return retVal;
	}

	/**
	 * Move path summary cursor to the path node which is references by the
	 * current node.
	 */
	private void movePathSummary() {
		if (mNodeRtx.getNode() instanceof ImmutableNameNode) {
			mPathSummaryReader.moveTo(((ImmutableNameNode) mNodeRtx.getNode())
					.getPathNodeKey());
		} else {
			throw new IllegalStateException();
		}
	}

	/**
	 * Insert a path node as first child.
	 * 
	 * @param name
	 *          {@link QNm} of the path node (not stored) twice
	 * @param pathKind
	 *          kind of node to index
	 * @param level
	 *          level in the path summary
	 * @return this {@link WriteTransaction} instance
	 * @throws SirixException
	 *           if an I/O error occurs
	 */
	public PathSummaryWriter insertPathAsFirstChild(final QNm name,
			final Kind pathKind, final int level) throws SirixException {
		if (!XMLToken.isValidQName(checkNotNull(name))) {
			throw new IllegalArgumentException("The QName is not valid!");
		}

		final long parentKey = mPathSummaryReader.getNodeKey();
		final long leftSibKey = Fixed.NULL_NODE_KEY.getStandardProperty();
		final long rightSibKey = mPathSummaryReader.getFirstChildKey();
		final PathNode node = mNodeFactory.createPathNode(parentKey, leftSibKey,
				rightSibKey, 0, name, pathKind, level);

		mPathSummaryReader.putMapping(node.getNodeKey(), node);
		mPathSummaryReader.moveTo(node.getNodeKey());
		adaptForInsert(node, InsertPos.ASFIRSTCHILD, PageKind.PATHSUMMARYPAGE);
		mPathSummaryReader.moveTo(node.getNodeKey());
		mPathSummaryReader.putQNameMapping(node, name);

		return this;
	}

	/**
	 * Adapting everything for insert operations.
	 * 
	 * @param newNode
	 *          pointer of the new node to be inserted
	 * @param insertPos
	 *          determines the position where to insert
	 * @param pageKind
	 *          kind of subtree root page
	 * @throws SirixIOException
	 *           if anything weird happens
	 */
	private void adaptForInsert(final Node newNode, final InsertPos insertPos,
			final PageKind pageKind) throws SirixIOException {
		assert newNode != null;
		assert insertPos != null;
		assert pageKind != null;

		if (newNode instanceof StructNode) {
			final StructNode strucNode = (StructNode) newNode;
			final StructNode parent = (StructNode) mPageWriteTrx
					.prepareEntryForModification(newNode.getParentKey(), pageKind, 0,
							Optional.<UnorderedKeyValuePage> empty());
			parent.incrementChildCount();
			if (insertPos == InsertPos.ASFIRSTCHILD) {
				parent.setFirstChildKey(newNode.getNodeKey());
			}

			if (strucNode.hasRightSibling()) {
				final StructNode rightSiblingNode = (StructNode) mPageWriteTrx
						.prepareEntryForModification(strucNode.getRightSiblingKey(),
								pageKind, 0, Optional.<UnorderedKeyValuePage> empty());
				rightSiblingNode.setLeftSiblingKey(newNode.getNodeKey());
			}
			if (strucNode.hasLeftSibling()) {
				final StructNode leftSiblingNode = (StructNode) mPageWriteTrx
						.prepareEntryForModification(strucNode.getLeftSiblingKey(),
								pageKind, 0, Optional.<UnorderedKeyValuePage> empty());
				leftSiblingNode.setRightSiblingKey(newNode.getNodeKey());
			}
		}
	}

	/**
	 * Adapt path summary either for moves or {@code setQName(QName)}.
	 * 
	 * @param node
	 *          the node for which the path node needs to be adapted
	 * @param name
	 *          the new {@link QName} in case of a new one is set, the old
	 *          {@link QName} otherwise
	 * @param nameKey
	 *          nameKey of the new node
	 * @param uriKey
	 *          uriKey of the new node
	 * @throws SirixException
	 *           if a Sirix operation fails
	 * @throws NullPointerException
	 *           if {@code pNode} or {@code pQName} is null
	 */
	public void adaptPathForChangedNode(final ImmutableNameNode node,
			final QNm name, final int uriKey, final int prefixKey,
			final int localNameKey, final OPType type) throws SirixException {
		// Possibly either reset a path node or decrement its reference counter
		// and search for the new path node or insert it.
		movePathSummary();

		final long oldPathNodeKey = mPathSummaryReader.getNodeKey();

		// Only one path node is referenced (after a setQName(QName) the
		// reference-counter would be 0).
		if (type == OPType.SETNAME && mPathSummaryReader.getReferences() == 1) {
			moveSummaryGetLevel(node);
			// Search for new path entry.
			final Axis axis = new FilterAxis(new ChildAxis(mPathSummaryReader),
					new NameFilter(mPathSummaryReader, Utils.buildName(name)),
					new PathKindFilter(mPathSummaryReader, node.getKind()));
			if (axis.hasNext()) {
				axis.next();

				// Found node.
				processFoundPathNode(oldPathNodeKey, mPathSummaryReader.getNodeKey(),
						node.getNodeKey(), uriKey, prefixKey, localNameKey, Remove.YES,
						type);
			} else {
				if (mPathSummaryReader.getKind() != Kind.DOCUMENT) {
					/* The path summary just needs to be updated for the new renamed node. */
					mPathSummaryReader.moveTo(oldPathNodeKey);
					final PathNode pathNode = (PathNode) mPageWriteTrx
							.prepareEntryForModification(mPathSummaryReader.getNodeKey(),
									PageKind.PATHSUMMARYPAGE, 0,
									Optional.<UnorderedKeyValuePage> empty());
					pathNode.setPrefixKey(prefixKey);
					pathNode.setLocalNameKey(localNameKey);
					pathNode.setURIKey(uriKey);
				}
			}
		} else {
			int level = moveSummaryGetLevel(node);
			// TODO: Johannes: Optimize? (either use this or use the name-mapping,
			// depending on the number of child nodes or nodes with a certain name).

			// Search for new path entry.
			final Axis axis = new FilterAxis(new ChildAxis(mPathSummaryReader),
					new NameFilter(mPathSummaryReader, Utils.buildName(name)),
					new PathKindFilter(mPathSummaryReader, node.getKind()));
			if (type == OPType.MOVEDSAMELEVEL || axis.hasNext()) {
				if (type != OPType.MOVEDSAMELEVEL) {
					axis.next();
				}

				// Found node.
				processFoundPathNode(oldPathNodeKey, mPathSummaryReader.getNodeKey(),
						node.getNodeKey(), uriKey, prefixKey, localNameKey, Remove.NO, type);
			} else {
				long nodeKey = mPathSummaryReader.getNodeKey();
				// Decrement reference count or remove path summary node.
				mNodeRtx.moveTo(node.getNodeKey());
				final Set<Long> nodesToDelete = new HashSet<>();
				for (final Axis descendants = new DescendantAxis(mNodeRtx,
						IncludeSelf.YES); descendants.hasNext();) {
					descendants.next();
					deleteOrDecrement(nodesToDelete);
					if (mNodeRtx.getKind() == Kind.ELEMENT) {
						final ElementNode element = (ElementNode) mNodeRtx.getCurrentNode();

						// Namespaces.
						for (int i = 0, nsps = element.getNamespaceCount(); i < nsps; i++) {
							mNodeRtx.moveToNamespace(i);
							deleteOrDecrement(nodesToDelete);
							mNodeRtx.moveToParent();
						}

						// Attributes.
						for (int i = 0, atts = element.getAttributeCount(); i < atts; i++) {
							mNodeRtx.moveToAttribute(i);
							deleteOrDecrement(nodesToDelete);
							mNodeRtx.moveToParent();
						}
					}
				}

				mPathSummaryReader.moveTo(nodeKey);

				// Not found => create new path nodes for the whole subtree.
				boolean firstRun = true;
				for (final Axis descendants = new DescendantAxis(mNodeRtx,
						IncludeSelf.YES); descendants.hasNext();) {
					descendants.next();
					if (mNodeRtx.getKind() == Kind.ELEMENT) {
						// Path Summary : New mapping.
						if (firstRun) {
							insertPathAsFirstChild(name, Kind.ELEMENT, ++level);
							nodeKey = mPathSummaryReader.getNodeKey();
						} else {
							insertPathAsFirstChild(mNodeRtx.getName(), Kind.ELEMENT, ++level);
						}
						resetPathNodeKey(mNodeRtx.getNodeKey());

						// Namespaces.
						for (int i = 0, nsps = mNodeRtx.getNamespaceCount(); i < nsps; i++) {
							mNodeRtx.moveToNamespace(i);
							// Path Summary : New mapping.
							insertPathAsFirstChild(mNodeRtx.getName(), Kind.NAMESPACE,
									level + 1);
							resetPathNodeKey(mNodeRtx.getNodeKey());
							mNodeRtx.moveToParent();
							mPathSummaryReader.moveToParent();
						}

						// Attributes.
						for (int i = 0, atts = mNodeRtx.getAttributeCount(); i < atts; i++) {
							mNodeRtx.moveToAttribute(i);
							// Path Summary : New mapping.
							insertPathAsFirstChild(mNodeRtx.getName(), Kind.ATTRIBUTE,
									level + 1);
							resetPathNodeKey(mNodeRtx.getNodeKey());
							mNodeRtx.moveToParent();
							mPathSummaryReader.moveToParent();
						}

						if (firstRun) {
							firstRun = false;
						} else {
							mPathSummaryReader.moveToParent();
							level--;
						}
					}
				}

				/*
				 * Remove path nodes with zero node references.
				 * 
				 * (TODO: Johannes: might not be necessary, as it's likely that future
				 * updates will reinsert the path).
				 */
				for (final long key : nodesToDelete) {
					mPathSummaryReader.moveTo(key);
					removePathSummaryNode(Remove.NO);
				}

				mPathSummaryReader.moveTo(nodeKey);
			}
		}
	}

	/**
	 * Process a found path node.
	 * 
	 * @param oldPathNodeKey
	 *          key of old path node
	 * @param newPathNodeKey
	 *          key of new path node
	 * @param oldNodeKey
	 *          key of old node
	 * @param uriKey
	 *          key of URI
	 * @param prefixKey
	 *          key of prefix
	 * @param localNameKey
	 *          key of local name
	 * @param remove
	 *          determines if a {@link PathNode} must be removed or not
	 * @param type
	 *          type of operation
	 * @throws SirixException
	 *           if Sirix fails to do so
	 */
	private void processFoundPathNode(final @Nonnegative long oldPathNodeKey,
			final @Nonnegative long newPathNodeKey,
			final @Nonnegative long oldNodeKey, final int uriKey,
			final int prefixKey, final int localNameKey, final Remove remove,
			final OPType type) throws SirixException {
		final PathSummaryReader cloned = PathSummaryReader.getInstance(
				mPageWriteTrx, mNodeRtx.getSession());
		boolean moved = cloned.moveTo(oldPathNodeKey).hasMoved();
		assert moved;

		// Set new reference count of the root.
		if (type != OPType.MOVEDSAMELEVEL) {
			final PathNode currNode = (PathNode) mPageWriteTrx
					.prepareEntryForModification(mPathSummaryReader.getNodeKey(),
							PageKind.PATHSUMMARYPAGE, 0,
							Optional.<UnorderedKeyValuePage> empty());
			currNode.setReferenceCount(currNode.getReferences()
					+ cloned.getReferences());
			currNode.setLocalNameKey(localNameKey);
			currNode.setPrefixKey(prefixKey);
			currNode.setURIKey(uriKey);
		}

		// For all old path nodes: Merge paths and adapt reference counts.
		mPathSummaryReader.moveToFirstChild();
		final int oldLevel = cloned.getLevel();
		for (final Axis oldDescendants = new DescendantAxis(cloned); oldDescendants
				.hasNext();) {
			oldDescendants.next();

			// Search for new path entry.
			final Axis axis = new FilterAxis(new LevelOrderAxis.Builder(
					mPathSummaryReader).filterLevel(cloned.getLevel() - oldLevel)
					.includeSelf().build(), new NameFilter(mPathSummaryReader,
					Utils.buildName(cloned.getName())), new PathKindFilter(
					mPathSummaryReader, cloned.getPathKind()), new PathLevelFilter(
					mPathSummaryReader, cloned.getLevel()));
			if (axis.hasNext()) {
				axis.next();

				// Set new reference count.
				if (type != OPType.MOVEDSAMELEVEL) {
					final PathNode currNode = (PathNode) mPageWriteTrx
							.prepareEntryForModification(mPathSummaryReader.getNodeKey(),
									PageKind.PATHSUMMARYPAGE, 0,
									Optional.<UnorderedKeyValuePage> empty());
					currNode.setReferenceCount(currNode.getReferences()
							+ cloned.getReferences());
				}
			} else {
				// Insert new node.
				insertPathAsFirstChild(cloned.getName(), cloned.getPathKind(),
						mPathSummaryReader.getLevel() + 1);

				// Set new reference count.
				final PathNode currNode = (PathNode) mPageWriteTrx
						.prepareEntryForModification(mPathSummaryReader.getNodeKey(),
								PageKind.PATHSUMMARYPAGE, 0,
								Optional.<UnorderedKeyValuePage> empty());
				currNode.setReferenceCount(cloned.getReferences());
			}
			mPathSummaryReader.moveTo(newPathNodeKey);
		}

		// Set new path nodes of the changed nodes, that is set their PCR
		// references.
		mPathSummaryReader.moveTo(newPathNodeKey);
		mNodeRtx.moveTo(oldNodeKey);

		boolean first = true;
		for (final Axis axis = new DescendantAxis(mNodeRtx, IncludeSelf.YES); axis
				.hasNext();) {
			axis.next();

			if (first && type == OPType.SETNAME) {
				first = false;
			} else if (mNodeRtx.getNode() instanceof ImmutableNameNode) {
				cloned.moveTo(((NameNode) mNodeRtx.getCurrentNode()).getPathNodeKey());
				resetPath(newPathNodeKey, cloned.getLevel());

				if (mNodeRtx.getNode().getKind() == Kind.ELEMENT) {
					final ElementNode element = (ElementNode) mNodeRtx.getCurrentNode();

					for (int i = 0, nspCount = element.getNamespaceCount(); i < nspCount; i++) {
						mNodeRtx.moveToNamespace(i);
						cloned.moveTo(((NameNode) mNodeRtx.getCurrentNode())
								.getPathNodeKey());
						resetPath(newPathNodeKey, cloned.getLevel());
						mNodeRtx.moveToParent();
					}
					for (int i = 0, attCount = element.getAttributeCount(); i < attCount; i++) {
						mNodeRtx.moveToAttribute(i);
						cloned.moveTo(((NameNode) mNodeRtx.getCurrentNode())
								.getPathNodeKey());
						resetPath(newPathNodeKey, cloned.getLevel());
						mNodeRtx.moveToParent();
					}
				}
			}
		}

		// Then: Remove old nodes.
		if (remove == Remove.YES) {
			mPathSummaryReader.moveTo(oldPathNodeKey);
			removePathSummaryNode(remove);
		}
	}

	/**
	 * Move path summary to the associated {@code parent} {@link PathNode} and get
	 * the level of the node.
	 * 
	 * @param node
	 *          the node to lookup for it's {@link PathNode}
	 * @return level of the path node
	 */
	private int moveSummaryGetLevel(final ImmutableNode node) {
		assert node != null;
		// Get parent path node and level.
		mNodeRtx.moveToParent();
		int level = 0;
		if (mNodeRtx.getKind() == Kind.DOCUMENT) {
			mPathSummaryReader.moveToDocumentRoot();
		} else {
			movePathSummary();
			level = mPathSummaryReader.getLevel();
		}
		mNodeRtx.moveTo(node.getNodeKey());
		return level;
	}

	/**
	 * Reset a path node key.
	 * 
	 * @param nodeKey
	 *          the nodeKey of the node to adapt
	 * @throws SirixException
	 *           if anything fails
	 */
	private void resetPathNodeKey(final @Nonnegative long nodeKey)
			throws SirixException {
		final NameNode currNode = (NameNode) mPageWriteTrx
				.prepareEntryForModification(nodeKey, PageKind.RECORDPAGE, -1,
						Optional.<UnorderedKeyValuePage> empty());
		currNode.setPathNodeKey(mPathSummaryReader.getNodeKey());
	}

	/**
	 * Remove a path summary node with the specified PCR.
	 * 
	 * @throws SirixException
	 *           if Sirix fails to remove the path node
	 */
	private void removePathSummaryNode(final Remove remove) throws SirixException {
		// Remove all descendant nodes.
		if (remove == Remove.YES) {
			for (final Axis axis = new DescendantAxis(mPathSummaryReader); axis
					.hasNext();) {
				axis.next();
				mPathSummaryReader.removeMapping(mPathSummaryReader.getNodeKey());
				mPathSummaryReader.removeQNameMapping(mPathSummaryReader.getPathNode(),
						mPathSummaryReader.getName());
				mPageWriteTrx.removeEntry(mPathSummaryReader.getNodeKey(),
						PageKind.PATHSUMMARYPAGE, 0,
						Optional.<UnorderedKeyValuePage> empty());
			}
		}

		// Adapt left sibling node if there is one.
		if (mPathSummaryReader.hasLeftSibling()) {
			final StructNode leftSibling = (StructNode) mPageWriteTrx
					.prepareEntryForModification(mPathSummaryReader.getLeftSiblingKey(),
							PageKind.PATHSUMMARYPAGE, 0,
							Optional.<UnorderedKeyValuePage> empty());
			leftSibling.setRightSiblingKey(mPathSummaryReader.getRightSiblingKey());
		}

		// Adapt right sibling node if there is one.
		if (mPathSummaryReader.hasRightSibling()) {
			final StructNode rightSibling = (StructNode) mPageWriteTrx
					.prepareEntryForModification(mPathSummaryReader.getRightSiblingKey(),
							PageKind.PATHSUMMARYPAGE, 0,
							Optional.<UnorderedKeyValuePage> empty());
			rightSibling.setLeftSiblingKey(mPathSummaryReader.getLeftSiblingKey());
		}

		// Adapt parent. If node has no left sibling it is a first child.
		StructNode parent = (StructNode) mPageWriteTrx.prepareEntryForModification(
				mPathSummaryReader.getParentKey(), PageKind.PATHSUMMARYPAGE, 0,
				Optional.<UnorderedKeyValuePage> empty());
		if (!mPathSummaryReader.hasLeftSibling()) {
			parent.setFirstChildKey(mPathSummaryReader.getRightSiblingKey());
		}
		parent.decrementChildCount();

		// Remove node.
		mPathSummaryReader.removeMapping(mPathSummaryReader.getNodeKey());
		mPathSummaryReader.removeQNameMapping(mPathSummaryReader.getPathNode(),
				mPathSummaryReader.getName());
		mPageWriteTrx.removeEntry(mPathSummaryReader.getNodeKey(),
				PageKind.PATHSUMMARYPAGE, 0, Optional.<UnorderedKeyValuePage> empty());
	}

	/**
	 * Reset the path node key of a node.
	 * 
	 * @param newPathNodeKey
	 *          path node key of new path node
	 * @param oldLevel
	 *          old level of node
	 * @throws SirixIOException
	 *           if an I/O error occurs
	 */
	private void resetPath(final @Nonnegative long newPathNodeKey,
			final @Nonnegative int oldLevel) throws SirixIOException {
		// Search for new path entry.
		mPathSummaryReader.moveTo(newPathNodeKey);
		final Axis filterAxis = new FilterAxis(new LevelOrderAxis.Builder(
				mPathSummaryReader).includeSelf().build(), new NameFilter(
				mPathSummaryReader, Utils.buildName(mNodeRtx.getName())),
				new PathKindFilter(mPathSummaryReader, mNodeRtx.getKind()),
				new PathLevelFilter(mPathSummaryReader, oldLevel));
		if (filterAxis.hasNext()) {
			filterAxis.next();

			// Set new path node.
			final NameNode node = (NameNode) mPageWriteTrx
					.prepareEntryForModification(mNodeRtx.getNodeKey(),
							PageKind.RECORDPAGE, -1,
							Optional.<UnorderedKeyValuePage> empty());
			node.setPathNodeKey(mPathSummaryReader.getNodeKey());
		} else {
			throw new IllegalStateException();
		}
	}

	/**
	 * Schedule for deletion of decrement path reference counter.
	 * 
	 * @param nodesToDelete
	 *          stores nodeKeys which should be deleted
	 * @throws SirixIOException
	 *           if an I/O error occurs while decrementing the reference counter
	 */
	private void deleteOrDecrement(final Set<Long> nodesToDelete)
			throws SirixIOException {
		if (mNodeRtx.getNode() instanceof ImmutableNameNode) {
			movePathSummary();
			if (mPathSummaryReader.getReferences() == 1) {
				nodesToDelete.add(mPathSummaryReader.getNodeKey());
			} else {
				final PathNode pathNode = (PathNode) mPageWriteTrx
						.prepareEntryForModification(mPathSummaryReader.getNodeKey(),
								PageKind.PATHSUMMARYPAGE, 0,
								Optional.<UnorderedKeyValuePage> empty());
				pathNode.decrementReferenceCount();
			}
		}
	}

	/**
	 * Decrements the reference-counter of the node or removes the path node if
	 * the reference-counter would be zero otherwise.
	 * 
	 * @param node
	 *          node which is going to removed from the storage
	 * @param nodeKind
	 *          the node kind
	 * @param page
	 *          the name page
	 * @throws SirixException
	 *           if anything went wrong
	 */
	public void remove(final NameNode node, final Kind nodeKind,
			final NamePage page) throws SirixException {
		if (mPathSummaryReader.moveTo(node.getPathNodeKey()).hasMoved()) {
			if (mPathSummaryReader.getReferences() == 1) {
				removePathSummaryNode(Remove.YES);
			} else {
				assert page.getCount(node.getLocalNameKey(), nodeKind) != 0;
				if (mPathSummaryReader.getReferences() > 1) {
					final PathNode pathNode = (PathNode) mPageWriteTrx
							.prepareEntryForModification(mPathSummaryReader.getNodeKey(),
									PageKind.PATHSUMMARYPAGE, 0,
									Optional.<UnorderedKeyValuePage> empty());
					pathNode.decrementReferenceCount();
				}
			}
		}
	}

	@Override
	protected NodeReadTrx delegate() {
		return mPathSummaryReader;
	}
}
