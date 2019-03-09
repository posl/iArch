package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCallChoice;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Annotation;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;

public class ConnectorToFSP {
	
	public String convert(Model archface) {
		return convert(archface, false);
	}
	public String convert(Model archface, boolean isProbabilistic) {
		//certain connectorをFSPに変換したものをcodeに入れていく
		//uncertain connectorをFSPに変換したものをucodeに入れていく
		List<String> code = certainBehaviorFSP(archface);
		List<String> ucode = uncertainBehaviorFSP(archface, isProbabilistic);
		//System.out.println(ucode);
		if (code.isEmpty() && ucode.isEmpty()) {
			return null;
		}

		StringBuffer sb = new StringBuffer();
		if (isProbabilistic) {
			sb.append("pca\n\n");
		} else {
			for (String c : code) {
				sb.append(c).append(".\n");
			}
		}
		if (!ucode.isEmpty()) {
			boolean firstUc = true;
			for (String uc : ucode) {
				if (firstUc) {
					firstUc = false;
				} else {
					sb.append(",\n");
				}
				sb.append(uc);
			}
			sb.append(".");
		}
		String fspcode = sb.toString();
		System.out.println(fspcode);

		return fspcode;
	}

	//certain connectorをFSPに変換
	public List<String> certainBehaviorFSP(Model archface) {
		List<String> certainCodeList = new ArrayList<String>();
		for (Connector connector : archface.getConnectors()) {
			for (Behavior behavior : connector.getBehaviors()) {
				StringBuffer certainCode = new StringBuffer();

				certainCode.append("property ")
					.append(behavior.getInterface().getName())
					.append(" = (");

				for (Method methodcall : behavior.getCall()) {
					certainCode.append(getFSPString(methodcall)).append(" -> ");
				}

				certainCode.append(behavior.getEnd().getName())
					.append(")");
				certainCodeList.add(certainCode.toString());
			}
		}
		return certainCodeList;
	}

	//uncertain connectorをFSPに変換
	public List<String> uncertainBehaviorFSP(Model archface) {
		return uncertainBehaviorFSP(archface, false);
	}
	public List<String> uncertainBehaviorFSP(Model archface, boolean isProbabilistic) {
		List<String> uncertainCodeList = new ArrayList<String>();
		for (UncertainConnector uconnector : archface.getU_connectors()) {
			for (UncertainBehavior ubehavior : uconnector.getU_behaviors()) {
				if (isProbabilistic) {
					uncertainCodeList.add(getFSPString(ubehavior));
				} else {
					StringBuffer uncertainCode = new StringBuffer();

					uncertainCode.append("U").append(ubehavior.getName())
					.append(" = (");

					List<String> altmethods = countaltmethod(ubehavior);
					if (altmethods.isEmpty()) {
						uncertainCode.append(printWithoutAltMethod(ubehavior, archface));
					} else {
						uncertainCode.append(printwithAltMethod(altmethods, ubehavior, archface));
					}

					uncertainCode.append(")");
					uncertainCodeList.add(uncertainCode.toString());
				}
			}
		}
		return uncertainCodeList;
	}

	//optionalなメソッドの数を数える
	public int countoptmethod(UncertainBehavior ubehavior, Model archface) {
		int numopt = 0;
		for (SuperCall supercall : ubehavior.getCall()) {
			if (supercall instanceof OptCall) {
				numopt++;
			}
		}
		return numopt;
	}
	
	//alternativeなメソッドの数を数える
	public List<String> countaltmethod(UncertainBehavior ubehavior) {
		List<String> altmethods = new ArrayList<String>();
		for (SuperCall supercall : ubehavior.getCall()) {
			if (supercall instanceof AltCall) {
				// Store string representation of the first alternative.
				altmethods.add(((AltCall) supercall).getName().getName().toString());
			}
		}
		return altmethods;
	}

	//alternativeなメソッドにあるメソッドをリストにする
	//{void a(),void b(),void c()}というalternativeなメソッドがあればa(),b(),c()をリストにする
	public List<List<String>> createAltMethod(Model archface) {
		List<List<String>> altlist = new ArrayList<List<String>>();

		for (UncertainInterface u_interface : archface.getU_interfaces()) {
			for (AltMethod a_method : u_interface.getAltmethods()) {
				List<String> subalt = new ArrayList<String>();
				for (Method m : a_method.getMethods()) {
					// Store string representation of each alternative.
					subalt.add(m.toString());
				}
				altlist.add(subalt);
			}
		}
		return altlist;
	}

