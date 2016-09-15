package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.checkplugin.Activator;

/**
 * Aspect生成の処理の起点です。
 * @author watanabeke
 *
 */
public class AspectGenerator {
	
	private static final String PACKAGE_NAME = "jp.ac.kyushu.iarch.lang";
	private static final String ASPECTGEN_PACKAGE_NAME = String.format("%s.aspectgen", PACKAGE_NAME);
	private static final String ASPECT_CLASS_NAME_PREFIX = "IarchAspect";
	private static final String ABSTRACT_CLASS_NAME = "IarchAbstractAspect";
	private static final String SUPPRESS_ANNOTATION_NAME = "SuppressArchfaceWarnings";
	
	private Configuration configuration;
	
	private void initConfiguration() throws IOException, TemplateModelException {
		configuration = new Configuration(Configuration.VERSION_2_3_25);
		org.osgi.framework.Bundle bundle = Activator.getDefault().getBundle();
		java.net.URL url = bundle.getEntry("/templates");
		java.net.URL fileUrl = FileLocator.toFileURL(url);
		String templateDirPath = fileUrl.getPath();
		java.io.File templateDir = new java.io.File(templateDirPath);
		configuration.setDirectoryForTemplateLoading(templateDir);
		configuration.setDefaultEncoding("UTF-8");
		
		// 定数の設定
		configuration.setSharedVariable("PACKAGE_NAME", PACKAGE_NAME);
		configuration.setSharedVariable("ASPECTGEN_PACKAGE_NAME", ASPECTGEN_PACKAGE_NAME);
		configuration.setSharedVariable("ASPECT_CLASS_NAME_PREFIX", ASPECT_CLASS_NAME_PREFIX);
		configuration.setSharedVariable("ABSTRACT_CLASS_NAME", ABSTRACT_CLASS_NAME);
		configuration.setSharedVariable("SUPPRESS_ANNOTATION_NAME", SUPPRESS_ANNOTATION_NAME);
	}
	
	private static IPackageFragmentRoot loadSrcRoot(IProject project) throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null) {
			throw new IllegalArgumentException();
		}

		// プロジェクト中のソースのルートを特定
		IPackageFragmentRoot srcRoot = null;
		IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
		for (IPackageFragmentRoot root : roots) {
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				srcRoot = root;
				break;
			}
		}
		return srcRoot;
	}
	
	private static List<UncertaintyBean> loadUncertaintyBeans(IProject project) {
		XMLreader xmlreader = new XMLreader(project);
		IResource archfile = xmlreader.getArchfileResource();
		Model model = new ArchModel(archfile).getModel();
		return new ArchReader().generateWeightedUncertaintyBean(model);
	}
	
	/**
	 * アスペクトと、それを保持するプロジェクトを作成します。
	 * @throws JavaModelException
	 * @throws CoreException
	 * @throws IOException 
	 * @throws TemplateException 
	 */
	public void generateAspectCode() throws JavaModelException, CoreException, IOException, TemplateException {
		// 準備
		initConfiguration();
		IProject project;
		try {
			project = ProjectReader.getProject();
		} catch (ProjectNotFoundException e) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Information", "Please open a file in a project.");
			return;
		}
		IPackageFragmentRoot srcRoot = loadSrcRoot(project);
		List<UncertaintyBean> uncertaintyBeans = loadUncertaintyBeans(project);
		
		// デフォルトパッケージを含むか？
		boolean containsDefaultPackage = false;
		isDefaultPackageLoop : for (UncertaintyBean uncertaintyBean : uncertaintyBeans) {
			for (IMethodBean iMethodBean : uncertaintyBean.getMethods()) {
				if (iMethodBean instanceof MethodBean && ((MethodBean) iMethodBean).getPackageName().isEmpty()) {
					boolean confirm = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Confirm", 
							"You have default package. Do you want to generate aspects in default package to apply aspects properly?");
					containsDefaultPackage = confirm;
					break isDefaultPackageLoop;
				}
			}
		}
		configuration.setSharedVariable("containsDefaultPackage", containsDefaultPackage);
		
		// --- 固定ファイルの生成 ---
		IPackageFragment packageFragment = srcRoot.createPackageFragment(
				PACKAGE_NAME, true, new NullProgressMonitor());
		
		try {
			createClassFile(
					packageFragment,
					String.format("%s.java", ABSTRACT_CLASS_NAME),
					configuration.getTemplate("abstract.ftl"));
		} catch (JavaModelException e) {
			// do nothing
		}
		try {
			createClassFile(
					packageFragment,
					String.format("%s.java", SUPPRESS_ANNOTATION_NAME),
					configuration.getTemplate("annotation.ftl"));
		} catch (JavaModelException e) {
			// do nothing
		}

		// --- アスペクトの生成 ---
		IPackageFragment aspectgenPackageFragment = containsDefaultPackage
				? srcRoot.createPackageFragment("", true, new NullProgressMonitor())
				: srcRoot.createPackageFragment(ASPECTGEN_PACKAGE_NAME, true, new NullProgressMonitor());

		// 前回のアスペクトの削除
		removeAspectCode();
		
		// 各不確かさに対するアスペクトの生成
		Template aspectTemplate = configuration.getTemplate("aspect.ftl");
		for (UncertaintyBean uncertaintyBean : uncertaintyBeans) {
			try {
				createClassFile(
						aspectgenPackageFragment,
						String.format("%s%s.java", ASPECT_CLASS_NAME_PREFIX, uncertaintyBean.getLabel()),
						aspectTemplate,
						uncertaintyBean);
			} catch (JavaModelException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	private static void createClassFile(IPackageFragment packageFragment, String name, Template template) throws JavaModelException {
		createClassFile(packageFragment, name, template, null);
	}
	
	private static void createClassFile(IPackageFragment packageFragment, String name, Template template, Object dataModel) throws JavaModelException {
		Writer writer = new StringWriter();
		try {
			template.process(dataModel, writer);
			writer.flush();
			packageFragment.createCompilationUnit(name, writer.toString(), false, new NullProgressMonitor());
			writer.close();
		} catch (TemplateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void removeAspectCode() throws JavaModelException {
		IProject project;
		try {
			project = ProjectReader.getProject();
		} catch (ProjectNotFoundException e) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Information", "Please open a file in a project.");
			return;
		}
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null) {
			throw new IllegalArgumentException();
		}
		for (IPackageFragment packageFragment : javaProject.getPackageFragments()) {
			for (IJavaElement javaElement : packageFragment.getChildren()) {
				if (javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
					ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
					for (IJavaElement javaElement2 : compilationUnit.getChildren()) {
						if (javaElement2.getElementType() == IJavaElement.TYPE) {
							IType type = (IType) javaElement2;
							String superClassName = type.getSuperclassName();
							if (superClassName != null && superClassName.equals(ABSTRACT_CLASS_NAME)) {
								compilationUnit.delete(true, new NullProgressMonitor());
							}
						}
					}
				}
			}
		}
	}

}
