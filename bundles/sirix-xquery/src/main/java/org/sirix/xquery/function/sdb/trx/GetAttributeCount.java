package org.sirix.xquery.function.sdb.trx;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.sirix.xquery.function.sdb.SDBFun;
import org.sirix.xquery.node.DBNode;

/**
 * <p>
 * Function for getting the number of attributes of the current node. Supported
 * signature is:
 * </p>
 * <ul>
 * <li>
 * <code>sdb:get-attribute-count($doc as xs:node) as xs:int</code></li>
 * </ul>
 *
 * @author Johannes Lichtenberger
 *
 */
public final class GetAttributeCount extends AbstractFunction {

	/** Get most recent revision function name. */
	public final static QNm GET_ATTRIBUTE_COUNT = new QNm(SDBFun.SDB_NSURI,
			SDBFun.SDB_PREFIX, "get-attribute-count");

	/**
	 * Constructor.
	 *
	 * @param name
	 *          the name of the function
	 * @param signature
	 *          the signature of the function
	 */
	public GetAttributeCount(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args)
			throws QueryException {
		final DBNode doc = ((DBNode) args[0]);

		return new Int32(doc.getTrx().getAttributeCount());
	}
}
