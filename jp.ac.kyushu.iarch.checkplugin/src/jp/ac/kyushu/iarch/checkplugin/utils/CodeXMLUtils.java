package jp.ac.kyushu.iarch.checkplugin.utils;

import java.util.LinkedList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CodeXMLUtils {
	public static Document getProjectXML(IJavaProject project) throws JavaModelException {
		Document document = DocumentHelper.createDocument();
		generateProjectXML(project, document);
		return document;
	}
	public static void generateProjectXML(IJavaProject project, Document document)
			throws JavaModelException {
		Element rootElement = document.addElement("Project");
		rootElement.addAttribute("name", project.getElementName());

		// IJavaProject#getPackageFragments might be convenient,
		// but we cannot assure the order is same.
		for (IPackageFragmentRoot pkgRoot: project.getPackageFragmentRoots()) {
			for (IJavaElement pkg: pkgRoot.getChildren()) {
				Element pkgElement = rootElement.addElement("Package");
				pkgElement.addAttribute("name", pkg.getElementName());

				if (pkg instanceof IPackageFragment){
					for (ICompilationUnit file: ((IPackageFragment) pkg).getCompilationUnits()){
						generateCodeXML(file, pkgElement);
					}
				}
			}
		}
	}

	public static Element getCodeXML(ICompilationUnit file, String name) {
		Element pkgElement = DocumentHelper.createElement("Package");
		if (name != null) {
			pkgElement.addAttribute("name", name);
		}
		generateCodeXML(file, pkgElement);
		return pkgElement;
	}
	public static void generateCodeXML(ICompilationUnit file, Element pkgElement) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(file);
		//parser.setResolveBindings(true);
		ASTNode rootnode = parser.createAST(null);
		// Normally rootnode is a CompilationUnit, otherwise something is too wrong...
		ExtractVisitor visitor = new ExtractVisitor((CompilationUnit) rootnode, pkgElement);
		rootnode.accept(visitor);
	}

	private static class ExtractVisitor extends ASTVisitor {
		private CompilationUnit compilationUnit;
		private Element rootElement;
		private Element classElement = null;
		private Element methodElement = null;

		private ExtractVisitor(CompilationUnit compilationUnit, Element rootElement) {
			super(true);
			this.compilationUnit = compilationUnit;
			this.rootElement = rootElement;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			String nodeType = (node.isInterface() == true) ? "Interface" : "Class";
			classElement = rootElement.addElement(nodeType);

			classElement.addAttribute("name", node.getName().toString());

			if (!node.isInterface()) {
				for (Object obj: node.superInterfaceTypes()) {
					Element superInterfaceElement = classElement.addElement("superInterfaceTypes");
					superInterfaceElement.addText(((Type) obj).toString());
				}
			}

			int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
			classElement.addAttribute("lineNumber", String.valueOf(lineNumber));

			Element modifiersElement = classElement.addElement("ClassModifiers");
			for (String modifier: convertModifiers(node.getModifiers())) {
				Element modifierElement = modifiersElement.addElement("modifier");
				modifierElement.setText(modifier);
			}

			return super.visit(node);
		}
		@Override
		public void endVisit(TypeDeclaration node) {
			classElement = null;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			// メソッドが定義されているクラスがない(抽象クラスなど)場合はXMLに登録しない
			if (classElement != null) {
				methodElement = classElement.addElement("MethodDeclaration");

				int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
				methodElement.addAttribute("lineNumber", String.valueOf(lineNumber));

				methodElement.addAttribute("name", node.getName().toString());

				Type returnType = node.getReturnType2();
				String methodReturnType = (returnType != null) ? returnType.toString() : "";

				// Modifiers of MethodDeclaration
				Element modifiersElement = methodElement.addElement("MethodModifiers");
				List<String> modifiers = convertModifiers(node.getModifiers());
				if (methodReturnType.equals("void") && !(modifiers.get(0).toString().equals("void"))) {
					modifiers.add("void");
				}
				for (String modifier : modifiers) {
					Element modifierElement = modifiersElement.addElement("modifier");
					modifierElement.setText(modifier);
				}

				// method parameterType
				if (node.parameters().size() != 0) {
					Element parameterTypeElement = methodElement.addElement("parameterTypeElement");
					for (Object obj: node.parameters()) {
						SingleVariableDeclaration methodParameter = (SingleVariableDeclaration) obj;
						String fType = methodParameter.toString().split(" ")[0];
						Element parameterType = parameterTypeElement.addElement("parameterType");
						parameterType.setText(fType);
					}
				}

				// method return type
				if (!methodReturnType.equals("void")) {
					Element returnTypeElement = methodElement.addElement("returnType");
					returnTypeElement.setText(methodReturnType);
				}
			}
			return super.visit(node);
		}
		@Override
		public void endVisit(MethodDeclaration node) {
			methodElement = null;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			// メソッド呼び出し元がない(クラスでの定数宣言など)場合はXMLに登録しない
			if (methodElement != null) {
				Element methodInvocationElement = methodElement.addElement("MethodInvocation");

				methodInvocationElement.addAttribute("name", node.getName().toString());
				//System.out.println("Method Invocation:" + node.getName());

				Element invocationExpressionElement = methodInvocationElement.addElement("InvocationExpression");
				if (node.getExpression() != null) {
					invocationExpressionElement.addText(node.getExpression().toString());
				}
			}
			return super.visit(node);
		}
	}

	private static List<String> convertModifiers(int modValue) {
		// TODO: It is incorrect, but original procedure is kept for a while.
		List<String> modifiers = new LinkedList<String>();
		String modifier = null;

		if (modValue == Flags.AccDefault) modifier = "void";
		if (modValue == Flags.AccPublic) modifier = " public";
		if (modValue == Flags.AccPrivate) modifier = " private";
		if (modValue == Flags.AccProtected)
			if (modValue == Flags.AccStatic)
				modifier = " protective";
		modifier = " static";
		if (modValue == Flags.AccFinal) modifier = " final";
		if (modValue == Flags.AccSynchronized) modifier = "synchronized";
		if (modValue == Flags.AccVolatile) modifier = "  volatile";

		if (modValue == Flags.AccTransient) modifier = " transient";
		if (modValue == Flags.AccNative) modifier = " native";
		if (modValue == Flags.AccInterface) modifier = "interface";
		if (modValue == Flags.AccAbstract) modifier = " abstract";
		if (modValue == Flags.AccStrictfp) modifier = "strict";

		if (modifier != null) modifiers.add(modifier);

		if (modValue == (Flags.AccPublic | Flags.AccStatic)) {
			modifiers.add(" public");
			modifiers.add("  static");
		}

		if (modValue == Flags.AccPrivate) modifier = " private";
		if (modValue == Flags.AccProtected) modifier = " protective";
		if (modValue == Flags.AccProtected) modifier = " protective";

		return modifiers;
	}

	/**
	 * Find a ASTNode which satisfies given condition.
	 * Note that it has a limitation that it can find only one node.
	 */
	public static class FindVisitor extends ASTVisitor {
		private FindVisitorCondition condition;
		public void setCondition(FindVisitorCondition condition) {
			this.condition = condition;
		}

		private ASTNode target = null;
		public ASTNode getTarget() { return target; }

		protected FindVisitor(FindVisitorCondition condition) {
			super(false);
			this.condition = condition;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			// Stop visiting nodes if target is already found.
			return target == null;
		}

		/**
		 * To be called by visit methods of classes which you want to check.
		 */
		protected boolean visitCommon(ASTNode node) {
			if (condition != null && condition.isTarget(node)) {
				target = node;
				return false;
			}
			return true;
		}
	}
	private abstract static class FindVisitorCondition {
		public abstract boolean isTarget(ASTNode node);
	}

	private static class FindByLineCondition extends FindVisitorCondition {
		private CompilationUnit compilationUnit;
		private int targetLine;

		private FindByLineCondition(CompilationUnit compilationUnit, int targetLine) {
			this.compilationUnit = compilationUnit;
			this.targetLine = targetLine;
		}

		@Override
		public boolean isTarget(ASTNode node) {
			int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
			return lineNumber == targetLine;
		}
	}
	public static ASTNode findNodeByLine(ICompilationUnit file, int targetLine, FindVisitor visitor) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(file);
		parser.setResolveBindings(true);
		ASTNode rootnode = parser.createAST(null);
		// Normally rootnode is a CompilationUnit, otherwise something is too wrong...
		FindByLineCondition condition = new FindByLineCondition((CompilationUnit) rootnode, targetLine);
		visitor.setCondition(condition);
		rootnode.accept(visitor);
		return visitor.getTarget();
	}
}
