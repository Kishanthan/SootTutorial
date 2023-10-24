package dev.navids.soottutorial.cfa;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import soot.Body;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.cfgcmd.CFGGraphType;

public class ControlFlowAnalysis {

    public static void main(String[] args) {
        String jarPath = "/Users/kishanthan/Work/research/clones/SootTutorial/demo/HelloSoot";
        String sourceMethodName = "fizzBuzz";
        String sinkMethodName = "printFizzBuzz";

        // Initialize Soot
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_whole_program(true);
        Scene.v().loadNecessaryClasses();
        SootClass targetClass = Scene.v().loadClassAndSupport("FizzBuzz");
//        Scene.v().setMainClass(targetClass);

        // Find the source and sink methods
        SootMethod sourceMethod = targetClass.getMethodByName(sourceMethodName);
//        SootMethod sinkMethod = targetClass.getMethodByName(sinkMethodName);

        // Create a CFG for the source method
        Body sourceBody = sourceMethod.retrieveActiveBody();
        UnitGraph sourceCFG = new ExceptionalUnitGraph(sourceBody);

        // Perform control flow analysis from source to sink
        boolean isReachable = isReachableFromSourceToSink(sourceCFG, sourceBody.getUnits().getFirst(), sinkMethodName);

        System.out.println("Is sink method reachable from source method? " + isReachable);

        CFGGraphType graphtype = CFGGraphType.getGraphType("BriefUnitGraph");
        DirectedGraph<Unit> graph = graphtype.buildGraph(sourceBody);
        System.out.println(graph);
    }

    private static boolean isReachableFromSourceToSink(UnitGraph cfg, Unit currentUnit, String sinkMethod) {
        Set<Unit> visited = new HashSet<>();
        Queue<Unit> worklist = new LinkedList<>();

        worklist.add(currentUnit);

        while (!worklist.isEmpty()) {
            Unit unit = worklist.poll();

            if (visited.contains(unit)) {
                continue;
            }

            visited.add(unit);

            // Check if we have reached the sink method
            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                if (invokeExpr.getMethod().getName().equals(sinkMethod)) {
                    return true;
                }
            }

            // Add successors to the worklist
            List<Unit> successors = cfg.getSuccsOf(unit);
            worklist.addAll(successors);
        }

        return false;
    }
}