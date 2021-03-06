/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;

import apex.jorje.semantic.ast.statement.StatementExecuted;

public class ASTStatementExecuted extends AbstractApexNode<StatementExecuted> {

    public ASTStatementExecuted(StatementExecuted node) {
        super(node);
    }

    public Object jjtAccept(ApexParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