	public String printWithoutAltMethod(UncertainBehavior ubehavior, Model archface) {
		StringBuffer uncertainCode = new StringBuffer();

		int numopt = countoptmethod(ubehavior, archface);
		int roop = 1;
		for (int i = 0; i < numopt; i++) {
			roop = roop * 2;
		}
		int[] optbit = new int[numopt];

		boolean firstPattern = true;
		while (roop > 0) {
			if (firstPattern) {
				firstPattern = false;
			} else {
				uncertainCode.append(" |\n\t");
			}

			int i = 0;
			for (SuperCall supercall : ubehavior.getCall()) {
				if (supercall instanceof OptCall) {
					if (optbit[i] == 1) {
						uncertainCode.append(getFSPString((OptCall) supercall)).append(" -> ");
					}
					i++;
				} else if (supercall instanceof AltCall) {
					System.out.println("ERROE ALTMETHODPRINTL");
				} else {
					uncertainCode.append(getFSPString((CertainCall) supercall)).append(" -> ");
				}
			}
			uncertainCode.append("U").append(ubehavior.getEnd().getName());
			roop--;
			createbit(optbit);
		}

		return uncertainCode.toString();
	}

	//alternativeなメソッドがconnectorに存在する場合の処理
	public String printwithAltMethod(List<String> altmethods, UncertainBehavior ubehavior, Model archface) {
		StringBuffer uncertainCode = new StringBuffer();

		int numopt = countoptmethod(ubehavior, archface);
		int optroop = 1;
		for (int i = 0; i < numopt; i++) {
			optroop = optroop * 2;
		}
		int[] optbit = new int[numopt];

		List<List<String>> altlist_used = new ArrayList<List<String>>();
		List<List<String>> altlist = createAltMethod(archface);
		for (String firstAltMethod : altmethods) {
			for (List<String> altMethods : altlist){
				for (String name : altMethods) {
					if (firstAltMethod.equals(name)) {
						altlist_used.add(altMethods);
						break;
					}
				}
			}
		}

		boolean firstPattern = true;
		while (optroop > 0) {
			int altroop = 1;
			int[] selectAlt = new int[altlist_used.size()];
			for (int k = 0; k < altlist_used.size(); k++) {
				altroop *= altlist_used.get(k).size();
			}

			while (altroop > 0) {
				if (firstPattern) {
					firstPattern = false;
				} else {
					uncertainCode.append(" |\n\t");
				}

				int altset = 0;
				int optelement = 0;
				for (SuperCall supercall : ubehavior.getCall()) {
					if (supercall instanceof OptCall) {
						if (optbit[optelement] == 1) {
							uncertainCode.append(getFSPString((OptCall) supercall)).append(" -> ");
						}
						optelement++;
					} else if (supercall instanceof AltCall) {
						uncertainCode.append(getFSPString((AltCall) supercall, selectAlt[altset])).append(" -> ");
						altset++;
					} else {
						uncertainCode.append(getFSPString((CertainCall) supercall)).append(" -> ");
					}
				}
				uncertainCode.append("U").append(ubehavior.getEnd().getName());
				altroop--;
				incleSelectAlt(selectAlt, altlist_used);
			}

			optroop--;
			createbit(optbit);
		}

		return uncertainCode.toString();
	}

	static int[] createbit(int num[]) {
		for (int i = 0; i < num.length; i++) {
			if (num[i] == 0) {
				num[i] = 1;
				return num;
			} else {
				num[i] = 0;
			}
		}
		return num;
	}

	static int[] incleSelectAlt(int[] selectAlt, List<List<String>> altlist) {
		for (int i = 0; i < selectAlt.length; i++) {
			if (selectAlt[i] != altlist.get(i).size() - 1) {
				selectAlt[i]++;
				return selectAlt;
			} else {
				selectAlt[i] = 0;
			}
		}
		return selectAlt;
	}

	private static class BranchInfo {
		double probability;
		String identifier;
		BranchInfo(double probability, String identifier) {
			this.probability = probability;
			this.identifier = identifier;
		}
		BranchInfo copy() {
			return new BranchInfo(probability, identifier);
		}
	}

	private List<List<BranchInfo>> convertToBranchInfo(UncertainBehavior uBehavior) {
		List<List<BranchInfo>> info = new ArrayList<List<BranchInfo>>();
		for (SuperCall superCall : uBehavior.getCall()) {
			info.add(convertToBranchInfo(superCall));
		}
		return info;
	}

