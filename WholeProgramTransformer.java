package pta;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import soot.Value;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Local;
import soot.ValueBox;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.StaticFieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ArrayRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.jimple.toolkits.callgraph.Units;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {
	
	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		
		CallGraph cg = Scene.v().getCallGraph();

		TreeMap<Integer, Value> queries = new TreeMap<Integer, Value>();
		Anderson anderson = new Anderson(); 
		
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
		Map<String, Value> staticptrs = new HashMap<String, Value>(); // This is used to remove duplicate static pointers
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			Map<String, Value> instanceptrs = new HashMap<String, Value>(); // This is used to remove duplicate instance pointers
			if (sm.toString().contains("FieldSensitivity")) {
				// System.out.println(sm);
				int allocId = 0;
				List<Value> inspect = new ArrayList<Value>(); // debug
				if (sm.hasActiveBody()) {
					for (Unit u : sm.getActiveBody().getUnits()) {
						// System.out.println("Statement: " + u);
						// System.out.println(u.getClass());
						if (u instanceof InvokeStmt) {
							InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
							if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
								allocId = ((IntConstant)ie.getArgs().get(0)).value;
							}
							if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
								Value v = ie.getArgs().get(1);
								int id = ((IntConstant)ie.getArgs().get(0)).value;
								queries.put(id, (Value)v);
							}
						}
						if (u instanceof DefinitionStmt) {
							DefinitionStmt du = (DefinitionStmt)u;
							Value l = du.getLeftOp(); String sl = l.toString();
							Value r = du.getRightOp(); String sr = r.toString();
							// if (sm.toString().contains("Hello")) {  // debug
							//     System.out.println("Unit: " + u + ", Type: " + u.getClass()); // debug
							//     System.out.println("LeftType: " + l.getClass() + // debug
							//                        ", RightType: " + r.getClass()); // debug
							// } // debug
							if (r instanceof NewExpr) {
								if (allocId != 0)
								  System.out.println("Alloc: " + allocId + ", Lhs: " + l);
								anderson.addNewConstraint(allocId, l);
							} else if (l instanceof Local && r instanceof Local) {
								anderson.addAssignConstraint(r, l);
							} else if ((l instanceof Local && r instanceof InstanceFieldRef) ||
							           (r instanceof Local && l instanceof InstanceFieldRef)) {
								// Remove duplicate pointers
								if (!instanceptrs.containsKey(sl)) instanceptrs.put(sl, l);
								else l = instanceptrs.get(sl);
								if (!instanceptrs.containsKey(sr)) instanceptrs.put(sr, r);
								else r = instanceptrs.get(sr);
								anderson.addAssignConstraint(r, l);
							} else if ((l instanceof Local && r instanceof StaticFieldRef) ||
							           (r instanceof Local && l instanceof StaticFieldRef)) {
								// Remove duplicate pointers
								if (!staticptrs.containsKey(sl)) staticptrs.put(sl, l);
								else l = staticptrs.get(sl);
								if (!staticptrs.containsKey(sr)) staticptrs.put(sr, r);
								else r = staticptrs.get(sr);
								anderson.addAssignConstraint(r, l);
							} else if (l instanceof Local && r instanceof ArrayRef) {
								// TODO: Global/Local
								sr = ((ArrayRef)r).getBase().toString() + ".[]";
								if (!instanceptrs.containsKey(sr)) instanceptrs.put(sr, r);
								else r = instanceptrs.get(sr);
								anderson.addAssignConstraint(r, l);
							} else if (r instanceof Local && l instanceof ArrayRef) {
								sl = ((ArrayRef)l).getBase().toString() + ".[]";
								if (!instanceptrs.containsKey(sl)) instanceptrs.put(sl, l);
								else l = instanceptrs.get(sl);
								anderson.addAssignConstraint(r, l);
							}
						}
						if (u instanceof IdentityStmt){
							
							Value leftOp = ((IdentityStmt)u).getLeftOp();
							int indexOfParameter = getIndexOfParameter(((IdentityStmt)u).getRightOp().toString());
							
							if(indexOfParameter <= -2){
								continue;
							}

							Iterator sources = new Units(cg.edgesInto(sm));
							
							while(sources.hasNext()){
								Stmt src = (Stmt)sources.next();
								if (src == null) continue;
								if (!src.containsInvokeExpr()) continue;
								if (indexOfParameter == -1){
									if(src.getInvokeExpr() instanceof InstanceInvokeExpr){
										Value rightOp = ((InstanceInvokeExpr)(src.getInvokeExpr())).getBase();
										anderson.addAssignConstraint(rightOp, leftOp);
										anderson.addAssignConstraint(leftOp, rightOp);
										if (sm.toString().contains("FieldSensitivity")) { //debug
											   System.out.println("Unit: " + u); //debug
											   System.out.println("Possible source: " + src);
											   System.out.println("InvokeExpr @this: " + ((InstanceInvokeExpr)(src.getInvokeExpr())).getBase()); //debug
										} //debug
									}
								} else {
									Value rightOp = src.getInvokeExpr().getArg(indexOfParameter);
									anderson.addAssignConstraint(rightOp, leftOp);
									anderson.addAssignConstraint(leftOp, rightOp);
								    if (sm.toString().contains("FieldSensitivity")) { //debug
								    	   System.out.println("Unit: " + u); //debug
								    	   System.out.println("Possible source: " + src);
								    	   System.out.println("Parameter" + indexOfParameter + ": " + src.getInvokeExpr().getArg(indexOfParameter)); //debug
								    	   System.out.println("LeftOp: " + leftOp); //debug
								    } //debug
								}
							}
						}
						if (u instanceof ReturnStmt) {
							Value op = ((ReturnStmt) u).getOp();
							Iterator sources = new Units(cg.edgesInto(sm));
							while (sources.hasNext()) {
								Stmt src = (Stmt) sources.next();
								if (src == null) {
									continue;
								}
								if (!src.containsInvokeExpr()) {
									continue;
								}

								System.out.println("Src: " + src + "\n" + "Class: " + src.getClass());

								if (src instanceof AssignStmt) {
									anderson.addAssignConstraint(op, ((AssignStmt)src).getLeftOp());
								}
							}
						}
					}
				}
			}
		}
		
		anderson.run();
		String answer = "";
		for (Entry<Integer, Value> q : queries.entrySet()) {
			TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			if (result != null) {
				for (Integer i : result) {
					answer += " " + i;
				}
			}
			answer += "\n";
		}
		AnswerPrinter.printAnswer(answer);
		
	}

	protected int getIndexOfParameter(String expression){
		if((expression.substring(0, 5)).contains("@this")){
			return -1;
		}
		if(!((expression.substring(0, 10)).contains("@parameter"))){
			return -2;
		}
		int indexOfColon = expression.indexOf(":");
		assert(indexOfColon != -1);
		String index = expression.substring(10, indexOfColon);
		return Integer.parseInt(index);
	}

}
