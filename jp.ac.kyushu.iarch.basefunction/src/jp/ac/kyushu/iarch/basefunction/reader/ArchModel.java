package jp.ac.kyushu.iarch.basefunction.reader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import jp.ac.kyushu.iarch.archdsl.ArchDSLPlugin;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.ui.internal.ArchDSLActivator;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.resource.XtextResourceSetProvider;

import com.google.inject.Injector;

/**
 * A class for reading arch-code AST.
 * 
 * @author Templar
 *
 */
public class ArchModel {
	protected Resource resource = null;
	final private static Injector injector = ArchDSLActivator.getInstance()
			.getInjector(ArchDSLPlugin.getLanguageName());
	public ArchModel(IResource archfile){
		readResoure(archfile);
	}
	public Model getModel(){
		return (Model) resource.getContents().get(0);
	}

	private void readResoure(IResource archfile) {
		XtextResourceSet rs = (XtextResourceSet) injector.getInstance(
				XtextResourceSetProvider.class).get(archfile.getProject());
		rs.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);

		resource = rs.getResource(URI.createPlatformResourceURI(
				archfile.getFullPath().toString(), true), true);
	}

	// Null output stream.
	private static OutputStream nullOutputStream = new OutputStream() {
		@Override
		public void write(int arg0) throws IOException {
		}
	};
	public void save() throws IOException {
		Map<Object, Object> options = null;
		if (resource != null) {
			// To avoid file truncation on error, performs output to null stream first.
			resource.save(nullOutputStream, options);
			// If succeeded, you can save the model safely.
			resource.save(options);
		}
	}
}
