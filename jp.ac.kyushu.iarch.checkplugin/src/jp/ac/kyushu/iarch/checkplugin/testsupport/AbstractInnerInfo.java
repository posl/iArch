package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public abstract class AbstractInnerInfo<C extends IHasParentInfo> implements IHasChildrenInfo<C> {

	private final List<C> children;

	public AbstractInnerInfo(List<C> children) {
		super();
		this.children = Collections.unmodifiableList(children);
		for (C child : children) {
			child.setParent(this);
		}
	}

	@Override
	public List<C> getChildren() {
		return children;
	}

	@Override
	public void generateAspect(StringWriter writer) {
		for (C child: getChildren()) {
			child.generateAspect(writer);
		}
	}

	@Override
	public void generateXML(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(getXMLTag());
		for (List<String> pair : getXMLAttrIter()) {
			writer.writeAttribute(pair.get(0), pair.get(1));
		}
		for (C child : getChildren()) {
			child.generateXML(writer);
		}
		writer.writeEndElement();
	}

	abstract protected String getXMLTag();

	abstract protected List<List<String>> getXMLAttrIter();

}
