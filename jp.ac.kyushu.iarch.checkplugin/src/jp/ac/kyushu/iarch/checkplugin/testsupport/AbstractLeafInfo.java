package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.StringWriter;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public abstract class AbstractLeafInfo<P> implements IHasParentInfo<P> {

	private P parent;

	public AbstractLeafInfo() {
		super();
	}

	@Override
	public P getParent() {
		return parent;
	}

	@Override
	public void setParent(P parent) {
		this.parent = parent;
	}

	@Override
	public void generateAspect(StringWriter writer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void generateXML(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(getXMLTag());
		for (List<String> pair : getXMLAttrIter()) {
			writer.writeAttribute(pair.get(0), pair.get(1));
		}
		writer.writeEndElement();
	}

	abstract protected String getXMLTag();

	abstract protected List<List<String>> getXMLAttrIter();

}
