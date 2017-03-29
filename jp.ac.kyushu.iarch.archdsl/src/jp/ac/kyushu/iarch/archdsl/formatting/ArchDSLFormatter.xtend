/*
 * generated by Xtext
 */
package jp.ac.kyushu.iarch.archdsl.formatting

import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter
import org.eclipse.xtext.formatting.impl.FormattingConfig
import jp.ac.kyushu.iarch.archdsl.services.ArchDSLGrammarAccess

// import com.google.inject.Inject;
// import jp.ac.kyushu.iarch.archdsl.services.ArchDSLGrammarAccess

/**
 * This class contains custom formatting description.
 * 
 * see : http://www.eclipse.org/Xtext/documentation.html#formatting
 * on how and when to use it 
 * 
 * Also see {@link org.eclipse.xtext.xtext.XtextFormattingTokenSerializer} as an example
 */
class ArchDSLFormatter extends AbstractDeclarativeFormatter {

//	@Inject extension ArchDSLGrammarAccess
	
	override protected void configureFormatting(FormattingConfig c) {
// It's usually a good idea to activate the following three statements.
// They will add and preserve newlines around comments
//		c.setLinewrap(0, 1, 2).before(SL_COMMENTRule)
//		c.setLinewrap(0, 1, 2).before(ML_COMMENTRule)
//		c.setLinewrap(0, 1, 1).after(ML_COMMENTRule)

		val f = getGrammarAccess() as ArchDSLGrammarAccess

		c.setAutoLinewrap(160)

		// No space around parentheses(and semicolon)
		for (p : f.findKeywordPairs("(", ");")) {
			c.setSpace("").before(p.getFirst())
			c.setNoSpace().after(p.getFirst())
			c.setNoSpace().before(p.getSecond())
		}
		// No space before comma
		for (comma : f.findKeywords(",")) {
			c.setNoSpace().before(comma)
		}
		// Cancel empty space settings.
		for (p : f.findKeywords("=")) {
			c.setSpace(" ").around(p)
		}

		// Component
		c.setLinewrap().after(f.getInterfaceAccess().getLeftCurlyBracketKeyword_2())
		c.setLinewrap().after(f.getInterfaceAccess().getMethodsAssignment_3())
		c.setLinewrap().after(f.getInterfaceRule())
		c.setIndentationIncrement().after(f.getInterfaceAccess().getLeftCurlyBracketKeyword_2())
		c.setIndentationDecrement().before(f.getInterfaceAccess().getRightCurlyBracketKeyword_4())

		// Uncertain Component
		c.setLinewrap().after(f.getUncertainInterfaceAccess().getLeftCurlyBracketKeyword_3())
		c.setLinewrap().after(f.getUncertainInterfaceAccess().getAltmethodsAssignment_4_0())
		c.setLinewrap().after(f.getUncertainInterfaceAccess().getOptmethodsAssignment_4_1())
		c.setLinewrap().after(f.getUncertainInterfaceRule())
		c.setIndentationIncrement().after(f.getUncertainInterfaceAccess().getLeftCurlyBracketKeyword_3())
		c.setIndentationDecrement().before(f.getUncertainInterfaceAccess().getRightCurlyBracketKeyword_5())

		// Connector
		c.setLinewrap().after(f.getConnectorAccess().getLeftCurlyBracketKeyword_2())
		c.setLinewrap().after(f.getConnectorAccess().getBehaviorsAssignment_3())
		c.setLinewrap().after(f.getConnectorRule())
		c.setIndentationIncrement().after(f.getConnectorAccess().getLeftCurlyBracketKeyword_2())
		c.setIndentationDecrement().before(f.getConnectorAccess().getRightCurlyBracketKeyword_4())

		// Uncertain Connector
		c.setLinewrap().after(f.getUncertainConnectorAccess().getLeftCurlyBracketKeyword_3())
		c.setLinewrap().after(f.getUncertainConnectorAccess().getU_behaviorsAssignment_4())
		c.setLinewrap().after(f.getUncertainConnectorRule())
		c.setIndentationIncrement().after(f.getUncertainConnectorAccess().getLeftCurlyBracketKeyword_3())
		c.setIndentationDecrement().before(f.getUncertainConnectorAccess().getRightCurlyBracketKeyword_5())

		// Behavior
		c.setLinewrap().after(f.getBehaviorRule())
	}
}