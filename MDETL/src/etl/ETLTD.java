package etl;


import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.eol.dom.OperationList;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.dom.TypeExpression;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.compile.m3.NamedElement;
import org.eclipse.epsilon.eol.dom.FirstOrderOperationCallExpression;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.OperationCallExpression;
//import org.eclipse.epsilon.eol.dom.
import org.eclipse.epsilon.common.module.*;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.etl.dom.TransformationRule;
import org.eclipse.epsilon.etl.execute.context.EtlContext;
import org.eclipse.epsilon.etl.execute.context.IEtlContext;
import org.eclipse.epsilon.etl.execute.operations.EquivalentOperation;
import org.eclipse.epsilon.etl.strategy.ITransformationStrategy;
import org.eclipse.epsilon.etl.trace.TransformationTrace;
import etl.EpsilonStandalone; 

public class ETLTD {
	
	// List of operation names in the Module (+ equivalent + equivalents) 
	public static ArrayList<String> OpList ; 
	
	// Decomposed list of all children in each rule or operation 
	public static List<List<ModuleElement>> allDecomposedLists ;
	
	// List of traces from children in each rule or operation that are duplicated in the Module   
	public static List<List<ModuleElement>> duplicatedList ; 
	
	// List of all rules and operations which are called from each rule or operation 
	// Place 0 is Rule or Operation name and other places are rules or operation may be called  
	public static List<List<String>> ruleCallGraphs ; 
	
	public static List<List<String>> ruleBiggestSeq ;
	public static List<List<String>> ruleBiggestCycle ; 
	
	public static List<String> biggestSeq ; 
	public static List<String> biggestCycle ; 

