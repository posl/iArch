package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
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
import jp.ac.kyushu.iarch.checkplugin.model.BehaviorPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.CallPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;

public class ConnectorToFSP{
	
	public String convert(Model archface){
		if(archface.getConnectors().isEmpty()){
			if(archface.getU_connectors().isEmpty()){
				return null;
			}
			else{
			}
		}
		//certain connectorをFSPに変換したものをcodeに入れていく
		//uncertain connectorをFSPに変換したものをucodeに入れていく
		String code = "";
		String ucode  = "";
		if(!archface.getConnectors().isEmpty()){
			code = certainBehaviorFSP(archface);
		}else{
			code = "";
		}
		
		if(!archface.getU_connectors().isEmpty()){
			ucode = uncertainBehaviorFSP(archface);
		}else{
			ucode = "";
		}
		System.out.println(ucode);
		if(code.isEmpty() && ucode.isEmpty()){
			return null;
		}
		String fspcode = code + ucode;
		
		fspcode = fspcode.substring(0,fspcode.length()-2);
		fspcode += ".";
		System.out.println(fspcode);
		return fspcode;
	}
	
	//certain connectorをFSPに変換
	public String certainBehaviorFSP(Model archface){
		String certainCode = "";
		for(Connector connector: archface.getConnectors()){
			for(Behavior behavior: connector.getBehaviors()){
				certainCode += "property " + behavior.getInterface().getName() + " = (";
					for(Method methodcall : behavior.getCall()){
						certainCode +=   "_" +  ((Interface) methodcall.eContainer()).getName() + "." + methodcall.getName() + " -> ";
					}
					certainCode += behavior.getEnd().getName() + ").\n";
				}
		}
		return certainCode;
	}
	
	//uncertain connectorをFSPに変換
	public String uncertainBehaviorFSP(Model archface){
		String uncertainCode = "";
		for(UncertainConnector uconnector : archface.getU_connectors()){
			for(UncertainBehavior ubehavior : uconnector.getU_behaviors()){
				uncertainCode += "U" + ubehavior.getName() + " = (";
				ArrayList<String> altmethods = countaltmethod(ubehavior);
				if(altmethods.size() != 0){
					uncertainCode += printwithAltMethod(altmethods,ubehavior,archface);
					continue;
				}
				int numopt = countoptmethod(ubehavior,archface);
				int roop=1;
				for(int i=0;i<numopt;i++){
					roop=roop*2;
				}
				int[] optbit = new int[numopt];
				while(roop>0){
					int i=0;
					for(SuperCall supercall : ubehavior.getCall()){
						String supercallType = supercall.eClass().getName();
						if(!"OptCall".equals(supercallType) &&
						   !"AltCall".equals(supercallType)){
							//元々のコード
							//uncertainCode += "_" + ((Interface) supercall.getName().eContainer()).getName() + "." + supercall.getName().getName() + " -> ";
							uncertainCode += "_" + ((Interface) supercall.getName().eContainer()).getName() + "." + ((Method) supercall.getName()).getName() + " -> ";
						}else if("OptCall".equals(supercallType)){
							if(optbit[i]==1){
								//元々のコード
								//uncertainCode += "_" + ((UncertainInterface) supercall.getName().eContainer()).getName() + "." + supercall.getName().getName() + " -> ";
								uncertainCode += "_" + ((UncertainInterface) supercall.getName().eContainer().eContainer()).getName() + "." + ((Method) supercall.getName()).getName() + " -> ";
							}i++;
						}else if("AltCall".equals(supercallType)){
							System.out.println("ERROE ALTMETHODPRINTL");
						}
					}
					uncertainCode += "U" + ubehavior.getEnd().getName() + " |\n	";	
					roop--;
					createbit(optbit);
				}
			uncertainCode = uncertainCode.substring(0,uncertainCode.length()-3);
			uncertainCode += ").\n";
			}
		}
		return uncertainCode;
	}
	
	//optionalなメソッドの数を数える
	public int countoptmethod(UncertainBehavior ubehavior,Model archface){
		int numopt=0;
		int roop=0;
			for(SuperCall supercall : ubehavior.getCall()){
					roop++;
					if("OptCall".equals(supercall.eClass().getName())){
						numopt++;
					}
			}
			return numopt;
	}
	
	//alternativeなメソッドの数を数える
	public ArrayList<String> countaltmethod(UncertainBehavior ubehavior){
		ArrayList<String> altmethods = new ArrayList<String>();
		for(SuperCall supercall : ubehavior.getCall()){
			if("AltCall".equals(supercall.eClass().getName())){
				//元々のコード
				//altmethods.add(supercall.getName().getName());
				altmethods.add(supercall.getName().toString());
			}
		}
		return altmethods;
	}
	