	private List<BranchInfo> convertToBranchInfo(SuperCall superCall) {
		if (superCall instanceof CertainCall) {
			return Arrays.asList(new BranchInfo(1.0, getFSPString((CertainCall) superCall)));
		} else if (superCall instanceof OptCall) {
			OptCall optCall = (OptCall) superCall;

			double probability = 0.5;
			if (! optCall.getAnnotations().isEmpty()) {
				Annotation annotation = optCall.getAnnotations().get(0);
				String aName = annotation.getName();
				if ("ExecForce".equals(aName)) {
					probability = 1.0;
				} else if ("ExecIgnore".equals(aName)) {
					probability = 0.0;
				} else if ("ExecRatio".equals(aName)) {
					if (! annotation.getArgs().isEmpty()) {
						try {
							probability = Double.parseDouble(annotation.getArgs().get(0));
						} catch (NumberFormatException e) {
							System.err.println("ignored ExecRatio with non-number.");
						}
					} else {
						System.err.println("ignored ExecRatio without arguments.");
					}
				} else if ("ExecWeight".equals(aName)) {
					System.err.println("ignored ExecWeight.");
				}
			}

			if (probability <= 0.0) {
				return Arrays.asList(new BranchInfo(1.0, null));
			} else if (probability >= 1.0) {
				return Arrays.asList(new BranchInfo(1.0, getFSPString((OptCall) superCall)));
			} else {
				BranchInfo bi1 = new BranchInfo(1.0 - probability, null);
				BranchInfo bi2 = new BranchInfo(probability, getFSPString((OptCall) superCall));
				return Arrays.asList(bi1, bi2);
			}
		} else if (superCall instanceof AltCall) {
			AltCall altCall = (AltCall) superCall;

			List<Annotation> annotationList = new ArrayList<Annotation>();
			if (altCall.getName().getAnnotations().isEmpty()) {
				annotationList.add(null);
			} else {
				annotationList.add(altCall.getName().getAnnotations().get(0));
			}
			for (AltCallChoice choice : altCall.getA_name()) {
				if (choice.getAnnotations().isEmpty()) {
					annotationList.add(null);
				} else {
					annotationList.add(choice.getAnnotations().get(0));
				}
			}
			int size = annotationList.size();

			List<String> execForceChoices = new ArrayList<String>();
			for (int i = 0; i < size; ++i) {
				Annotation an = annotationList.get(i);
				if (an != null && "ExecForce".equals(an.getName())) {
					execForceChoices.add(getFSPString(altCall, i));
				}
			}
			if (! execForceChoices.isEmpty()) {
				double probability = 1.0 / execForceChoices.size();
				List<BranchInfo> bis = new ArrayList<BranchInfo>();
				for (String identifier : execForceChoices) {
					bis.add(new BranchInfo(probability, identifier));
				}
				return bis;
			}

			boolean hasExecRatio = false;
			boolean hasExecWeight = false;
			for (int i = 0; i < size; ++i) {
				Annotation an = annotationList.get(i);
				if (an != null) {
					if ("ExecRatio".equals(an.getName())) {
						if (! an.getArgs().isEmpty()) {
							try {
								Double.parseDouble(an.getArgs().get(0));
								hasExecRatio = true;
							} catch (NumberFormatException e) {
								System.err.println("ignored ExecRatio with non-number.");
							}
						}
					} else if ("ExecWeight".equals(an.getName())) {
						if (! an.getArgs().isEmpty()) {
							try {
								Double.parseDouble(an.getArgs().get(0));
								hasExecWeight = true;
							} catch (NumberFormatException e) {
								System.err.println("ignored ExecWeight with non-number.");
							}
						}
					}
				}
			}
			if (hasExecRatio) {
				if (hasExecWeight) {
					System.err.println("cannot set ExecRatio and ExecWeight.");
				} else {
					double[] ratio = new double[size];
					for (int i = 0; i < size; ++i) {
						ratio[i] = 0.0;
						Annotation an = annotationList.get(i);
						if (an != null) {
							if ("ExecRatio".equals(an.getName())) {
								if (! an.getArgs().isEmpty()) {
									try {
										ratio[i] = Double.parseDouble(an.getArgs().get(0));
									} catch (NumberFormatException e) {
									}
								}
							}
						}
					}
					double totalRatio = 0.0;
					int countPositive = 0;
					for (int i = 0; i < size; ++i) {
						if (ratio[i] > 0.0) {
							totalRatio += ratio[i];
							countPositive++;
						}
					}
					if (totalRatio >= 1.0) {
						List<BranchInfo> bis = new ArrayList<BranchInfo>();
						for (int i = 0; i < size; ++i) {
							if (ratio[i] > 0.0) {
								bis.add(new BranchInfo(ratio[i] / totalRatio,
										getFSPString(altCall, i)));
							}
						}
						return bis;
					} else if (countPositive > 0) {
						double rest = (1.0 - totalRatio) / (size - countPositive);
						List<BranchInfo> bis = new ArrayList<BranchInfo>();
						for (int i = 0; i < size; ++i) {
							double p = ratio[i] > 0.0 ? ratio[i] : rest;
							bis.add(new BranchInfo(p, getFSPString(altCall, i)));
						}
						return bis;
					}
				}
			} else if (hasExecWeight) {
				double[] weight = new double[size];
				for (int i = 0; i < size; ++i) {
					weight[i] = 0.0;
					Annotation an = annotationList.get(i);
					if (an != null) {
						if ("ExecWeight".equals(an.getName())) {
							if (! an.getArgs().isEmpty()) {
								try {
									weight[i] = Double.parseDouble(an.getArgs().get(0));
								} catch (NumberFormatException e) {
								}
							}
						}
					}
				}
				double totalWeight = 0.0;
				for (int i = 0; i < size; ++i) {
					if (weight[i] > 0.0) {
						totalWeight += weight[i];
					}
				}
				if (totalWeight > 0.0) {
					List<BranchInfo> bis = new ArrayList<BranchInfo>();
					for (int i = 0; i < size; ++i) {
						if (weight[i] > 0.0) {
							double p = weight[i] / totalWeight;
							bis.add(new BranchInfo(p, getFSPString(altCall, i)));
						}
					}
					return bis;
				}
			}

			List<String> validChoices = new ArrayList<String>();
			for (int i = 0; i < size; ++i) {
				Annotation an = annotationList.get(i);
				if (!(an != null && "ExecIgnore".equals(an.getName()))) {
					validChoices.add(getFSPString(altCall, i));
				}
			}
			if (validChoices.isEmpty()) {
				System.err.println("ignored ExecIgnore for all choices.");
				List<BranchInfo> bis = new ArrayList<BranchInfo>();
				for (int i = 0; i < size; ++i) {
					bis.add(new BranchInfo(1.0 / size, getFSPString(altCall, i)));
				}
				return bis;
			} else {
				double probability = 1.0 / validChoices.size();
				List<BranchInfo> bis = new ArrayList<BranchInfo>();
				for (String identifier : validChoices) {
					bis.add(new BranchInfo(probability, identifier));
				}
				return bis;
			}
		}
		return null;
	}

