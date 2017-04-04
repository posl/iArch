package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;

public class ConnectorToFSP {
	
	public String convert(Model archface) {
		//certain connectorをFSPに変換したものをcodeに入れていく
		//uncertain connectorをFSPに変換したものをucodeに入れていく
		List<String> code = certainBehaviorFSP(archface);
		List<String> ucode = uncertainBehaviorFSP(archface);
		//System.out.println(ucode);
		if (code.isEmpty() && ucode.isEmpty()) {
			return null;
		}

		StringBuffer sb = new StringBuffer();
		for (String c : code) {
			sb.append(c).append(".\n");
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
		List<String> uncertainCodeList = new ArrayList<String>();
		for (UncertainConnector uconnector : archface.getU_connectors()) {
			for (UncertainBehavior ubehavior : uconnector.getU_behaviors()) {
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
				altmethods.add(supercall.getName().toString());
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
		return getFSPString(i == 0 ? altCall.getName() : altCall.getA_name().get(i - 1));
	}

}