	//alternativeなメソッドにあるメソッドをリストにする
	//{void a(),void b(),void c()}というalternativeなメソッドがあればa(),b(),c()をリストにする
	public ArrayList<ArrayList<String>> createAltMethod(Model archface){
		ArrayList<ArrayList<String>> altlist = new ArrayList<ArrayList<String>>();
		ArrayList<String> subalt = new ArrayList<String>();
		
		for(UncertainInterface u_interface : archface.getU_interfaces()){
			for(AltMethod a_method:u_interface.getAltmethods()){
				//元々のコード
				//subalt.add(a_method.getName());
				//subalt.addAll(a_method.getA_name());
				for (Method m : a_method.getMethods()) {
					subalt.add(m.toString());
				}
				altlist.add((ArrayList<String>) subalt.clone());
				subalt.clear();
			}
		}
		return altlist;
	}
	
	//alternativeなメソッドがconnectorに存在する場合の処理
	public String printwithAltMethod(ArrayList<String> altmethods,UncertainBehavior ubehavior,Model archface){
		String uncertainCode = "";
		ArrayList<ArrayList<String>> altlist = createAltMethod(archface);
		ArrayList<ArrayList<String>> altlist_used = new ArrayList<ArrayList<String>>();
		int optroop=1;
		int numopt = countoptmethod(ubehavior,archface);
		for(int i=0;i<numopt;i++){
			optroop=optroop*2;
		}
		int[] optbit = new int[numopt];
		for(int i=0;i<altmethods.size();i++){
			String altMethodName = altmethods.get(i);
			for(int j=0;j<altlist.size();j++){
				ArrayList<String> altNames = altlist.get(j);
				for (String name : altNames) {
					if (altMethodName.equals(name)) {
						altlist_used.add(altNames);
						break;
					}
				}
			}
		}
		while(optroop>0){
			int altroop=1;
			int[] selectAlt = new int[altlist_used.size()];
			for(int k=0;k<altlist_used.size();k++){
				altroop *= altlist_used.get(k).size();
			}
			while(altroop>0){
				int altset = 0;
				int optelement=0;
				for(SuperCall supercall : ubehavior.getCall()){
					String supercallType = supercall.eClass().getName();
					if(!"OptCall".equals(supercallType) &&
					   !"AltCall".equals(supercallType)){
						//今まではsupercall.getName().getName()でメソッド名が取れていたけど、取れなくなった
						
						//元々のコード
						//uncertainCode += "_" + ((Interface) supercall.getName().eContainer()).getName() + "." + supercall.getName().getName() + " -> ";
						uncertainCode += "_" + ((Interface) supercall.getName().eContainer()).getName() + "." + ((Method) supercall.getName()).getName() + " -> ";
					}else if("OptCall".equals(supercallType)){
						if(optbit[optelement]==1){
							//元々のコード
							//uncertainCode += "_" + ((UncertainInterface) supercall.getName().eContainer()).getName() + "." + supercall.getName().getName() + " -> ";
							uncertainCode += "_" + ((UncertainInterface) supercall.getName().eContainer().eContainer()).getName() + "." + ((Method) supercall.getName()).getName() + " -> ";
						}optelement++;
					}else if("AltCall".equals(supercallType)){
						String methodName = selectAlt[altset] == 0 ?
								((Method) supercall.getName()).getName() :
									((Method) ((AltCall) supercall).getA_name().get(selectAlt[altset] - 1)).getName();
						uncertainCode += "_" + ((UncertainInterface) supercall.getName().eContainer().eContainer()).getName() + "." + methodName + " -> ";
						altset++;
					}
				}
				uncertainCode +=  "U" + ubehavior.getEnd().getName() + " |\n	";
				altroop--;
				incleSelectAlt(selectAlt,altlist_used);
			}optroop--;
			 createbit(optbit);
		}
		uncertainCode = uncertainCode.substring(0,uncertainCode.length()-3) + ").\n";
		//uncertainCode += ").\n";
		return uncertainCode;
	}
	
	
	static int[] createbit(int num[]){
		for(int i=0;i<num.length;i++){
			if(num[i] == 0){
				num[i]=1;
				return num;
			}
			else{
				num[i]=0;
			}
		}
		return num;
	}
	
	static int[] incleSelectAlt(int[] selectAlt,ArrayList<ArrayList<String>> altlist){
		for(int i=0;i<selectAlt.length;i++){
			if(selectAlt[i] != altlist.get(i).size()-1){
				selectAlt[i]++;
				return selectAlt;
			}else{
				selectAlt[i]=0;
			}
		}
		return selectAlt;
	}
	
}



