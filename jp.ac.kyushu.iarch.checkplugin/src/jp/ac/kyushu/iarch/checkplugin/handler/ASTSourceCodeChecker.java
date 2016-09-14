package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.basefunction.utils.ProblemViewManager;
import jp.ac.kyushu.iarch.checkplugin.model.AltMethodPairsContainer;
import jp.ac.kyushu.iarch.checkplugin.model.BehaviorPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.CallPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentMethodPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.UncertainBehaviorContainer;
import jp.ac.kyushu.iarch.checkplugin.utils.CodeXMLUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.GitUtils;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public class ASTSourceCodeChecker{
	private ArrayList<ComponentClassPairModel> componentClassPairModels = new ArrayList<ComponentClassPairModel>();
	private ArrayList<BehaviorPairModel> behaviorPairModels = new ArrayList<BehaviorPairModel>();
	private ArrayList<UncertainBehaviorContainer> uncertainBehaviorContainers = new ArrayList<UncertainBehaviorContainer>();
	private GitController gitController;
	public static boolean isEnableGit;

	public static String InsertPath;
	public static String InsertMethod;
	public static String InsertJavaCode;
	public static IJavaProject project;

	public void SourceCodeArchifileChecker(Model archface,	IJavaProject javaProject){
		ASTSourceCodeChecker.project = javaProject;
		IProject project = javaProject.getProject();
		Document codeXmlDocument = DocumentHelper.createDocument();
		ASTSourceCodeChecker.isEnableGit = GitUtils.isEnableGitRepository(project);

		// read javafiles and write its info on codeXML.xml
		try {
			CodeXMLUtils.generateProjectXML(javaProject, codeXmlDocument);

			try {
				OutputFormat format = OutputFormat.createPrettyPrint();
				String projectPath = project.getLocation().toOSString();
				XMLWriter output = new XMLWriter(new FileWriter(new File(projectPath + "/codeXML.xml")), format);
				codeXmlDocument.setXMLEncoding("utf-8");
				output.write(codeXmlDocument);
				output.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}

		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Element root = codeXmlDocument.getRootElement();
		Element packageElement = (Element) root.selectSingleNode("//Package[@name='']");

		// pairModels Clear.
		pairModelsInit();

		// Interface check
		@SuppressWarnings("unchecked")
		List<Element> packageElements = root.selectNodes("Package");
		typeCheckInterface(archface,packageElements);

		// Warn unwrite methods
		warnInterface(archface, packageElements, project);

		// behaver
		typeCheckBehavior(archface);

		// Uncertain Behavior
		typeCheckUncertainBehavior(archface);

		// Old Behavior Check (must delete in the future)
		for (Behavior behavior: archface.getBehaviors()) {
			String interfaceName = behavior.getInterface().getName();
			//First LastClass is defined first Method's Class
			String prevClassName = ((Interface) behavior.getCall().get(0).eContainer()).getName();
			String prevMethodName = null;
			String infoString = null;
			for (Method method: behavior.getCall()) {
				String className = ((Interface) method.eContainer()).getName();
				String methodName = method.getName();
				if (null == methodName) continue; //add by AiDi

				Node classNode = null;
				if (prevClassName != null) {
					classNode = packageElement.selectSingleNode("Class[@name='" + prevClassName + "']");
				}
				if (classNode == null) continue; //add by AiDi

				boolean invocationFoundFlag = false;
				int lineNumber = 0;
				if (prevMethodName != null) {
					Node methodNode = classNode.selectSingleNode("MethodDeclaration[@name='" + prevMethodName + "']");
					lineNumber = Integer.parseInt(((Element) methodNode).attributeValue("lineNumber").toString());
					invocationFoundFlag = (methodNode.selectSingleNode("MethodInvocation[@name='" + methodName + "']") != null);
					if (!invocationFoundFlag) {
						IResource prevClassResource = project.getFile("/src/" + prevClassName + ".java");
						String message = "Behavior  : " + interfaceName + " :  "
								+ prevClassName + "." + prevMethodName + " : "
								+ methodName + " " + "is not defined";
						ProblemViewManager.addError1(prevClassResource, message,
								prevClassResource.getLocationURI().getPath() ,lineNumber);
					}
				}

				if (invocationFoundFlag) {
					infoString = "Behavior  : " + interfaceName + " : "
							+ prevClassName + "." + prevMethodName + " ->" + className + "." + methodName;
				}
				if (infoString != null) {
					IResource prevClassResource = project.getFile("/src/" + prevClassName + ".java");
					ProblemViewManager.addInfo1(prevClassResource, infoString, prevClassName, lineNumber);
				}

				prevClassName = className;
				prevMethodName = methodName;
			}
		}

		// Git Control
		if(ASTSourceCodeChecker.isEnableGit){
			this.gitController = new GitController(project,
					this.componentClassPairModels);
			gitController.checkGitInfo();
		}
		outputErrorMessages(project);
	}

	/**
	 * エラーメッセージ出力
	 * TODO 最終的に処理を書くTypeCheckHogeHogeに分散させる．
	 * @param resource IResource型 リソース
	 */
	private void outputErrorMessages(IProject project) {
		// tmp compile error output
		for (ComponentClassPairModel pairModel: componentClassPairModels) {
			String classPath = null;
			if (pairModel.getPackageNode().attributeValue("name").equals("")) { // Is it appropriate?
				classPath = "src/" + pairModel.getName() + ".java";
			}else{
				classPath = "src/" + pairModel.getPackageNode().attributeValue("name") + "/" + pairModel.getName() + ".java";
			}
			IResource classResource = project.getFile(new Path(classPath));
			String classResourcePath = classResource.getLocationURI().getPath();

			if (pairModel.hasJavaNode()) {
				int lineNumber = Integer.parseInt(((Element) pairModel.getJavaClassNode()).attributeValue("lineNumber"));
//				ProblemViewManager.addInfo1(classResource,
//						"Component :" + pairModel.getName() + " is defined",
//						pairModel.getName(), lineNumber);
				for (ComponentMethodPairModel methodModel: pairModel.methodPairsList) {
					if (!(methodModel instanceof AltMethodPairsContainer)) {
						if (methodModel.hasJavaNode()) {
							Node methodNode = methodModel.getJavaMethodNode();
							int lineNumberMethod = Integer.parseInt(((Element) methodNode).attributeValue("lineNumber"));
							// Mark for OptMethod
							if (methodModel.isOpt()) {
								ProblemViewManager.addOptionalInfo(classResource,
										"OptComponent :" + methodModel.getName() + " is defined",
										classResourcePath, lineNumberMethod);
							} else {
//								ProblemViewManager.addInfo1(classResource,
//										"Component :" + methodModel.getName() + " is defined",
//										pairModel.getName(), lineNumber);
							}
						} else {
							if (methodModel.isOpt()) {
								ProblemViewManager.addWarning1(classResource,
										"OptComponent :" +  methodModel.getName() + " is not defined",
										classResourcePath, lineNumber);
							} else {
								ProblemViewManager.addError1(classResource,
										"Interface- " +  methodModel.getName() + " is not defined",
										classResourcePath, lineNumber);
							}
						}
					} else {
						boolean altMethodNotFoundFlag = true;
						for (ComponentMethodPairModel altMethod: ((AltMethodPairsContainer) methodModel).getAltMethodPairs()) {
							if (altMethod.hasJavaNode()) {
								ProblemViewManager.addInfo1(classResource,
										"AltComponent :" + altMethod.getName() + " is defined",
										pairModel.getName(), lineNumber);
								altMethodNotFoundFlag = false; //AltMethodはひとつでも確立ができればOK
							}
						}
						if (altMethodNotFoundFlag) {
							ProblemViewManager.addError1(classResource,
									"AltComponent : " + methodModel.getName() + " is not defined",
									classResourcePath, lineNumber);
						}
					}
				}
			} else {
				if (!pairModel.methodPairsList.isEmpty()) {
					ProblemViewManager.addError1(classResource,
							"Interface- " + pairModel.getArchInterface().getName() + " is not defined",
							classResourcePath, 0);
				} else {
					ProblemViewManager.addWarning1(classResource,
							"Component :" + pairModel.getArchInterface().getName() + " is not defined",
							classResourcePath, 0);
				}
			}
		}

		for (BehaviorPairModel pairModel: behaviorPairModels) {
			List<CallPairModel> callModelList = pairModel.getCallModels();
			//ループを行う回数はコールの数 - 1
			for (int i = 0; i < callModelList.size() - 1; i++) {
				CallPairModel currentCallModel = callModelList.get(i);
				CallPairModel nextCallModel = callModelList.get(i + 1);

				IResource currentResource =
						currentCallModel.getMethodModel().getParentModel().getClassPath(project);
				String currentResourcePath = currentResource.getLocationURI().getPath();
				int lineNumber = Integer.parseInt(((Element) currentCallModel.getMethodModel().getJavaMethodNode()).attributeValue("lineNumber"));

				if (currentCallModel.getMethodModel().hasInvocation(nextCallModel.getName())) {
					ProblemViewManager.addInfo1(currentResource,
							"Behavior - " + currentCallModel.getName() + " -> " + nextCallModel.getName() + " is defined",
							currentResourcePath, lineNumber);
				} else {
					ProblemViewManager.addError1(currentResource,
							"Behavior - " + currentCallModel.getName() + " -> " + nextCallModel.getName() + " is not defined",
							currentResourcePath, lineNumber);
				}
			}
		}

		for(UncertainBehaviorContainer container : uncertainBehaviorContainers){
			IResource callResource = null;
			if(container.getCompileSuccessedBehaviors().size() == 1){

			}else if(container.getCompileSuccessedBehaviors().size() == 0){
				for(ArrayList<CallPairModel> errorCallPair : container.getErrorCallSet()){
					callResource = errorCallPair.get(0).getMethodModel().getParentModel().getClassPath(project.getProject());
					ProblemViewManager.addError1(callResource, "UncertainBehavior - " + errorCallPair.get(0).getName() + " -> " + errorCallPair.get(1).getName() + " is not defined.",
							callResource.getLocationURI().getPath(), errorCallPair.get(0).getMethodModel().getLineNumber());

				}
			}else{
				ProblemViewManager.addError(container.getCompileSuccessedBehaviors().get(0).getCallModels().get(0).getMethodModel().getParentModel().getClassPath(project.getProject()),
						"UncertainBehavior - " + container.getName() + " is dupulicated flow in implemetation.", null);
			}
		}
	}

	/**
	 * Behaviorに記述されたメソッドの該当したメソッドの組み合わせのComponentMethodPairModelをコール順にListへ格納します．
	 * Behaviorの成立可否はComponentMethodPairModelのisInvocationExistメソッドによって判定します．
	 * @param archface Archface全情報が入っているモデル
	 */
	private void typeCheckBehavior(Model archiface) {
		for (jp.ac.kyushu.iarch.archdsl.archDSL.Connector connector : archiface.getConnectors()) {
			for (Behavior behavior : connector.getBehaviors()) {
				ArrayList<CallPairModel> callPairModels = new ArrayList<CallPairModel>();
				for(Method methodCall : behavior.getCall()){
					callPairModels.add(new CallPairModel(componentClassPairModels, methodCall));
				}
				behaviorPairModels.add(new BehaviorPairModel(behavior.getInterface().getName(), callPairModels));
			}
		}
	}

	/**
	 * Archfaceにおける不確かなBehaviorからCallを抜き出し，それとセットのmethodPairを作成し，それをリストとしてまとめる
	 * 細かい処理は非常に煩雑なため，モデルのコンストラクタに任せる．
	 * @param archiface Archface全情報が入っているモデル
	 */
	private void typeCheckUncertainBehavior(Model archiface){
		for(UncertainConnector u_connector : archiface.getU_connectors()){
			ArrayList<UncertainBehaviorContainer> uncertainBehaviors = new ArrayList<UncertainBehaviorContainer>();
			for(UncertainBehavior u_behavior : u_connector.getU_behaviors()){
				ArrayList<CallPairModel> callPairModels = new ArrayList<CallPairModel>();
				for(SuperCall methodCall : u_behavior.getCall()){
					callPairModels.add(new CallPairModel(componentClassPairModels, (SuperCall) methodCall));
				}
				uncertainBehaviorContainers.add(new UncertainBehaviorContainer(new BehaviorPairModel(u_behavior.getName(), callPairModels)));
			}
		}
	}

	 /**
	 * ソースコード上に存在するが、Archfaceに記述されていないメソッドを探し、警告を作成する
	 */
	private void warnInterface(Model archface, List<Element> packageElements, IProject project){
		for(Element packageElement : packageElements){
			for (Object classObj: packageElement.selectNodes("Class")) {
				Element javacodeClass = (Element) classObj;
				String javacodeClassName = javacodeClass.attributeValue("name");
				for(Object methodObj : javacodeClass.selectNodes("MethodDeclaration")){
					Element javacodeMethod = (Element) methodObj;
					String javacodeMethodName = javacodeMethod.attributeValue("name");
					int lineNumberMethod = Integer.parseInt(javacodeMethod.attributeValue("lineNumber"));
					boolean hasMethod = false;
					for(Interface archClass : archface.getInterfaces()){
						String archClassName = archClass.getName();
						if(archClassName.equals(javacodeClassName)){
							for(Method archMethod : archClass.getMethods()){
								if(archMethod.getName().equals(javacodeMethodName)){
									hasMethod = true;
								}
							}
						}
					}
					// have priority of certain components
					if(!hasMethod){
						for(UncertainInterface uInterface : archface.getU_interfaces()){
							String archClassName = uInterface.getSuperInterface().getName();
							if(archClassName.equals(javacodeClassName)){
								for(OptMethod optMethod : uInterface.getOptmethods()){
									if(optMethod.getMethod().getName().equals(javacodeMethodName)){
										hasMethod = true;
									}
								}
								for(AltMethod altMethod : uInterface.getAltmethods()){
									for(Method method : altMethod.getMethods()){
										if(method.getName().equals(javacodeMethodName)){
											hasMethod = true;
										}
									}
								}
							}
						}
					}
					if (!hasMethod) {
						String classPath = null;
						if (packageElement.attributeValue("name").equals("")) { // Is it appropriate?
							classPath = "src/" + javacodeClassName + ".java";
						}else{
							classPath = "src/" + packageElement.attributeValue("name") + "/" + javacodeClassName + ".java";
						}
						IResource javacodeResource = project.getFile(classPath);
						ProblemViewManager.addWarning1(javacodeResource,
								"JavaCode- Method :" + javacodeMethodName + " is not in the Archface",
								project.getLocationURI().getPath() + File.separator +classPath,
								lineNumberMethod);
					}
				}
			}
		}

	}
	/**
	 * Archfaceと同じクラス，メソッド名をJavaソースコードから探し，ComponentClassPairModelへ格納します．
	 * @param archface Archface全情報が入っているモデル
	 * @param packageElements
	 */
	private void typeCheckInterface(Model archface, List<Element> packageElements) {
		Node classNode = null;

		for (Interface archClass: archface.getInterfaces()) {
			String archClassPath = "Class[@name='" + archClass.getName() + "']";
			for (Element packageElement: packageElements) {
				classNode =  packageElement.selectSingleNode(archClassPath);
				if (classNode != null) {
					break;
				}
			}

			ComponentClassPairModel classPairModel = new ComponentClassPairModel(archClass, classNode);
			if (classNode != null) {
				for (Method archMethod: archClass.getMethods()) {
					String archMethodPath = "MethodDeclaration[@name='" + archMethod.getName() + "']";
					Node methodNode = classNode.selectSingleNode(archMethodPath);
					ComponentMethodPairModel methodPairModel =
							new ComponentMethodPairModel(archMethod, methodNode, classPairModel);
					// Insert Method Model to class Pair Model
					classPairModel.methodPairsList.add(methodPairModel);
				}
			}

			// uncertain check
			typeCheckUncertainInterface(archface.getU_interfaces(), classPairModel);
			// Insert Pair Model to static model
			componentClassPairModels.add(classPairModel);
		}
	}

	/**
	 * 不確かさを包容したComponentに対してのタイプチェックを行い、classPairModelを更新します。
	 * @param uInterfaces 不確かなComponentのリスト
	 * @param classPairModel 更新すべきComponentClassPairModelのリスト
	 */
	private void typeCheckUncertainInterface(
			EList<UncertainInterface> uInterfaces, ComponentClassPairModel classPairModel) {
		if (!classPairModel.hasJavaNode()) {
			return;
		}
		Node classNode = classPairModel.getJavaClassNode();
		String classNodeName = ((Element) classNode).attributeValue("name");

		for (UncertainInterface uInterface: uInterfaces) {
			Interface superInterface = uInterface.getSuperInterface();
			if (superInterface != null) { // if super-interface is not exist, type check should not be done.
				if (classNodeName.equals(superInterface.getName())) { // this means the class defined by super-interface is exist in java code.
					//optmethods check
					for (OptMethod optMethod: uInterface.getOptmethods()) {
						String methodNodePath = "MethodDeclaration[@name='" + optMethod.getMethod().getName() + "']";
						Node methodNode = classNode.selectSingleNode(methodNodePath);
						ComponentMethodPairModel uncertainMethodPairModel =
								new ComponentMethodPairModel(optMethod.getMethod(), methodNode, classPairModel);
						// insert or override OptMethodPairModel
						classPairModel.overrideMethodPairModel(uncertainMethodPairModel);
					}

					//altmethods check
					for (AltMethod altMethod: uInterface.getAltmethods()) {
						//List<String> altMethodNames = new ArrayList<String>();
						List<ComponentMethodPairModel> altMethodPairModels = new ArrayList<ComponentMethodPairModel>();

						for (Method method: altMethod.getMethods()) {
							//altMethodNames.add(method.getName());
							String methodNodePath = "MethodDeclaration[@name='" + method.getName() + "']";
							Node methodNode = classNode.selectSingleNode(methodNodePath);
							ComponentMethodPairModel uncertainMethodPairModel =
									new ComponentMethodPairModel(method, methodNode, classPairModel);
							altMethodPairModels.add(uncertainMethodPairModel);
						}

						AltMethodPairsContainer altMethodPairsContainer =
								new AltMethodPairsContainer(altMethodPairModels, classPairModel);
						// insert or override AltMethodPairModel
						classPairModel.overrideMethodPairModel(altMethodPairsContainer);
					}
				}
			}
		}
	}

	private void pairModelsInit() {
		componentClassPairModels.clear();
		behaviorPairModels.clear();
	}

	public List<ComponentClassPairModel> getComponentClassPairModels() {
		return componentClassPairModels;
	}

	public List<BehaviorPairModel> getBehaviorPairModels() {
		return behaviorPairModels;
	}

	/**
	 * @return uncertainBehaviorContainers
	 */
	public ArrayList<UncertainBehaviorContainer> getUncertainBehaviorContainers() {
		return uncertainBehaviorContainers;
	}


	public void setBehaviorModels(ArrayList<BehaviorPairModel> pairModels){
		behaviorPairModels = pairModels;
	}

	/**
	 * @return gitController
	 */
	public GitController getGitController() {
		return gitController;
	}
}

