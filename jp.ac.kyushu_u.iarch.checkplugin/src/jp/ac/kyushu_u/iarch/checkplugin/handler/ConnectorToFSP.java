package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCallChoice;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Annotation;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;

public class ConnectorToFSP {
	
	public String convert(Model archface) {
		return convert(archface, false);
	}
	public String convert(Model archface, boolean isProbabilistic) {
		//certain connectorをFSPに変換したものをcodeに入れていく
		//uncertain connectorをFSPに変換したものをucodeに入れていく
		List<String> code = certainBehaviorFSP(archface);
		Map<String, Set<String>> refSymbolsMap = new HashMap<String, Set<String>>();
		List<String> ucode = uncertainBehaviorFSP(archface, isProbabilistic, refSymbolsMap);
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
					sb.append(".\n");
				}
				sb.append(uc);
			}
			sb.append(".");

			String compositeProcess = convertRefSymbols(refSymbolsMap);
			if (compositeProcess != null) {
				sb.append("\n").append(compositeProcess);
			}
		}
		String fspcode = sb.toString();
		//System.out.println(fspcode);

		return fspcode;
	}

	// Naming conventions.
	private String makeStateName(String symbol) {
		return "U" + (symbol != null ? symbol : "null");
	}
	private String makeVariableName(String symbol) {
		return "_" + (symbol != null ? symbol : "null");
	}

	// Make a composite process.
	private String convertRefSymbols(Map<String, Set<String>> refSymbolsMap) {
		List<String> processes = new ArrayList<String>();

		Map<String, String> processMap = new HashMap<String, String>();
		Set<String> resources = new HashSet<String>();

		// parallel processes.
		int processCount = 1;
		List<String> symbols = new ArrayList<String>(refSymbolsMap.keySet());
		Collections.sort(symbols);
		for (String symbol : symbols) {
			Set<String> refSymbols = refSymbolsMap.get(symbol);
			if (!refSymbols.isEmpty()) {
				String processName = "p" + (processCount++);
				processMap.put(symbol, processName);
				resources.addAll(refSymbols);

				processes.add(processName + ":" + makeStateName(symbol));
			}
		}

		// common resources.
		for (String resource : resources) {
			List<String> useProcesses = new ArrayList<String>();
			for (String symbol : processMap.keySet()) {
				Set<String> refSymbols = refSymbolsMap.get(symbol);
				if (refSymbols.contains(resource)) {
					useProcesses.add(processMap.get(symbol));
				}
			}
			if (!useProcesses.isEmpty()) {
				Collections.sort(useProcesses);
				StringBuffer sb = new StringBuffer("{");
				for (String up : useProcesses) {
					if (up != useProcesses.get(0)) {
						sb.append(",");
					}
					sb.append(up);
				}
				sb.append("}::").append(makeVariableName(resource))
					.append(":").append(makeStateName(resource));
				processes.add(sb.toString());
			}
		}

		if (processes.isEmpty()) {
			return null;
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append("||SYS = (");
			for (String p : processes) {
				if (p != processes.get(0)) {
					sb.append(" || ");
				}
				sb.append(p);
			}
			sb.append(").");
			return sb.toString();
		}
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
		return uncertainBehaviorFSP(archface, false, null);
	}
	public List<String> uncertainBehaviorFSP(Model archface,
			boolean isProbabilistic, Map<String, Set<String>> refSymbolsMap) {
		List<String> uncertainCodeList = new ArrayList<String>();
		for (UncertainConnector uconnector : archface.getU_connectors()) {
			for (UncertainBehavior ubehavior : uconnector.getU_behaviors()) {
				Set<String> refSymbols = new HashSet<String>();
				uncertainCodeList.add(getFSPString(ubehavior, isProbabilistic, refSymbols));
				if (refSymbolsMap != null) {
					String symbol = ubehavior.getName();
					refSymbolsMap.put(symbol, refSymbols);
				}
			}
		}
		return uncertainCodeList;
	}

	private static class BranchInfo {
		double probability;
		// action: <probability>method -> (next)
		Method method;
		List<BranchInfo> next;
		// or end state. it should not be in a branch.
		Interface endState;

		BranchInfo copy() {
			BranchInfo bi = new BranchInfo();
			bi.probability = probability;
			bi.method = method;
			bi.next = next == null ? null : new ArrayList<BranchInfo>(next);
			bi.endState = endState;
			return bi;
		}
	}

	private List<BranchInfo> convertToBranchInfo(UncertainBehavior uBehavior, boolean considerAnnotation) {
		BranchInfo tail = new BranchInfo();
		tail.probability = 1.0;
		tail.endState = uBehavior.getEnd();
		List<BranchInfo> heads = Arrays.asList(tail);
		// to indicate the possibility that endState is in a branch.
		boolean safeTail = false;

		List<SuperCall> superCalls = uBehavior.getCall();
		for (int i = superCalls.size() - 1; i >= 0; --i) {
			SuperCall superCall = superCalls.get(i);

			if (superCall instanceof CertainCall) {
				// (<q1>Tail1 | <q2>Tail2 ...)
				// => <1.0>Certain -> (<q1>Tail1 | <q2>Tail2 ...)
				BranchInfo bi = new BranchInfo();
				bi.probability = 1.0;
				bi.method = (Method) ((CertainCall) superCall).getName();
				bi.next = heads;

				heads = Arrays.asList(bi);
				if (!safeTail){
					heads = joinBranch(heads);
					safeTail = true;
				}
			} else if (superCall instanceof OptCall) {
				// (<q1>Tail1 | <q2> Tail2 ...)
				// => (<p>Opt -> (<q1>Tail1 ...) | <(1-p)*q1>Tail1 ...)
				OptCall optCall = (OptCall) superCall;

				double probability;
				if (considerAnnotation) {
					probability = getOptCallProbability(optCall);
				} else {
					probability = 0.5;
				}

				List<BranchInfo> bis = new ArrayList<BranchInfo>();
				// call
				if (probability > 0.0) {
					BranchInfo bi1 = new BranchInfo();
					bi1.probability = probability;
					bi1.method = (Method) optCall.getName();
					bi1.next = heads;
					bis.add(bi1);
				}
				// not call
				if (probability < 1.0) {
					for (BranchInfo bi : heads) {
						BranchInfo bi2 = bi.copy();
						bi2.probability *= 1.0 - probability;
						bis.add(bi2);
					}
				} else {
					safeTail = true;
				}
				heads = bis;
			} else if (superCall instanceof AltCall) {
				// (<q1>Tail1 | <q2>Tail2 ...)
				// => (<p1>Alt1 -> (<q1>Tail1 ...) | <p2>Alt2 -> (<q1>Tail1 ...))
				AltCall altCall = (AltCall) superCall;

				double[] probabilities;
				if (considerAnnotation) {
					probabilities = getAltCallProbabilities(altCall);
				} else {
					int size = 1 + altCall.getA_name().size();
					probabilities = new double[size];
					for (int j = 0; j < size; ++j) {
						probabilities[j] = 1.0 / size;
					}
				}

				List<BranchInfo> bis = new ArrayList<BranchInfo>();
				int j = 0;
				if (probabilities[j] > 0.0) {
					BranchInfo bi1 = new BranchInfo();
					bi1.probability = probabilities[j++];
					bi1.method = (Method) altCall.getName().getName();
					bi1.next = heads;
					bis.add(bi1);
				} else {
					j++;
				}
				for (AltCallChoice altCallChoice : altCall.getA_name()) {
					if (probabilities[j] > 0.0) {
						BranchInfo bi2 = new BranchInfo();
						bi2.probability = probabilities[j++];
						bi2.method = (Method) altCallChoice.getName();
						bi2.next = heads;
						bis.add(bi2);
					} else {
						j++;
					}
				}

				heads = bis;
				if (!safeTail){
					heads = joinBranch(heads);
					safeTail = true;
				}
			}
		}
		return heads;
	}
	// example of what this method does:
	// <p>A -> (<q1>Tail1 ... | State)
	// => (<p*z>A -> (<q1/z>Tail1 ...) | <p*(1-z)>A -> State)
	// where z = sum_i(qi)
	private List<BranchInfo> joinBranch(List<BranchInfo> heads) {
		List<BranchInfo> newHeads = new ArrayList<BranchInfo>();
		for (BranchInfo head : heads) {
			BranchInfo endBranch = null;
			if (head.next.size() > 1) {
				for (BranchInfo child : head.next){
					if (child.endState != null) {
						endBranch = child;
						break;
					}
				}
			}
			if (endBranch == null) {
				newHeads.add(head);
			} else {
				BranchInfo head1 = head.copy();
				head1.probability *= 1.0 - endBranch.probability;
				List<BranchInfo> next1 = new ArrayList<BranchInfo>();
				for (BranchInfo child : head1.next) {
					if (child != endBranch) {
						BranchInfo newChild = child.copy();
						newChild.probability /= 1.0 - endBranch.probability;
						next1.add(newChild);
					}
				}
				head1.next = next1;
				newHeads.add(head1);

				BranchInfo head2 = head.copy();
				head2.probability *= endBranch.probability;
				head2.next = Arrays.asList(endBranch);
				newHeads.add(head2);
			}
		}
		return newHeads;
	}

	// Get probability from the annotation of OptCall.
	private double getOptCallProbability(OptCall optCall) {
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
						double p = Double.parseDouble(annotation.getArgs().get(0));
						probability = p > 1.0 ? 1.0 : (p > 0.0 ? p : 0.0);
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
		return probability;
	}
	// Get array of probability from the annotation of AltCall.
	private double[] getAltCallProbabilities(AltCall altCall) {
		// Collect annotations.
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
		double[] probabilities = new double[size];
		double sum = 0.0;

		// 1. If any call has "ExecForce", choose calls which have it.
		for (int i = 0; i < size; ++i) {
			Annotation an = annotationList.get(i);
			if (an != null && "ExecForce".equals(an.getName())) {
				probabilities[i] = 1.0;
				sum += 1.0;
			}
		}
		if (sum > 0.0) {
			for (int i = 0; i < size; ++i) {
				probabilities[i] /= sum;
			}
			return probabilities;
		}

		// 2. If any call has ExecRatio or ExecWeight...
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
				int countPositive = 0;
				int countIgnore = 0;
				for (int i = 0; i < size; ++i) {
					Annotation an = annotationList.get(i);
					if (an != null) {
						if ("ExecRatio".equals(an.getName())) {
							if (! an.getArgs().isEmpty()) {
								try {
									double p = Double.parseDouble(an.getArgs().get(0));
									if (p > 0.0) {
										probabilities[i] = p;
										sum += p;
										countPositive++;
									}
								} catch (NumberFormatException e) {
								}
							}
						} else if ("ExecIgnore".equals(an.getName())) {
							countIgnore++;
						}
					}
				}
				// If total ratio is less than 1,
				// (1 - total_ratio) is distributed to calls
				// which does not have ratios nor ignore labels.
				if (sum >= 1.0 || countPositive + countIgnore >= size) {
					// can not distribute.
					for (int i = 0; i < size; ++i) {
						probabilities[i] /= sum;
					}
					return probabilities;
				} else if (sum > 0.0) {
					double rest = (1.0 - sum) / (size - countPositive - countIgnore);
					for (int i = 0; i < size; ++i) {
						if (probabilities[i] == 0.0) {
							Annotation an = annotationList.get(i);
							if (!(an != null && "ExecIgnore".equals(an.getName()))) {
								probabilities[i] = rest;
							}
						}
					}
					return probabilities;
				}
			}
		} else if (hasExecWeight) {
			for (int i = 0; i < size; ++i) {
				Annotation an = annotationList.get(i);
				if (an != null && "ExecWeight".equals(an.getName())) {
					if (! an.getArgs().isEmpty()) {
						try {
							double p = Double.parseDouble(an.getArgs().get(0));
							if (p > 0.0) {
								probabilities[i] = p;
								sum += p;
							}
						} catch (NumberFormatException e) {
						}
					}
				}
			}
			if (sum > 0.0) {
				for (int i = 0; i < size; ++i) {
					probabilities[i] /= sum;
				}
				return probabilities;
			}
		}

		// 3. excludes ExecIgnore
		for (int i = 0; i < size; ++i) {
			Annotation an = annotationList.get(i);
			if (!(an != null && "ExecIgnore".equals(an.getName()))) {
				probabilities[i] = 1.0;
				sum += 1.0;
			}
		}
		if (sum > 0.0) {
			for (int i = 0; i < size; ++i) {
				probabilities[i] /= sum;
			}
		} else {
			System.err.println("ignored ExecIgnore for all choices.");
			for (int i = 0; i < size; ++i) {
				probabilities[i] = 1.0 / size;
			}
		}
		return probabilities;
	}

	private String getFSPString(String ifName, String stateName,
			BranchInfo bi, boolean printProbability, int indentLevel,
			Set<String> refSymbols) {
		StringBuffer sb = new StringBuffer();
		if (bi.endState != null) {
			//sb.append(makeStateName(bi.endState.getName()));
			sb.append(stateName);
		} else {
			if (printProbability) {
				sb.append("<").append(bi.probability).append("> ");
			}

			Interface cInterface = ArchModelUtils.getInterface(bi.method);
			if (cInterface != null && !ifName.equals(cInterface.getName())) {
				String symbol = cInterface.getName();
				refSymbols.add(symbol);
				sb.append(makeVariableName(symbol)).append(".");
			}
			sb.append(bi.method.getName()).append(" -> ");

			boolean branch = (bi.next.size() > 1);
			if (branch) {
				sb.append("\n");
				for (int i = 0; i <= indentLevel; ++i) {
					sb.append("\t");
				}
				sb.append("(");
				indentLevel++;
			}
			for (BranchInfo child : bi.next) {
				if (child != bi.next.get(0)) {
					sb.append("\n");
					for (int i = 0; i < indentLevel; ++i) {
						sb.append("\t");
					}
					sb.append("| ");
				}
				String childFSP = getFSPString(ifName, stateName,
						child, printProbability, indentLevel, refSymbols);
				sb.append(childFSP);
			}
			if (branch) {
				indentLevel--;
				sb.append(")");
			}
		}
		return sb.toString();
	}

	private String getFSPString(UncertainBehavior uBehavior, boolean isProbabilistic,
			Set<String> refSymbols) {
		List<BranchInfo> info = convertToBranchInfo(uBehavior, isProbabilistic);

		String ifName = uBehavior.getEnd().getName();
		String stateName = makeStateName(uBehavior.getName());

		StringBuffer sb = new StringBuffer();
		sb.append(stateName).append(" = (");
		for (BranchInfo bi : info) {
			if (bi != info.get(0)) {
				sb.append("\n\t| ");
			}
			sb.append(getFSPString(ifName, stateName, bi, isProbabilistic, 0, refSymbols));
		}

		return sb.append(")").toString();
	}

	private String getFSPString(Method methodCall) {
		String className = ArchModelUtils.getClassName(methodCall, true);
		String methodName = methodCall.getName();
		return makeVariableName(className) + "." + methodName;
	}
}
