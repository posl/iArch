package jp.ac.kyushu.iarch.checkplugin.testsupport;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public interface IXMLGeneratable {

	public void generateXML(XMLStreamWriter writer) throws XMLStreamException;

}