	private List<List<BranchInfo>> flattenBranchInfo(List<List<BranchInfo>> info) {
		List<List<BranchInfo>> flattenInfo = null;
		for (List<BranchInfo> branch : info) {
			if (flattenInfo == null) {
				flattenInfo = new ArrayList<List<BranchInfo>>();
				for (BranchInfo choice : branch) {
					List<BranchInfo> flattenChoice = new ArrayList<BranchInfo>();
					flattenChoice.add(choice.copy());
					flattenInfo.add(flattenChoice);
				}
			} else {
				List<List<BranchInfo>> newInfo = new ArrayList<List<BranchInfo>>();
				for (List<BranchInfo> flattenChoice : flattenInfo) {
					for (BranchInfo choice : branch) {
						List<BranchInfo> newChoice = new ArrayList<BranchInfo>();
						for (BranchInfo f : flattenChoice) {
							newChoice.add(f.copy());
						}
						newChoice.add(choice.copy());
						newInfo.add(newChoice);
					}
				}
				flattenInfo = newInfo;
			}
		}

		for (List<BranchInfo> choice : flattenInfo) {
			for (int j = 1; j < choice.size(); ++j) {
				choice.get(0).probability *= choice.get(j).probability;
				choice.get(j).probability = 1.0;
			}
		}
		return flattenInfo;
	}

	private String getFSPString(UncertainBehavior uBehavior) {
		List<List<BranchInfo>> info = convertToBranchInfo(uBehavior);
		info = flattenBranchInfo(info);

		StringBuffer sb = new StringBuffer();
		sb.append("U").append(uBehavior.getName()).append(" = (");

		boolean firstChoice = true;
		for (List<BranchInfo> choice : info) {
			if (firstChoice) {
				firstChoice = false;
			} else {
				sb.append(" |\n\t");
			}

			sb.append("<").append(choice.get(0).probability).append("> ");
			boolean first = true;
			for (BranchInfo step : choice) {
				if (step.identifier != null) {
					if (first) {
						first = false;
					} else {
						sb.append("<1.0> ");
					}
					sb.append(step.identifier).append(" -> ");
				}
			}
			sb.append("U").append(uBehavior.getEnd().getName());
		}

		return sb.append(")").toString();
	}

	private String getFSPString(Method methodCall) {
		String className = ArchModelUtils.getClassName(methodCall, true);
		String methodName = methodCall.getName();
		return "_" + className + "." + methodName;
	}
	private String getFSPString(SuperMethod superMethod) {
		Interface cInterface = ArchModelUtils.getInterface(superMethod);
		String className = cInterface != null ? cInterface.getName() : "";
		String methodName = superMethod instanceof Method ? ((Method) superMethod).getName() : "";
		return "_" + className + "." + methodName;
	}
	private String getFSPString(CertainCall certainCall) {
		return getFSPString(certainCall.getName());
	}
	private String getFSPString(OptCall optCall) {
		return getFSPString(optCall.getName());
	}
	private String getFSPString(AltCall altCall, int i) {
		return getFSPString(i == 0 ? altCall.getName().getName()
				: altCall.getA_name().get(i - 1).getName());
	}

}
