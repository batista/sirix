package org.sirix.xquery.function.sdb.index.create;

import java.util.HashSet;
import java.util.Set;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.sirix.access.IndexController;
import org.sirix.api.NodeReadTrx;
import org.sirix.api.NodeWriteTrx;
import org.sirix.exception.SirixIOException;
import org.sirix.index.IndexDef;
import org.sirix.index.IndexDefs;
import org.sirix.index.IndexType;
import org.sirix.xquery.function.sdb.SDBFun;
import org.sirix.xquery.node.DBNode;

import com.google.common.collect.ImmutableSet;

/**
 * Function for creating name indexes on stored documents, optionally restricted
 * to a set of included {@code QNm}s. If successful, this function returns
 * statistics about the newly created index as an XML fragment. Supported
 * signatures are:</br>
 * <ul>
 * <li>
 * <code>sdb:create-path-index($doc as node(), $include as xs:QName*) as 
 * node()</code></li>
 * <li>
 * <code>sdb:create-path-index($doc as node()) as node()</code></li>
 * </ul>
 * 
 * @author Max Bechtold
 * @author Johannes Lichtenberger
 * 
 */
public final class CreateNameIndex extends AbstractFunction {

	/** Path index function name. */
	public final static QNm CREATE_NAME_INDEX = new QNm(SDBFun.SDB_NSURI,
			SDBFun.SDB_PREFIX, "create-name-index");

	/**
	 * Constructor.
	 * 
	 * @param name
	 *          the name of the function
	 * @param signature
	 *          the signature of the function
	 */
	public CreateNameIndex(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(final StaticContext sctx, final QueryContext ctx,
			final Sequence[] args) throws QueryException {
		if (args.length != 2 && args.length != 3) {
			throw new QueryException(new QNm("No valid arguments specified!"));
		}

		final DBNode doc = ((DBNode) args[0]);
		final NodeReadTrx rtx = doc.getTrx();
		final IndexController controller = rtx.getSession().getWtxIndexController(
				rtx.getRevisionNumber() - 1);

		if (!(doc.getTrx() instanceof NodeWriteTrx)) {
			throw new QueryException(new QNm("Collection must be updatable!"));
		}

		if (controller == null) {
			throw new QueryException(new QNm("Document not found: "
					+ ((Str) args[1]).stringValue()));
		}

		if (!(doc.getTrx() instanceof NodeWriteTrx)) {
			throw new QueryException(new QNm("Collection must be updatable!"));
		}

		final Set<QNm> include = new HashSet<>();
		if (args.length > 1 && args[1] != null) {
			final Iter it = args[1].iterate();
			Item next = it.next();
			while (next != null) {
				include.add((QNm) next);
				next = it.next();
			}
		}

		final IndexDef idxDef = IndexDefs.createSelectiveNameIdxDef(include,
				controller.getIndexes().getNrOfIndexDefsWithType(IndexType.NAME));
		try {
			controller.createIndexes(ImmutableSet.of(idxDef),
					(NodeWriteTrx) doc.getTrx());
		} catch (final SirixIOException e) {
			throw new QueryException(new QNm("I/O exception: " + e.getMessage()), e);
		}
		return idxDef.materialize();
	}

}