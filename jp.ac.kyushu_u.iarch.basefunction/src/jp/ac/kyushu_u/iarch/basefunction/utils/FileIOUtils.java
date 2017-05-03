package jp.ac.kyushu_u.iarch.basefunction.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Output to file in project with standard Eclipse method.
 * @author watanabeke
 */
public class FileIOUtils {

	private FileIOUtils() {}
	
	public static void writeFile(IFile file, InputStream inputStream) throws CoreException {
		if (file.exists()) {
			file.setContents(inputStream, true, false, null);
		} else {
			file.create(inputStream, true, null);
		}
	}
	
	public static void xmlWriteFile(IFile file, Document document, OutputFormat format) throws CoreException, IOException {
		StringWriter writer = new StringWriter();
		XMLWriter xmlWriter = new XMLWriter(writer, format);
		xmlWriter.write(document);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(writer.toString().getBytes("utf-8"));
		writeFile(file, inputStream);
		inputStream.close();
		xmlWriter.close();
		writer.close();
	}
	
	public static void xmlWriteFile(IFile file, Document document) throws CoreException, IOException {
		xmlWriteFile(file, document, OutputFormat.createPrettyPrint());
	}

}
