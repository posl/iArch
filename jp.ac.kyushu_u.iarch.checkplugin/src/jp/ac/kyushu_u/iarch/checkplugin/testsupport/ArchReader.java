package jp.ac.kyushu_u.iarch.checkplugin.testsupport;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.eclipse.emf.ecore.EObject;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCallChoice;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Annotation;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Param;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu_u.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu_u.iarch.checkplugin.utils.GeneralUtils;

/**
 * Aspect生成時に、中間オブジェクトの生成などを担います。
 * @author watanabeke
 *
 */
public class ArchReader {
	
	private static final String LABEL_ANNOTATION_NAME = "Label";
	private static final String FORCE_ANNOTATION_NAME = "ExecForce";
	private static final String IGNORE_ANNOTATION_NAME = "ExecIgnore";
	private static final String RATIO_ANNOTATION_NAME = "ExecRatio";
	private static final String WEIGHT_ANNOTATION_NAME = "ExecWeight";
	private static final Set<String> EXEC_ANNOTATION_NAMES = new HashSet<>(Arrays.asList(
			FORCE_ANNOTATION_NAME, IGNORE_ANNOTATION_NAME, RATIO_ANNOTATION_NAME, WEIGHT_ANNOTATION_NAME));
	
	private static final Pattern classNamePattern = Pattern.compile("(.*)\\.(\\w*)$");
	
	private Document codeXML = null;
	