	// metamodel elements 
	public static EPackage metapackage ; 
	public static void main(String[] args) throws Exception {
		
		// Load ETL Transformation 
		EtlModule module = new EtlModule();		
//		module.parse(EpsilonStandalone.class.getResource("Copyflowchart.etl").toURI());
//		module.parse(EpsilonStandalone.class.getResource("OO2DB.etl").toURI());
		module.parse(EpsilonStandalone.class.getResource("RefinementsEcore2SimpleCodeDOM.etl").toURI());
		
		if (!module.getParseProblems().isEmpty()) return;		
		
		// Load metamodel 
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
//		Resource res = rs.createResource( URI.createFileURI( "src/models/Flowchart.ecore" ));
//		Resource res = rs.createResource( URI.createFileURI( "src/models/OO.ecore" ));
		Resource res = rs.createResource( URI.createFileURI( "src/models/RefinementsEcore.ecore" ));
		res.load(null);
		
		metapackage = (EPackage)res.getContents().get(0);

		//////////////////////////////////////////

		
		module.getTransformationRules(); // Returns a list of transformation rules
		OpList = new ArrayList<String>() ; 
		OpList.add("equivalent") ;
		OpList.add("equivalents") ;
		for(int i=0; i<module.getOperations().size(); i++){
			Operation op = module.getOperations().get(i) ; 
			OpList.add(op.getName()) ;
		}
		
//		int var = calcVarible(module.getTransformationRules().get(2).getChildren()) ; 
//		System.out.println(var) ;
//		
		int TLOC = calcLOC(module.getRegion().toString()) ;
		System.out.println("Transformation Info:");
		System.out.println("LOC = " + TLOC) ; 
		
		int ETS = 0 ; 
		ETS = calcTotalSize(module.getChildren()) ;
		System.out.println("\nETS = " + ETS) ; 
		
		int ENR = module.getTransformationRules().size() ; 
		System.out.println("ENR = " + ENR) ; 

		int ENO = module.getOperations().size() ; 
		System.out.println("ENO = " + ENO) ; 
		
		System.out.println("\nRules:"); 
		for(int i=0; i<module.getTransformationRules().size(); i++)
		{
			TransformationRule rule = module.getTransformationRules().get(i);
			System.out.print("	" + rule.getName() + " ->") ; 
			rule.getBody(); // Returns the body of rule
			rule.getGuard(); // Returns the guard of rule	
		
			int RLOC = calcLOC(rule.getRegion().toString()) ; 
			System.out.print(" (LOC = " + RLOC ) ;
			
			int EPL = 0 ; 
			if(rule.getSourceParameter()!= null)
				EPL ++ ; 
			EPL += rule.getTargetParameters().size() ;
			EPL += calcVarible(rule.getChildren()) ;
			System.out.print("), (EPL = " + EPL + ")") ;
			
			int ERS = calcOperatorVarSize(rule.getBody().getChildren()) ; 
			System.out.print(", (ERS = " + ERS + ")") ;
			
			int EFO = calcOpCall(rule.getBody().getChildren()) ; 
			System.out.print(", (EFO = " + EFO + ")") ;
			
			int CC = 0 ; 
			if(rule.getGuard() != null)
				CC = calcLogicalExp(rule.getGuard().getChildren()) ; 
			System.out.println(", (CC = " + CC + ")") ;
			
//			ETS += ERS ; 
		}

		
		System.out.println("\nOperations:") ; 
		for(int i=0; i<module.getOperations().size(); i++)
		{
			Operation op = module.getOperations().get(i) ; 
			System.out.print("	" + op.getName() + " ->") ; 
			op.getBody(); // Returns the body of the first rule		
			
			int OLOC = calcLOC(op.getRegion().toString()) ; 
			System.out.print(" (LOC = " + OLOC ) ;
			
			
			int EPL = 0 ; 
			if(op.getReturnTypeExpression()!= null)
				EPL++ ; 
			EPL += op.getFormalParameters().size() ;
			EPL += calcVarible(op.getChildren()) ;
			System.out.print("), (EPL = " + EPL + ")") ; 
			
			
			int EHS = calcOperatorVarSize(op.getBody().getChildren()) ; 
			System.out.print(", (EHS = " + EHS + ")") ;
			

			int EFO = calcOpCall(op.getBody().getChildren()) ; 
			System.out.println(", (EFO = " + EFO + ")") ;
	
//			ETS += EHS ; 
		}
		
		allDecomposedLists = new ArrayList<List<ModuleElement>>() ;
		ruleCallGraphs = new ArrayList<List<String>>() ;
		
		// Check for Pres
		for(int i=0; i<module.getPre().size(); i++){
			ModuleElement me = module.getPre().get(i).getBody() ;  
			List<ModuleElement> mList = decompose(me) ; 
			allDecomposedLists.add(mList) ;
		} 
		
//		// Check for rules
		for(int i=0; i<module.getTransformationRules().size(); i++){
			ModuleElement me = module.getTransformationRules().get(i).getBody() ;  
			
			List<ModuleElement> mList = decompose(me) ; 
			allDecomposedLists.add(mList) ;
			
			List<Parameter> listP = new ArrayList<Parameter>() ; 
			listP.add(module.getTransformationRules().get(i).getSourceParameter()) ;
			List<String> cgList = calculateCall(mList, module.getTransformationRules().get(i).getName(), listP) ; 
			List<String> ruleCalledrules = new ArrayList<String>();
			ruleCalledrules.add(cgList.get(0)) ;
			for(int j=1; j<cgList.size(); j++){
				for (TransformationRule tr : module.getTransformationRules()){
					String ss = tr.getSourceParameter().getTypeName() ; 
					if(tr.getSourceParameter().getTypeName().equals(cgList.get(j)) || cgList.get(j).equals("Any"))
						ruleCalledrules.add(tr.getName());
				}
			}
			// Add name of called operation to calles list of each rule
			for(int j=0 ; j<mList.size(); j++)
			{
				ModuleElement ME = mList.get(j) ;
				if(ME.getClass().getName().contains("FirstOrderOperation")){
					continue ;
				}else if(ME.getClass().getName().contains("Operation")){
					OperationCallExpression m = (OperationCallExpression)ME ;				
					if(!m.getOperationName().equals("equivalent") && !m.getOperationName().equals("equivalents") && OpList.contains(m.getOperationName()))
						ruleCalledrules.add(m.getOperationName()) ;
				}
			}
			ruleCallGraphs.add(ruleCalledrules) ; 
		}
		
		// Check for Operations
		for(int i=0; i<module.getOperations().size(); i++){
			ModuleElement me = module.getOperations().get(i).getBody() ;  
			
			List<ModuleElement> mList = decompose(me) ; 
			allDecomposedLists.add(mList) ;
			
			List<String> cgList = calculateCall(mList, module.getOperations().get(i).getName(), module.getOperations().get(i).getFormalParameters()) ; 
			List<String> ruleCalledrules = new ArrayList<String>();
			ruleCalledrules.add(cgList.get(0)) ;
			for(int j=1; j<cgList.size(); j++){
				for (TransformationRule tr : module.getTransformationRules()){
					if(tr.getSourceParameter().getTypeName().equals(cgList.get(j)) || cgList.get(j).equals("Any"))
						ruleCalledrules.add(tr.getName());
				}
			}
			// Add name of called operation to calles list of each rule
			for(int j=0 ; j<mList.size(); j++)
			{
				ModuleElement ME = mList.get(j) ;
				if(ME.getClass().getName().contains("FirstOrderOperation")){
					continue ;
				}else if(ME.getClass().getName().contains("Operation")){
					OperationCallExpression m = (OperationCallExpression)ME ;				
					if(!m.getOperationName().equals("equivalent") && !m.getOperationName().equals("equivalents") && OpList.contains(m.getOperationName()))
						ruleCalledrules.add(m.getOperationName()) ;
				}
			}
			ruleCallGraphs.add(ruleCalledrules) ; 
		}
		
		// Check for Posts
		for(int i=0; i<module.getPost().size(); i++){
			ModuleElement me = module.getPost().get(i).getBody() ;  
			List<ModuleElement> mList = decompose(me) ; 
			allDecomposedLists.add(mList) ;
		}
		
		int DC = calcDuplication() ; 
		System.out.println("\nDC = "+ DC) ;   
		
		ruleBiggestSeq = new ArrayList<List<String>>() ; 
		ruleBiggestCycle = new ArrayList<List<String>>() ; 
		

		int maxSeqIndex = 0, maxCycIndex = 0 ; 
		int maxSeqSize = 0 , maxCycSize = 0;
		for(int i=0; i<ruleCallGraphs.size(); i++){
			biggestSeq = new ArrayList<String>() ; 
			biggestCycle = new ArrayList<String>() ; 
			List<String> seq = new ArrayList<String>(); 
			if(ruleCallGraphs.get(i).size()>1){
				createCallGraph(i, seq) ;
				ruleBiggestSeq.add(biggestSeq) ;
				ruleBiggestCycle.add(biggestCycle) ;
				if(biggestSeq.size()>maxSeqSize){
					maxSeqIndex = i ; 
					maxSeqSize = biggestSeq.size() ;
				}
				if(biggestCycle.size()>maxCycSize){
					maxCycIndex = i ; 
					maxCycSize = biggestCycle.size() ; 
				}
			}else{
				List<String> temp = new ArrayList<String>() ; 
				temp.add(ruleCallGraphs.get(i).get(0)) ;
				ruleBiggestCycle.add(temp) ;
				ruleBiggestSeq.add(temp) ;
			}
		}
		
		System.out.println(); 
		// max Sequenc Size:
		System.out.println("CBR(X): " + maxSeqSize);
//		System.out.print("    ") ;
//		for(int i=0; i<ruleBiggestSeq.get(maxSeqIndex).size(); i++){
//			System.out.print(ruleBiggestSeq.get(maxSeqIndex).get(i)) ;
//			if(i==(ruleBiggestSeq.get(maxSeqIndex).size()-1))
//				System.out.println();
//			else
//				System.out.print(" -> ");
//		}
		
		//max calls in Cyclic Sequenc Size: 
		if(maxSeqSize > 2)
			System.out.println("CBR(Y): " + (maxSeqSize-2));
		else 
			System.out.println("CBR(Y): " + 0);
		System.out.print("    ") ;
//		for(int i=0; i<ruleBiggestCycle.get(maxCycIndex).size(); i++){
//			System.out.print(ruleBiggestCycle.get(maxCycIndex).get(i)) ;
//			if(i==(ruleBiggestCycle.get(maxCycIndex).size()-1))
//				System.out.println();
//			else
//				System.out.print(" -> ");
//		}		
		System.out.print("\nFinish");
	}
	
