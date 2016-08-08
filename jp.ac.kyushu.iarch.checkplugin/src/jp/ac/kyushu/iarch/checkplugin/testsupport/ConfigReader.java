package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * メソッドの選択が記述された構成ファイルを読み込み、
 * SelectionInfoオブジェクトへ変換します。
 * 構成ファイルはConfig.xsdで定義されるXMLファイルです。
 * @author watanabeke
 */
public class ConfigReader implements IInfoGenerateReader {

	private static final ConfigReader instance = new ConfigReader();

	private ConfigReader() {}

	public static ConfigReader getInstance() {
		return instance;
	}

	/**
	 * 構成ファイルの情報をSelectionInfoオブジェクトとして読み出します。
	 * @return 構成ファイルの情報を持つオブジェクトです。
	 */
	@Override
	public SelectionInfo read(IFile file) throws XMLStreamException, CoreException,
	SAXException, IOException, ParserConfigurationException {
		// 検証
		SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Source schemaSource = new StreamSource(getClass().getResourceAsStream("Config.xsd"));
		Schema schema = factory.newSchema(schemaSource);
		Validator validator = schema.newValidator();
		Source source = new StreamSource(file.getContents());
		validator.validate(source);
		// 読み出し
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(file.getContents());
		Element selectionElement = (Element)document.getElementsByTagName(SelectionInfo.TAG).item(0);
		if (selectionElement == null) {
			return null;
		} else {
			return readSelectionElement(selectionElement);
		}
	}

	private SelectionInfo readSelectionElement(Element element) {
		ArrayList<InterfaceInfo> infos = new ArrayList<>();
		for (Node childElement : Utility.iterate(
				element.getElementsByTagName(InterfaceInfo.TAG))) {
			infos.add(readInterfaceElement((Element)childElement));
		}
		return new SelectionInfo(infos);
	}

	private InterfaceInfo readInterfaceElement(Element element) {
		ArrayList<AbstractUncertaintyInfo> infos = new ArrayList<>();
		for (Node childElement : Utility.iterate(
				element.getElementsByTagName("*"))) {
			String name = childElement.getNodeName();
			if (name.equals(OptionalInfo.TAG)) {
				infos.add(readOptionalElement((Element)childElement));
			} else if (name.equals(AlternativeInfo.TAG)) {
				infos.add(readAlternativeElement((Element)childElement));
			}
		}
		return new InterfaceInfo(infos, element.getAttribute(InterfaceInfo.ATTR_NAME));
	}

	private OptionalInfo readOptionalElement(Element element) {
		String selection = element.getAttribute(OptionalInfo.ATTR_SEL);
		MethodInfo info = readMethodElement((Element)element.getElementsByTagName(MethodInfo.TAG).item(0));
		if (selection.isEmpty()) {
			return new OptionalInfo(info);
		} else {
			return new OptionalInfo(info, Utility.parseBoolean(selection));
		}
	}

	private AlternativeInfo readAlternativeElement(Element element) {
		String selection = element.getAttribute(AlternativeInfo.ATTR_SEL);
		ArrayList<MethodInfo> infos = new ArrayList<>();
		for (Node childElement : Utility.iterate(
				element.getElementsByTagName(MethodInfo.TAG))) {
			infos.add(readMethodElement((Element)childElement));
		}
		if (selection.isEmpty()) {
			return new AlternativeInfo(infos);
		} else {
			// 1-base -> 0-base
			return new AlternativeInfo(infos, Integer.parseInt(selection) - 1);
		}
	}

	private MethodInfo readMethodElement(Element element) {
		ArrayList<ParameterInfo> infos = new ArrayList<>();
		for (Node childElement : Utility.iterate(
				element.getElementsByTagName(ParameterInfo.TAG))) {
			infos.add(readParameterElement((Element)childElement));
		}
		return new MethodInfo(infos,
				element.getAttribute(MethodInfo.ATTR_TYPE),
				element.getAttribute(MethodInfo.ATTR_NAME));
	}

	private ParameterInfo readParameterElement(Element element) {
		return new ParameterInfo(
				element.getAttribute(ParameterInfo.ATTR_TYPE),
				element.getAttribute(ParameterInfo.ATTR_NAME));
	}

}
