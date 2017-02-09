package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;

public class ConnectorToFSP {
	
	public String convert(Model archface) {
		//certain connectorをFSPに変換したものをcodeに入れていく
		//uncertain connectorをFSPに変換したものをucodeに入れていく
		String code = "";
		if (!archface.getConnectors().isEmpty()) {
			code = certainBehaviorFSP(archface);
		}
		String ucode  = "";
		if (!archface.getU_connectors().isEmpty()) {
			ucode = uncertainBehaviorFSP(archface);
		}
		System.out.println(ucode);
		if (code.isEmpty() && ucode.isEmpty()) {
			return null;
		}

		String fspcode = code + ucode;
		fspcode = fspcode.substring(0, fspcode.length() - 2) + ".";
		System.out.println(fspcode);
		return fspcode;
	}

	//certain connectorをFSPに変換
	public String certainBehaviorFSP(Model archface) {
		StringBuffer certainCode = new StringBuffer();
		for (Connector connector : archface.getConnectors()) {
			for (Behavior behavior : connector.getBehaviors()) {
				certainCode.append("property ")
					.append(behavior.getInterface().getName())
					.append(" = (");
				for (Method methodcall : behavior.getCall()) {
					certainCode.append("_")
						.append(((Interface) methodcall.eContainer()).getName())
						.append(".")
						.append(methodcall.getName())
						.append(" -> ");
				}
				certainCode.append(behavior.getEnd().getName())
					.append(").\n");
			}
		}
		return certainCode.toString();
	}

	//uncertain connectorをFSPに変換
	public String uncertainBehaviorFSP(Model archface) {
		StringBuffer uncertainCode = new StringBuffer();
		for (UncertainConnector uconnector : archface.getU_connectors()) {
			for (UncertainBehavior ubehavior : uconnector.getU_behaviors()) {
				uncertainCode.append("U").append(ubehavior.getName())
					.append(" = (");

				List<String> altmethods = countaltmethod(ubehavior);
				if (!altmethods.isEmpty()) {
					uncertainCode.append(printwithAltMethod(altmethods, ubehavior, archface));
					continue;
				}

				int numopt = countoptmethod(ubehavior, archface);
				int roop = 1;
				for (int i = 0; i < numopt; i++) {
					roop = roop * 2;
				}
				int[] optbit = new int[numopt];
				while (roop > 0) {
					int i = 0;
					for (SuperCall supercall : ubehavior.getCall()) {
						if (supercall instanceof OptCall) {
							if (optbit[i] == 1) {
								uncertainCode.append("_").append(((UncertainInterface) supercall.getName().eContainer().eContainer()).getName())
									.append(".")
									.append(((Method) supercall.getName()).getName())
									.append(" -> ");
							}
							i++;
						} else if (supercall instanceof AltCall) {
							System.out.println("ERROE ALTMETHODPRINTL");
						} else {
							uncertainCode.append("_").append(((Interface) supercall.getName().eContainer()).getName())
								.append(".")
								.append(((Method) supercall.getName()).getName())
								.append(" -> ");
						}
					}
					uncertainCode.append("U").append(ubehavior.getEnd().getName())
						.append(" |\n\t");
					roop--;
					createbit(optbit);
				}
				uncertainCode.delete(uncertainCode.length() - 3, uncertainCode.length());
				uncertainCode.append(").\n");
			}
		}
		return uncertainCode.toString();
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

		while (optroop > 0) {
			int altroop = 1;
			int[] selectAlt = new int[altlist_used.size()];
			for (int k = 0; k < altlist_used.size(); k++) {
				altroop *= altlist_used.get(k).size();
			}

			while (altroop > 0) {
				int altset = 0;
				int optelement = 0;
				for (SuperCall supercall : ubehavior.getCall()) {
					if (supercall instanceof OptCall) {
						if (optbit[optelement] == 1) {
							uncertainCode.append("_").append(((UncertainInterface) supercall.getName().eContainer().eContainer()).getName())
								.append(".")
								.append(((Method) supercall.getName()).getName())
								.append(" -> ");
						}
						optelement++;
					} else if (supercall instanceof AltCall) {
						String methodName = selectAlt[altset] == 0 ?
								((Method) supercall.getName()).getName() :
									((Method) ((AltCall) supercall).getA_name().get(selectAlt[altset] - 1)).getName();
						uncertainCode.append("_").append(((UncertainInterface) supercall.getName().eContainer().eContainer()).getName())
							.append(".")
							.append(methodName)
							.append(" -> ");
						altset++;
					} else {
						uncertainCode.append("_").append(((Interface) supercall.getName().eContainer()).getName())
							.append(".")
							.append(((Method) supercall.getName()).getName())
							.append(" -> ");
					}
				}
				uncertainCode.append("U").append(ubehavior.getEnd().getName())
					.append(" |\n\t");
				altroop--;
				incleSelectAlt(selectAlt, altlist_used);
			}
			optroop--;
			createbit(optbit);
		}
		uncertainCode.delete(uncertainCode.length() - 3, uncertainCode.length());
		uncertainCode.append(").\n");
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

}
