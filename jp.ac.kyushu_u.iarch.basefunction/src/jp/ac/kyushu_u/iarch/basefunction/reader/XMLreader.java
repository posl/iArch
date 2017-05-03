package jp.ac.kyushu_u.iarch.basefunction.reader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Read an arch configuration file in a Java project
 * @author Templar
 */
public class XMLreader {
	private String ArchfilePath = null;
	private String ClassDiagramPath = null;
	private List<String> SequenceDiagramPathes= new ArrayList<String>();
	private List<String> SourceCodePathes = new ArrayList<String>();
	private String ARXMLPath = null;
	private IJavaProject JavaProject = null;
	
	public static final String CONFIG_FILEPATH = "Config.xml";


	public XMLreader(IProject project){
		readXMLContent(project);
		setJavaProject(JavaCore.create(project));
	}

	public static boolean isConfigFileExist(IProject project){
		return project.getFile(XMLreader.CONFIG_FILEPATH).exists();
	}
	
	public static IFile getConfigFile(IProject project){
		return project.getFile(XMLreader.CONFIG_FILEPATH);
	}

	public static IResource readIResource(IPath path){
		IResource re = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		return re;
	}

	public void readXMLContent(IProject project) {
		IFile file = project.getFile(CONFIG_FILEPATH);
		if (!file.exists()){
			try {
				IMarker marker = project.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.MESSAGE, "Auto-Check failed: Please check the Archface Configration.(Menu->iArch->Configration)");
				marker.setAttribute(IMarker.DONE, false);
				marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try{
				SAXReader saxReader = new SAXReader();
				Document document = saxReader.read(file.getContents());
				{
					@SuppressWarnings("unchecked")
					List<Node> Archfilelist = document.selectNodes("//Archfile/Path/@Attribute");
					Attribute attribute=(Attribute) Archfilelist.get(0);
					setArchfilePath(attribute.getValue());
				}

				{
					@SuppressWarnings("unchecked")
					List<Node> ClassDiagramlist = document.selectNodes("//ClassDiagram/Path/@Attribute");
					if(ClassDiagramlist.size()!=0){
						Attribute attribute=(Attribute) ClassDiagramlist.get(0);
						setClassDiagramPath(attribute.getValue());
					}
				}

				{
					@SuppressWarnings("unchecked")
					List<Node> SequenceDiagramlist = document.selectNodes("//SequenceDiagram/Path/@Attribute");
					if(SequenceDiagramlist.size()!=0){
						for (Iterator<Node> iter = SequenceDiagramlist.iterator(); iter.hasNext(); ) {
							Attribute attribute = (Attribute) iter.next();
							String url = attribute.getValue();
							SequenceDiagramPathes.add(url);
						}
					}
				}

				{
					@SuppressWarnings("unchecked")
					List<Node> SourceCodelist = document.selectNodes("//SourceCode/Path/@Attribute");
					for (Iterator<Node> iter = SourceCodelist.iterator(); iter.hasNext(); ) {
						Attribute attribute = (Attribute) iter.next();
						String url = attribute.getValue();
						SourceCodePathes.add(url);
						}
				}

				{
					@SuppressWarnings("unchecked")
					List<Node> ARXMLlist = document.selectNodes("//ARXML/Path/@Attribute");
					Attribute attribute=(Attribute) ARXMLlist.get(0);
					setARXMLPath(attribute.getValue());
				}

			}
			catch(DocumentException e){
				System.out.println(e.getMessage());
			}
			catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return the aRXMLResource
	 */
	public IResource getARXMLResource() {
		if (ARXMLPath == null) { return null; }
		IPath path = new Path(ARXMLPath);
		IResource ARXMLResource = readIResource(path);
		return ARXMLResource;
	}

	/**
	 * @param aRXMLPath the aRXMLPath to set
	 */
	public void setARXMLPath(String aRXMLPath) {
		ARXMLPath = aRXMLPath;
	}

	/**
	 * @return the classDiagramResource
	 */
	public IResource getClassDiagramResource() {
		if (ClassDiagramPath == null) { return null; }
		IPath path = new Path(ClassDiagramPath);
		IResource ClassDiagramResource = readIResource(path);
		return ClassDiagramResource;
	}

	/**
	 * @param classDiagramPath the classDiagramPath to set
	 */
	public void setClassDiagramPath(String classDiagramPath) {
		ClassDiagramPath = classDiagramPath;
	}

	/**
	 * @return the archfileResource
	 */
	public IResource getArchfileResource() {
		if (ArchfilePath == null) { return null; }
		IPath path = new Path(ArchfilePath);
		IResource Archfile = readIResource(path);
		return Archfile;
	}

	/**
	 * @param archfilePath the archfilePath to set
	 */
	public void setArchfilePath(String archfilePath) {
		ArchfilePath = archfilePath;
	}

	/**
	 * @return the SequenceDiagramResources
	 */
	public List<IResource> getSequenceDiagramResource(){
		List<IResource> SequenceDiagramResources = new ArrayList<IResource>();
		for (String SequenceDiagramPath : SequenceDiagramPathes) {
			IPath path = new Path(SequenceDiagramPath);
			IResource SequenceDiagramResource = readIResource(path);
			SequenceDiagramResources.add(SequenceDiagramResource);
		}
		return SequenceDiagramResources;
	}

	/**
	 * @return the SourceCodeResources
	 */
	public List<IResource> getSourceCodeResource(){
		List<IResource> SourceCodeResources = new ArrayList<IResource>();
		for (String SourceCodePath : SourceCodePathes) {
			IPath path = new Path(SourceCodePath);
			IResource SourceCodeResource = readIResource(path);
			SourceCodeResources.add(SourceCodeResource);
		}
		return SourceCodeResources;
	}

	/**
	 * @return the javaProject
	 */
	public IJavaProject getJavaProject() {
		return JavaProject;
	}

	/**
	 * @param javaProject the javaProject to set
	 */
	private void setJavaProject(IJavaProject javaProject) {
		JavaProject = javaProject;
	}
}
