package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.SAXException;

public final class ResourceUtility {

	private ResourceUtility() {}

	/**
	 * archfaceから構成ファイルを生成します。
	 * 既に存在する場合は上書きします。
	 * @throws CoreException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public static void generateConfig(IFile archFile, IFile configFile)
			throws CoreException, XMLStreamException, IOException {
		SelectionInfo info = ArchfaceReader.getInstance().read(archFile);
		String configCode = info.generateXML();
		InputStream configStream = new ByteArrayInputStream(configCode.getBytes());
		if (configFile.exists()) {
			configFile.setContents(configStream, false, false, null);
		} else {
			configFile.create(configStream, false, null);
		}
	}

	public static SelectionInfo readConfig(IFile configFile) throws
	XMLStreamException, CoreException, SAXException, IOException, ParserConfigurationException {
		return ConfigReader.getInstance().read(configFile);
	}

	public static void writeConfig(SelectionInfo info, IFile configFile)
			throws CoreException, XMLStreamException, IOException {
		String configCode = info.generateXML();
		InputStream configStream = new ByteArrayInputStream(configCode.getBytes());
		if (configFile.exists()) {
			configFile.setContents(configStream, false, false, null);
		} else {
			configFile.create(configStream, false, null);
		}
	}

	/**
	 * configファイルをコピーします。
	 * @throws CoreException
	 */
	public static void copyConfig(IFile origConfigFile, IFile destConfigFile) throws CoreException {
		if (destConfigFile.exists()) {
			destConfigFile.setContents(origConfigFile.getContents(), false, false, null);
		} else {
			destConfigFile.create(origConfigFile.getContents(), false, null);
		}
	}

	/**
	 * archfaceの内容に構成ファイルを適合させます。
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws CoreException
	 * @throws XMLStreamException
	 */
	public static void adjustConfig(IFile archFile, IFile configFile) throws
	XMLStreamException, CoreException, SAXException, IOException, ParserConfigurationException {
		SelectionInfo newInfo = ArchfaceReader.getInstance().read(archFile);
		SelectionInfo oldInfo = ConfigReader.getInstance().read(configFile);
		newInfo.update(oldInfo);  // マージ
		String configCode = newInfo.generateXML();
		InputStream configStream = new ByteArrayInputStream(configCode.getBytes());
		if (configFile.exists()) {
			configFile.setContents(configStream, false, false, null);
		} else {
			configFile.create(configStream, false, null);
		}
	}

	/**
	 * 構成ファイルからAspectを生成します。
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws CoreException
	 * @throws XMLStreamException
	 */
	public static void generateAspect(IFile configFile, IFile aspectFile) throws
	XMLStreamException, CoreException, SAXException, IOException, ParserConfigurationException {
		SelectionInfo info = ConfigReader.getInstance().read(configFile);
		String aspectCode = info.generateAspect(ResourceFetcher.ASPECT_NAME);
		InputStream aspectStream = new ByteArrayInputStream(aspectCode.getBytes());
		if (aspectFile.exists()) {
			aspectFile.setContents(aspectStream, false, false, null);
		} else {
			aspectFile.create(aspectStream, false, null);
		}
	}

}