	public static EmfModel getEmfModel(String name, String model, 
			String metamodel, boolean readOnLoad, boolean storeOnDisposal) 
					throws EolModelLoadingException, URISyntaxException {
		EmfModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI,URI.createFileURI(metamodel).toString());
		properties.put(EmfModel.PROPERTY_MODEL_URI,URI.createFileURI(model).toString());
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, 
				storeOnDisposal + "");
		emfModel.load(properties, (IRelativePathResolver) null);
		return emfModel;
	}
	
	public static int calcLOC(String LOC){
		String[] parts = LOC.split("-");
		String part1 = parts[0];
		String part2 = parts[1];
		
		int begin = Integer.parseInt(part1.split(":")[0]);
		int end = Integer.parseInt(part2.split(":")[0]);
		
		return end - begin + 1 ; 
	}
	
	public static int calcVarible(List<ModuleElement> MEList){
		int varCount = 0 ;
		for(int i=0 ; i<MEList.size(); i++)
		{
			ModuleElement ME = MEList.get(i) ;
	//		System.out.println(ME.getClass().getName().toString()) ; 
			if(ME.getClass().getName().equals("org.eclipse.epsilon.eol.dom.VariableDeclaration")){
				
				varCount++ ; 
			}
			else if(ME.getChildren().size()>0)
				varCount += calcVarible(ME.getChildren());
		}
		return varCount ; 
	}

	public static int calcOperatorVarSize(List<ModuleElement> MEList){
		int sizeCount = 0 ;
		for(int i=0 ; i<MEList.size(); i++)
		{
			ModuleElement ME = MEList.get(i) ;
//			System.out.println("\n"+ME.getClass().getName().toString()) ; 
			if(ME.getClass().getName().contains("Variable") || 
					ME.getClass().getName().contains("Assignment")|| ME.getClass().getName().contains("Operator"))
			{
//				System.out.println("\n"+ME.getClass().getName().toString()) ;
				sizeCount++ ; 
			}
			if(ME.getClass().getName().contains("FirstOrderOperation")){
				FirstOrderOperationCallExpression m = (FirstOrderOperationCallExpression) ME ; 
//				System.out.println(m.getNameExpression().getName()) ;
				sizeCount++ ;
				continue ; 
			}
			else if(ME.getClass().getName().contains("OperationCall")){
//				System.out.print("\n"+ME.getClass().getName().toString()) ;
				OperationCallExpression m = (OperationCallExpression)ME ;
				
//				System.out.println(" ** " + m.getOperationName()) ;	
				if(!OpList.contains(m.getOperationName()))
					sizeCount++ ; 
			}
			if(ME.getChildren().size()>0)
				sizeCount += calcOperatorVarSize(ME.getChildren());
		}
		return sizeCount ; 
	}
	
	public static int calcOpCall(List<ModuleElement> MEList){
		int opCallCount = 0 ;
		for(int i=0 ; i<MEList.size(); i++)
		{
			ModuleElement ME = MEList.get(i) ;
//			System.out.println("\n"+ME.getClass().getName().toString()) ; 
			if(ME.getClass().getName().contains("EquivalentAssignment"))
				opCallCount++ ; 
			else if(ME.getClass().getName().contains("FirstOrderOperation")){
				opCallCount++ ;
				continue ;
			}else if(ME.getClass().getName().contains("Operation")){
//				System.out.print("\n"+ME.getClass().getName().toString()) ;
				OperationCallExpression m = (OperationCallExpression)ME ;
//				System.out.println(" ** " + m.getOperationName()) ;	
				if(OpList.contains(m.getOperationName()))
					opCallCount++ ; 	
			}
			if(ME.getChildren().size()>0)
				opCallCount += calcOpCall(ME.getChildren());
		}
		return opCallCount ; 
	}

	public static int calcLogicalExp(List<ModuleElement> MEList){
		int logicalExpCount = 0 ;
		for(int i=0 ; i<MEList.size(); i++)
		{
			ModuleElement ME = MEList.get(i) ;
//			System.out.println("\n"+ME.getClass().getName().toString()) ; 
			if(ME.getClass().getName().contains("Operator"))
				logicalExpCount++ ; 
			else if(ME.getClass().getName().contains("Operation")){
//				System.out.print("\n"+ME.getClass().getName().toString()) ;
				OperationCallExpression m = (OperationCallExpression)ME ;
//				System.out.println(" ** " + m.getOperationName()) ;	
				if(!OpList.contains(m.getOperationName()))
					logicalExpCount++ ; 	
			}
			if(ME.getChildren().size()>0)
				logicalExpCount += calcLogicalExp(ME.getChildren());
		}
		return logicalExpCount ; 
	}
	
	public static int calcTotalSize(List<ModuleElement> MEList){
		int opCallCount = 0 ;
		for(int i=0 ; i<MEList.size(); i++)
		{
			ModuleElement ME = MEList.get(i) ;
			if(ME.getClass().getName().contains("Operation") || ME.getClass().getName().contains("Operator") 
					|| ME.getClass().getName().contains("Variable") || ME.getClass().getName().contains("Assignment")){
//				System.out.print("\n"+ME.getClass().getName().toString()) ;
				opCallCount++ ; 	
			}
			if(ME.getChildren().size()>0)
				opCallCount += calcTotalSize(ME.getChildren());
		}
		return opCallCount ; 
	}
	
	public static int calcDuplication(){
		int totalDC = 0 ; 
		duplicatedList = new ArrayList<List<ModuleElement>>() ; 
		int index=0 ; 
		while(index<allDecomposedLists.size()){
			if(allDecomposedLists.get(index).size()>= 10){
				List<ModuleElement> meList = allDecomposedLists.get(index) ;
				
				//if j==index => begin=0&&end=9 => checking start=10 => change meList1
				
				for(int j=0; j<allDecomposedLists.size(); j++){
					if(j==index)
						continue ;
					int itBegin = 0 ;
					int itEnd = 9 ; 
					while(itEnd < meList.size()){
						List<ModuleElement> meList1 = allDecomposedLists.get(j) ;						
						List<ModuleElement> meList2 = new ArrayList<ModuleElement>() ;
						for(int l=itBegin; l<=itEnd; l++)
							meList2.add(meList.get(l)) ;
						List<ModuleElement> tempList = new ArrayList<ModuleElement>() ; 
						int tempDC = 0 ; 

						int localDC = isContain(meList1, meList2) ; 
						while(localDC > 0){
//							tempList = meList2 ;
							tempList.clear(); 
							for(int l=0; l<meList2.size(); l++)
								tempList.add(meList2.get(l));							
							tempDC = localDC ; 
							int next = itBegin + meList2.size() ; 
							if(next < meList.size()){
								meList2.add(meList.get(next)) ;
								localDC = isContain(meList1, meList2) ;
							}else{
								break ; 
							}
						}
						if(tempList.size()>0){
							boolean addFlag = true ; 
							for(int k=0; k<duplicatedList.size(); k++){
								if(duplicatedList.get(k).size()==tempList.size()){
									if(isContain(duplicatedList.get(k), tempList) > 0){
										addFlag = false ; 
										break ;
									}
								}
							}
							if(addFlag)
								duplicatedList.add(tempList) ; 
							totalDC += tempDC ;
							
							itBegin += tempList.size() ;
							itEnd += tempList.size() ;
						}else
						{
							itBegin++ ; 
							itEnd++ ;
						}
					}
				}			
			}
			index++ ; 
		}
		return totalDC ;  
	}
		
	// Check meList1 contain meList2
	public static int isContain(List<ModuleElement> meList1, List<ModuleElement> meList2){ 
		int dupCount = 0 ; 
		if(meList1.size()<meList2.size())
			return dupCount ;
				
		int meListIt = 0 ; 
		for(int i=0; i<meList1.size() && meListIt<meList2.size(); i++)
		{
			if(meList1.get(i).getClass().getName().equals(meList2.get(meListIt).getClass().getName())){
				if(meList1.get(i).getClass().getName().contains("NameExpression")){	
					NameExpression ne1 = (NameExpression)meList1.get(i) ;
					NameExpression ne2 = (NameExpression)meList2.get(meListIt) ;
					String name1 = ne1.getName() ;
					String name2 = ne2.getName() ; 
					if(name1.equals(name2)){
						meListIt++ ;
						if(meListIt == meList2.size())
						{
							dupCount++ ;
							meListIt = 0 ; 
						}
					}
					else{
						meListIt = 0 ; 
						if(meList1.size()-i-1 < meList2.size())
							break ;
					}
				}else if(meList1.get(i).getClass().getName().contains("TypeExpression")){
					TypeExpression ne1 = (TypeExpression)meList1.get(i) ;
					TypeExpression ne2 = (TypeExpression)meList2.get(meListIt) ;
					String name1 = ne1.getName() ;
					String name2 = ne2.getName() ; 
					if(name1.equals(name2)){
						meListIt++ ;
						if(meListIt == meList2.size())
						{
							dupCount++ ;
							meListIt = 0 ; 
						}
					}
					else{
						meListIt = 0 ; 
						if(meList1.size()-i-1 < meList2.size())
							break ;
					}
				}
				else{
					meListIt++ ;
					if(meListIt == meList2.size())
					{
						dupCount++ ;
						meListIt = 0 ; 
					}
				}
			}
			else{
				meListIt = 0 ;  
				if(meList1.size()-i-1 < meList2.size())
					break ;
			}
		}
		return dupCount ; 
	}
	
	public static List<ModuleElement> decompose(ModuleElement me){
		List<ModuleElement> meList = new ArrayList<ModuleElement>() ;
		for(int i=0; i<me.getChildren().size(); i++){
			meList.add(me.getChildren().get(i)) ; 
			if(me.getChildren().get(i).getChildren().size()>0){
				meList.addAll(decompose(me.getChildren().get(i))); 
			}
		}
		return meList ; 
	}
	
	public static List<String> calculateCall(List<ModuleElement> MEList, String ruleName, List<Parameter> listParameter){
		List<String> cg = new ArrayList<String>();
		cg.add(ruleName) ; 
		
		for(int i=0; i<MEList.size(); i++){
			if(MEList.get(i).getClass().getName().contains("FirstOrderOperation")){
				continue ; 
			}else if(MEList.get(i).getClass().getName().contains("Operation")){
				OperationCallExpression m = (OperationCallExpression)MEList.get(i) ;
				if(m.getOperationName().equals("equivalent") || m.getOperationName().equals("equivalents")){
					List<String> elList = new ArrayList<String>() ; 
					int index = i ; 
					while(true){
						if(MEList.get(index).getClass().getName().contains("NameExpression")){
							NameExpression ne = (NameExpression)MEList.get(index) ;
							String name = ne.getName() ;
							if(name.equals("equivalent") || name.equals("equivalents")){
								break ;
							}else{
								elList.add(name) ;
							}
						}
						index++ ; 
					}
					//  Convert name of first variable in equivalent call to type of it
					String typeName = detectBaseType(MEList, listParameter, elList.get(0), i) ; 
					elList.set(0, typeName) ;

					if(elList.get(0).equals("")){
						continue ; 
					}else if(elList.get(0).equals("Any"))
					{
						if(elList.size()==1){
							if(!cg.contains("Any"))
								cg.add("Any") ;
						}else
							cg.addAll(identifyMMClassforAny(elList)) ;
					}else{
						cg.addAll(identifyMMClass(elList)) ;
					}
				}
			}
			else if(MEList.get(i).getClass().getName().contains("EquivalentAssignmentStatement")){
				List<String> elList = new ArrayList<String>() ; 
				if(MEList.get(i).getChildren().get(1).getClass().getName().contains("NameExpression")){
					NameExpression ne = (NameExpression)MEList.get(i).getChildren().get(1) ;
					String name = ne.getName() ;
					elList.add(name) ; 
				}
				else{
					List<ModuleElement> equiList = decompose(MEList.get(i).getChildren().get(1)) ;
					for(int j=0; j<equiList.size(); j++){
						if(equiList.get(j).getClass().getName().contains("NameExpression")){
							NameExpression ne = (NameExpression)equiList.get(j) ;
							String name = ne.getName() ;
							elList.add(name) ; 
						}
					}
				}
				// Convert name of first variable in equivalent call to type of it 
				String typeName = detectBaseType(MEList, listParameter, elList.get(0), i) ; 
				elList.set(0, typeName) ;

				if(elList.get(0).equals("")){
					continue ; 
				}else if(elList.get(0).equals("Any"))
				{
					if(elList.size()==1){
						if(!cg.contains("Any"))
							cg.add("Any") ;
					}else
						cg.addAll(identifyMMClassforAny(elList)) ;
				}else{
					cg.addAll(identifyMMClass(elList)) ;
				}
			}
		}
		
		return cg ;  
	}
	
	public static String detectBaseType(List<ModuleElement> MEList, List<Parameter> listofRuleParameter, String firstVar, int index){
		String typeName = ""; 
		
		boolean flag = true ; 
		for(int j=0; j<listofRuleParameter.size(); j++){
			NameExpression p = (NameExpression) listofRuleParameter.get(j).getChildren().get(0) ;
			if(firstVar.equals(p.getName())){
				TypeExpression t = (TypeExpression) listofRuleParameter.get(j).getChildren().get(1) ;
				typeName = t.getName() ;
				flag = false ; 
				break ; 
			}
		}
		if(flag){
			for(int k=0; k<index; k++){
				ModuleElement ME = MEList.get(k) ;
				if(ME.getClass().getName().equals("org.eclipse.epsilon.eol.dom.VariableDeclaration")){
					NameExpression p = (NameExpression) ME.getChildren().get(0) ;
					if(firstVar.equals(p.getName())){
						TypeExpression t = (TypeExpression) ME.getChildren().get(1) ;
						typeName = t.getName() ;
						flag = false ; 
						break ; 
					}
				}else if(ME.getClass().getName().equals("org.eclipse.epsilon.eol.dom.ForStatement")){
					Parameter p = (Parameter) ME.getChildren().get(0) ;
					if(p.getName().equals(firstVar)){
						int indexP = k+1 ; 
						while(!MEList.get(indexP).getClass().getName().contains("NameExpression")){
							indexP++; 
						}
						NameExpression ne = (NameExpression) MEList.get(indexP) ;
						if(firstVar.equals(ne.getName())){
							List<String> forParameterType = new ArrayList<String>() ; 
							indexP++ ; 
							while(!MEList.get(indexP).getClass().getName().equals("org.eclipse.epsilon.eol.dom.StatementBlock")){
								if(MEList.get(indexP).getClass().getName().contains("NameExpression")){
									NameExpression nameExp = (NameExpression) MEList.get(indexP) ;
									forParameterType.add(nameExp.getName()) ;
								}
								indexP++ ; 
							}
							typeName = detectBaseType(MEList, listofRuleParameter, forParameterType.get(0), indexP) ;
							break ; 
						}
					}
				}
			}
		}
		
		return typeName ; 
	}
	
	public static List<String> identifyMMClass(List<String> equiList){
		List<String> mmClasses = new ArrayList<String>() ; 
		
		String [] first = equiList.get(0).split("!") ;
		String metamodelName = first[0] ;
		String firstClassName = first[1] ;
		EClass finalClass = null ;
		
		EList<EClassifier> classifierList = metapackage.getEClassifiers() ; 
	    for(int i=0; i<classifierList.size(); i++){
	    	EClass eClass = null ;     	
	    	if(classifierList.get(i).getClass().getName().contains("EClassImpl")){
	    		eClass = (EClass) classifierList.get(i) ;
	    		if(firstClassName.equals(eClass.getName())){
		    		finalClass = eClass ;
		    		break ;
	    		}
	    	}
	    }
		
	    boolean flag = false ; 
	    for(int i=1; i<equiList.size(); i++){
	    	if(finalClass!=null){
	    		flag = true ; 
		    	EList<EReference> ref = finalClass.getEAllReferences() ;
		    	for(int j=0; j<ref.size(); j++){
		    		if(ref.get(j).getName().equals(equiList.get(i))){
		    			finalClass = (EClass) ref.get(j).getEType() ;
		    			break ;
		    		}
		    	}
	    	}
	    }
	    
	    if(flag)
	    	mmClasses.add(metamodelName+"!"+finalClass.getName()) ;
	    
	    // add Generalized classes
	    for(int i=0; i<classifierList.size(); i++){
	    	EClass eClass = null ; 	    	
	    	if(classifierList.get(i).getClass().getName().contains("EClassImpl")){
	    		eClass = (EClass) classifierList.get(i) ;
    			if(finalClass.isSuperTypeOf(eClass) && !mmClasses.contains(metamodelName+"!"+eClass.getName())){
    				mmClasses.add(metamodelName+"!"+eClass.getName()) ;
    			}
	    	}
	    }
	    
		return mmClasses ;
	}
	
	public static List<String> identifyMMClassforAny(List<String> equiList){
		List<String> mmClasses = new ArrayList<String>() ; 

		EClass finalClass = null ;
		String metamodelName = metapackage.getName() ;
		
		EList<EClassifier> classifierList = metapackage.getEClassifiers() ; 
	    for(int l=0; l<classifierList.size(); l++){
	    	if(classifierList.get(l).getClass().getName().contains("EClassImpl"))
	    		finalClass = (EClass) classifierList.get(l) ;
	    	else
	    		continue ;
	    	
		    for(int i=1; i<equiList.size(); i++){
		    	EList<EReference> ref = finalClass.getEAllReferences() ;
		    	for(int j=0; j<ref.size(); j++){
		    		if(ref.get(j).getName().equals(equiList.get(i))){
		    			finalClass = (EClass) ref.get(j).getEType() ;
		    			break ;
		    		}
		    	}
		    }
		    
		    if(!mmClasses.contains(metamodelName+"!"+finalClass.getName()))
		    	mmClasses.add(metamodelName+"!"+finalClass.getName()) ;
		    
		    // add Generalized classes
		    for(int i=0; i<classifierList.size(); i++){
		    	EClass eClass = null ; 	    	
		    	if(classifierList.get(i).getClass().getName().contains("EClassImpl")){
		    		eClass = (EClass) classifierList.get(i) ;
	    			if(finalClass.isSuperTypeOf(eClass) && !mmClasses.contains(metamodelName+"!"+eClass.getName())){
	    				mmClasses.add(metamodelName+"!"+eClass.getName()) ;
	    			}
		    	}
		    }
		    
	    }	    
		return mmClasses ;
	}
	
	public static void createCallGraph(int cgRow, List<String> seq){
		if(ruleCallGraphs.get(cgRow).size()==1){
			//Occur Cycle and Finished 
//			if(seq.contains(ruleCallGraphs.get(cgRow).get(0))){
//				seq.add(ruleCallGraphs.get(cgRow).get(0)) ;
//				if(seq.size() > biggestCycle.size()){
//					biggestCycle.clear();
//					biggestCycle.addAll(seq) ;
//				}
//				return ; 
//			}
//			// Add to Seq and check for biggest seq
//			else{
				seq.add(ruleCallGraphs.get(cgRow).get(0)) ;
				if(seq.size() > biggestSeq.size()){
					biggestSeq.clear();
					biggestSeq.addAll(seq) ;
				}
				return ; 
//			}
			
		} else{
			//Occur Cycle and Finished 
			if(seq.contains(ruleCallGraphs.get(cgRow).get(0))){
				seq.add(ruleCallGraphs.get(cgRow).get(0)) ;
				if(seq.size() > biggestCycle.size()){
					biggestCycle.clear();
					biggestCycle.addAll(seq) ;
				}
				return ; 
			}
			
			seq.add(ruleCallGraphs.get(cgRow).get(0)) ;
			for(int i=1; i<ruleCallGraphs.get(cgRow).size(); i++){
				
				List<String> tempSeq = new ArrayList<String>() ; 
				tempSeq.addAll(seq) ;
				
				int cgColumn = 0 ; 
				for(int j=0; j<ruleCallGraphs.size(); j++){
					if(ruleCallGraphs.get(j).get(0).equals(ruleCallGraphs.get(cgRow).get(i))){
						cgColumn = j ; 
						break ;
					}
				}
				
				createCallGraph(cgColumn, tempSeq);
			}
		}	
	}

}
