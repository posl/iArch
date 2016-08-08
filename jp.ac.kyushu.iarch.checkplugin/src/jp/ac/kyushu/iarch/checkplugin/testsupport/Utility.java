package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author watanabeke
 */
public final class Utility {

	private Utility() {}

	/**
	 * iteratorから得られる文字列を、separatorを挟みながら連結します。
	 * @param iterator
	 * @param separator
	 * @return
	 */
	public static String join(Iterator<String> iterator, String separator) {
		String result = "";
		for ( ; iterator.hasNext(); ) {
			result += iterator.next();
			if (iterator.hasNext()) result += separator;
		}
		return result;
	}

	/**
	 * eclipseの標準APIを使用して、Javaコードを整形します。
	 * @param code
	 * @return
	 */
	public static String formatJavaCode(String code) {
		try {
			CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);
			TextEdit textEdit = codeFormatter.format(
					CodeFormatter.K_UNKNOWN, code, 0, code.length(), 0, null);
			IDocument document = new Document(code);
			textEdit.apply(document);  //textEditはnullの可能性がある
			return document.get();
		} catch (MalformedTreeException | BadLocationException | NullPointerException e) {
			e.printStackTrace();
			return code;  // 例外時はフォーマットせずに返す
		}
	}

	/**
	 * Javaの標準APIを利用して、XMLコードを整形します。
	 * @param code
	 * @return
	 */
	public static String formatXMLCode(String code, final int indentAmount) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
			InputStream inputStream = new ByteArrayInputStream(code.getBytes());
			StreamSource streamSource = new StreamSource(inputStream);
			OutputStream outputStream = new ByteArrayOutputStream();
			StreamResult streamResult = new StreamResult(outputStream);
			transformer.transform(streamSource, streamResult);
			return outputStream.toString();
		} catch (IllegalArgumentException
				| TransformerFactoryConfigurationError | TransformerException e) {
			e.printStackTrace();
			return code;  // 例外時はフォーマットせずに返す
		}
	}

	public static String formatXMLCode(String code) {
		return Utility.formatXMLCode(code, 2);
	}

	/**
	 * 0, 1も受理できるString -> boolean変換です。
	 */
	public static boolean parseBoolean(String s) {
		try {
			return Integer.parseInt(s) != 0;
		} catch (NumberFormatException e) {
			return s.equals("true");
		}
	}

	/**
	 * NodeListに対して拡張for文を使えるようにします。
	 * @param nodeList
	 * @return
	 */
	public static Iterable<Node> iterate(NodeList nodeList) {
		class NodeListIterator implements Iterator<Node> {

			private NodeList nodeList;
			private int index;

			public NodeListIterator(NodeList nodeList) {
				this.nodeList = nodeList;
				this.index = 0;
			}

			@Override
			public boolean hasNext() {
				return nodeList.getLength() > index;
			}

			@Override
			public Node next() {
				Node next = nodeList.item(index);
				if (next == null) {
					throw new NoSuchElementException();
				} else {
					index++;
					return next;
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		}

		class NodeListIterable implements Iterable<Node> {

			private Iterator<Node> iterator;

			public NodeListIterable(NodeList nodeList) {
				this.iterator = new NodeListIterator(nodeList);
			}

			@Override
			public Iterator<Node> iterator() {
				return iterator;
			}

		}

		return new NodeListIterable(nodeList);
	}

	/**
	 * ファイル名から拡張子を取り除きます。
	 */
	public static String withoutSuffix(String name) {
		return name.replaceFirst("[.][^.]+$", "");
	}

}
