package dev.navids.soottutorial.cfa;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import soot.Body;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class CFGInterProcedural {

    public static void main(String[] args) {
        String pathToJar = "/Users/kishanthan/Work/research/clones/joern/example/x42/java/X42.jar";

        String sourceIdentifierName = "args";
        String sinkMethodName = "println";

        // Set up the classpath
        String sootClasspath = pathToJar + ":" + Scene.v().defaultClassPath();
        Scene.v().setSootClassPath(sootClasspath);

        // Set up the options
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Collections.singletonList(pathToJar));

        // Enable Spark call graph
        Options.v().setPhaseOption("cg.spark", "on");

        // Load the classes
        Scene.v().loadNecessaryClasses();

        // Build the call graph
        PackManager.v().runPacks();

        // Get the call graph
        CallGraph cg = Scene.v().getCallGraph();

        // Find the source identifier
        Local sourceIdentifier = null;
        SootMethod containingMethod = null;
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                Body body = sm.retrieveActiveBody();
                for (Local local : body.getLocals()) {
                    if (local.getName().equals(sourceIdentifierName)) {
                        sourceIdentifier = local;
                        containingMethod = sm;
                        break;
                    }
                }
            }
            if (sourceIdentifier != null) {
                break;
            }
        }

        if (sourceIdentifier == null) {
            System.out.println("Source identifier not found");
            return;
        }

        // Perform reachability analysis from source identifier to sink method
        boolean isReachable = isReachableFromSourceToSink(cg, containingMethod, sinkMethodName);

        System.out.println("Is sink method reachable from source identifier? " + isReachable);
    }

    private static boolean isReachableFromSourceToSink(CallGraph cg, SootMethod containingMethod, String sinkMethod) {
        Set<SootMethod> visited = new HashSet<>();
        Queue<SootMethod> worklist = new LinkedList<>();

        // Start from the method that contains the source identifier
        worklist.add(containingMethod);

        while (!worklist.isEmpty()) {
            SootMethod currentMethod = worklist.poll();

            if (visited.contains(currentMethod)) {
                continue;
            }

            visited.add(currentMethod);

            // Check if we have reached the sink method
            if (currentMethod.getName().equals(sinkMethod)) {
                return true;
            }

//            // Add successors to the worklist
//            Iterator<Edge> edges = cg.edgesOutOf(currentMethod);
//            while (edges.hasNext()) {
//                Edge edge = edges.next();
//                worklist.add(edge.tgt());
//            }

            // Add methods in the control flow graph to the worklist
            Body body = currentMethod.retrieveActiveBody();
            UnitGraph cfg = new ExceptionalUnitGraph(body);
            List<Unit> heads = cfg.getHeads();

            for (Unit unit : cfg) {
                if (unit instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) unit;
                    SootMethod invokedMethod = invokeStmt.getInvokeExpr().getMethod();
                    worklist.add(invokedMethod);
                }
            }
        }

        return false;
    }

    void analyzeSuccessors(UnitGraph cfg, Unit unit, String sinkMethod) {
        List<Unit> successors = cfg.getSuccsOf(unit);
        for (Unit successor : successors) {
            if (successor instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) successor;
                SootMethod invokedMethod = invokeStmt.getInvokeExpr().getMethod();
            }
            analyzeSuccessors(cfg, successor, sinkMethod);
        }
    }
}