	/**
	 * codeXMLを取得し解析します。
	 */
	private void initializeSAX() {
		try {
			SAXReader saxReader = new SAXReader();
			FileInputStream fileInputStream = new FileInputStream(
					ProjectReader.getProject().getLocation().toOSString() + File.separator + "codeXML.xml");
			codeXML = saxReader.read(fileInputStream);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * Archfaceのモデルに対して、Aspect生成用の中間オブジェクトUncertaintyBeanを複数生成します。
	 * @param model
	 * @return
	 */
	protected List<UncertaintyBean> generateUncertaintyBeans(Model model) {
		initializeSAX();
		List<UncertaintyBean> uncertaintyBeans = new LinkedList<>();
		
		// Componentについて
		for (UncertainInterface uInterface : model.getU_interfaces()) {
			for (SuperMethod superMethod : GeneralUtils.joinLists(
					uInterface.getAltmethods(), uInterface.getOptmethods())) {
				UncertaintyBean uncertaintyBean = generateUncertaintyBean(
						superMethod, labelForIndex(uncertaintyBeans.size()));
				uncertaintyBeans.add(uncertaintyBean);
			}
		}
		
		// connectorについて
		for (UncertainConnector uConnector : model.getU_connectors()) {
			for (UncertainBehavior uBehavior : uConnector.getU_behaviors()) {
				List<List<IMethodBean>> methodsHistory = new LinkedList<>();  // connectorの前の要素を保存
				for (SuperCall superCall : uBehavior.getCall()) {
					List<IMethodBean> methods;
					if (superCall instanceof CertainCall) {
						// methodsのみ設定
						methods = new LinkedList<>();
						SuperMethod superMethod = ((CertainCall) superCall).getName();
						methods.add(generateMethodBean((Method) superMethod, null));
					} else {
						// メソッドの設定
						UncertaintyBean uncertaintyBean = generateUncertaintyBean(
								superCall, labelForIndex(uncertaintyBeans.size()));
						methods = uncertaintyBean.getMethods();
						// そのメソッドがどこから呼ばれなければならないかを設定
						List<MethodBean> whereCalled = new LinkedList<>();
						Map<String, String> importClasses = new HashMap<>();
						methodsHistoryLoop: for (List<IMethodBean> prevMethods : methodsHistory) {
							for (IMethodBean prevMethod : prevMethods) {
								if (prevMethod instanceof MethodBean) {
									whereCalled.add((MethodBean) prevMethod);
									importClasses.putAll(loadImports(
											((MethodBean) prevMethod).getPackageName(), 
											((MethodBean) prevMethod).getClassName()));
								}
							}
							for (IMethodBean prevMethod : prevMethods) {
								if (prevMethod instanceof EmptyMethodBean) {
									break methodsHistoryLoop;
								}
							}
						}
						importClasses.putAll(uncertaintyBean.getImportClasses());
						uncertaintyBean.setImportClasses(importClasses);
						uncertaintyBean.setWhereCalled(whereCalled);
						uncertaintyBeans.add(uncertaintyBean);
					}
					methodsHistory.add(0, methods);
				}
			}
		}
		return uncertaintyBeans;
	}
	
	private Map<String, String> loadImports(String packageName, String className) {
		Map<String, String> importClasses = new HashMap<>();
		@SuppressWarnings("unchecked")
		List<Node> importDeclarationNodes = codeXML.selectNodes(String.format(
				"//Package[@name=\"%s\"]/Class[@name=\"%s\"]/ImportDeclaration/@name",
				packageName, className));
		for (Node importDeclarationNode : importDeclarationNodes) {
			Matcher matcher = classNamePattern.matcher(importDeclarationNode.getText());
			if (matcher.find()) {
				importClasses.put(matcher.group(2), matcher.group(1));
			}
		}
		return importClasses;
	}
	
	private UncertaintyBean generateUncertaintyBean(EObject node, String defaultLabel) {
		UncertaintyBean uncertaintyBean = new UncertaintyBean();
		// 準備
		boolean isComp; boolean isOpt;
		if (node instanceof OptMethod) {
			isComp = true; isOpt = true;
		} else if (node instanceof AltMethod) {
			isComp = true; isOpt = false;
		} else if (node instanceof OptCall) {
			isComp = false; isOpt = true;
		} else if (node instanceof AltCall) {
			isComp = false; isOpt = false;
		} else {
			throw new IllegalArgumentException();
		}
		// メソッドの設定
		List<SuperMethod> superMethods = null;
		if (isOpt) {
			superMethods = Arrays.asList(isComp ? ((OptMethod) node).getMethod() : ((OptCall) node).getName());
		} else if (isComp) {
			superMethods = new ArrayList<>();
			superMethods.addAll(((AltMethod) node).getMethods());
		} else {
			superMethods = new ArrayList<>();
			superMethods.add(((AltCall) node).getName().getName());
			for (AltCallChoice choice : ((AltCall) node).getA_name()) {
				superMethods.add(choice.getName());
			}
		}

		List<IMethodBean> methods = new LinkedList<>();
		Map<String, String> importClasses = new HashMap<>();
		for (SuperMethod method : superMethods) {
			MethodBean methodBean = generateMethodBean((Method) method, labelForIndex(methods.size()));
			methods.add(methodBean);
			importClasses.putAll(loadImports(methodBean.getPackageName(), methodBean.getClassName()));
		}
		if (isOpt) {
			methods.add(generateEmptyMethodBean());
		}		
		uncertaintyBean.setMethods(methods);
		uncertaintyBean.setImportClasses(importClasses);
		uncertaintyBean.setWhereCalled(new LinkedList<MethodBean>());  // 空とする
		
		// ラベルを設定
		uncertaintyBean.setLabel(loadLabelOfUncertainty(node, defaultLabel));
		
		return uncertaintyBean;
	}
	
	private static String loadLabelOfUncertainty(EObject node, String defaultLabel) {
		for (Annotation annotation : node instanceof SuperMethod 
				? ((SuperMethod) node).getAnnotations() 
				: ((SuperCall) node).getAnnotations()) {
			if (annotation.getName().equals(LABEL_ANNOTATION_NAME)) {
				 return stripQuote(annotation.getArgs().get(0));
			}
		}
		return defaultLabel;
	}
	
	private MethodBean generateMethodBean(Method method, String defaultLabel) {
		MethodBean methodBean = new MethodBean();
		String className = ArchModelUtils.getClassName(method);
		methodBean.setClassName(className);
		methodBean.setType(method.getType());
		methodBean.setName(method.getName());
		List<ParamBean> paramBeans = new LinkedList<>();
		for (Param param : method.getParam()) {
			ParamBean paramBean = new ParamBean();
			paramBean.setType(param.getType());
			paramBean.setName(param.getName());
			paramBeans.add(paramBean);
		}
		methodBean.setParams(paramBeans);
		
		// ラベルを設定
		methodBean.setLabel(loadLabelOfMethod(method, defaultLabel));
		
		// プロジェクト内のcodeXMLの情報を元にメソッドがstaticかどうか判定
		boolean isStatic = false;
		// Xpath言語で取得
		isStatic = Boolean.valueOf(codeXML.valueOf(String.format(
				"string(//Class[@name=\"%s\"]/MethodDeclaration[@name=\"%s\"]/MethodModifiers/modifier/text()=\"static\")", 
				className, method.getName())));
		methodBean.setIsStatic(isStatic);
		
		// codeXMLの情報を元にパッケージ名を設定
		String packageName = null;
		packageName = codeXML.valueOf(String.format(
				"string(//Package[Class[@name=\"%s\"][MethodDeclaration[@name=\"%s\"]]]/@name)",
				className, method.getName()));  // デフォルトパッケージならば空になるはず
		methodBean.setPackageName(packageName);
		
		return methodBean;
	}
	
	private static EmptyMethodBean generateEmptyMethodBean() {
		EmptyMethodBean emptyMethodBean = new EmptyMethodBean();
		emptyMethodBean.setLabel("Empty");
		return emptyMethodBean;
	}
	
	private static String loadLabelOfMethod(Method method, String defaultLabel) {
		for (Annotation annotation : method.getAnnotations()) {
			if (annotation.getName().equals(LABEL_ANNOTATION_NAME)) {
				return stripQuote(annotation.getArgs().get(0));
			}
		}
		return defaultLabel;
	}
	
	/**
	 * Archfaceのモデルに対して、重み情報を追加したAspect生成用の中間オブジェクトUncertaintyBeanのリストを生成します。
	 * アノテーションで重みの設定されていない不確かさは、フィルターされ、返しません。
	 * @param model
	 * @return
	 */
	protected List<UncertaintyBean> generateWeightedUncertaintyBean(Model model) {
		List<UncertaintyBean> uncertaintyBeans = generateUncertaintyBeans(model);
		List<UncertaintyBean> weightedUncertaintyBeans = new LinkedList<>();  // 重みありのBeanだけを返す
		for (UncertaintyBean uncertaintyBean : uncertaintyBeans) {
			List<MethodAnnotationPair> methodAnnotationPairs = generateMethodAnnotationPairs(model, uncertaintyBean);
			// まずアノテーションを数える
			Map<String, Integer> annotationCount = new HashMap<>();
			for (MethodAnnotationPair pair : methodAnnotationPairs) {
				if (pair.getAnnotation() != null) {
					String name = pair.getAnnotation().getName();
					assert EXEC_ANNOTATION_NAMES.contains(name);
					annotationCount.put(name, annotationCount.get(name) == null ? 1 : annotationCount.get(name) + 1);
				}
			}
			// アノテーションの数に応じて処理
			if (annotationCount.size() == 0) {
				// Do Nothing
			} else if (annotationCount.size() > 1) {
				throw new IllegalStateException("Invalid annotation combination");
			} else {
				switch (annotationCount.keySet().iterator().next()) {
				case FORCE_ANNOTATION_NAME:
					for (MethodAnnotationPair pair : methodAnnotationPairs) {
						// forceのメソッドのみ1、他は0
						if (pair.getAnnotation() != null && 
								pair.getAnnotation().getName().equals(FORCE_ANNOTATION_NAME)) {
							pair.getMethod().setWeight(1d);
						} else {
							pair.getMethod().setWeight(0d);
						}
					}
					break;
				case RATIO_ANNOTATION_NAME:
					// ExecRatioのないメソッドの数と、余る割合を計算
					double rest = 1;
					int restCount = 0;
					for (MethodAnnotationPair pair : methodAnnotationPairs) {
						if (pair.getAnnotation() != null && 
								pair.getAnnotation().getName().equals(RATIO_ANNOTATION_NAME)) {
							rest -= Double.parseDouble(pair.getAnnotation().getArgs().get(0));
						} else {
							restCount++;
						}
					}
					rest = rest >= 0 ? rest : 0;
					// ExecRatioのないメソッドはすべてこの重み
					double restWeight = rest / restCount;
					// 重みの設定
					for (MethodAnnotationPair pair : methodAnnotationPairs) {
						if (pair.getAnnotation() != null && 
								pair.getAnnotation().getName().equals(RATIO_ANNOTATION_NAME)) {
							pair.getMethod().setWeight(Double.parseDouble(pair.getAnnotation().getArgs().get(0)));
						} else {
							pair.getMethod().setWeight(restWeight);
						}
					}
					break;
				case WEIGHT_ANNOTATION_NAME:
					for (MethodAnnotationPair pair : methodAnnotationPairs) {
						if (pair.getAnnotation() != null && 
								pair.getAnnotation().getName().equals(WEIGHT_ANNOTATION_NAME)) {
							pair.getMethod().setWeight(Double.parseDouble(pair.getAnnotation().getArgs().get(0)));
						} else {
							pair.getMethod().setWeight(0);  // 設定なしの場合は0
						}
					}
					break;
				case IGNORE_ANNOTATION_NAME:
					for (MethodAnnotationPair pair : methodAnnotationPairs) {
						// forceのメソッドのみ0、他は1
						if (pair.getAnnotation() != null && 
								pair.getAnnotation().getName().equals(IGNORE_ANNOTATION_NAME)) {
							pair.getMethod().setWeight(0);
						} else {
							pair.getMethod().setWeight(1);
						}
					}
					break;
				}
				weightedUncertaintyBeans.add(uncertaintyBean);
			}
		}
		return weightedUncertaintyBeans;
	}
	
	private static List<MethodAnnotationPair> generateMethodAnnotationPairs(Model model, UncertaintyBean uncertaintyBean) {
		List<MethodAnnotationPair> methodAnnotationPairs = new LinkedList<>();
		for (IMethodBean iMethodBean : uncertaintyBean.getMethods()) {
			MethodAnnotationPair methodAnnotationPair = new MethodAnnotationPair(iMethodBean, null);
			if (iMethodBean instanceof MethodBean) {
				MethodBean methodBean = (MethodBean)iMethodBean;
				Method method = ArchModelUtils.searchMethodByInterfaceAndName(
						model, methodBean.getClassName(), methodBean.getName());
				for (Annotation annotation : method.getAnnotations()) {
					if (EXEC_ANNOTATION_NAMES.contains(annotation.getName())) {
						methodAnnotationPair.setAnnotation(annotation);
						break;
					}
				}
			}
			methodAnnotationPairs.add(methodAnnotationPair);
		}
		return methodAnnotationPairs;
	}
	
	@SuppressWarnings("unused")
	private static class MethodAnnotationPair {
		
		private IMethodBean method;
		private Annotation annotation;
		
		public MethodAnnotationPair(IMethodBean method, Annotation annotation) {
			super();
			this.method = method;
			this.annotation = annotation;
		}
		public IMethodBean getMethod() {
			return method;
		}
		public void setMethod(IMethodBean method) {
			this.method = method;
		}
		public Annotation getAnnotation() {
			return annotation;
		}
		public void setAnnotation(Annotation annotation) {
			this.annotation = annotation;
		}
		
	}

	/*
	 * 現在アノテーションの引数に文字列を与えているとき、annotation.getArgs().get(0)で得られる値はクオーテーションを含んでいる。
	 * そこでこのメソッドは"abc"をabcにする。将来的にはxtextの機能でクオーテーションなしの値が返るようにすべきである。
	 */
	private static String stripQuote(String string) {
		return string.substring(1, string.length() - 1);
	}
	
	private static String labelForIndex(int index) {
		return String.format("%07d", index);
	}

}
