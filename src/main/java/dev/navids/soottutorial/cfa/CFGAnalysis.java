package dev.navids.soottutorial.cfa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class CFGAnalysis {

    public static void main(String[] args) {
        String pathToJar = "/Users/kishanthan/Work/research/clones/joern/example/x42/java/X42.jar";
//        // Set Soot's internal options
//        Options.v().set_output_format(Options.output_format_jimple);
//        Options.v().set_allow_phantom_refs(true);
//        Options.v().set_process_dir(Collections.singletonList(pathToJar));
//        Options.v().set_whole_program(true);
//        Scene.v().loadNecessaryClasses();
//        PackManager.v().runPacks();
////        Options.v().set_output_format(Options.output_format_jimple);
//
//        // Load and analyze the classes in the JAR file
////        Scene.v().loadNecessaryClasses();
//
//        // Define your source and sink methods
//        Set<String> sourceMethods = new HashSet<>();
//        sourceMethods.add("main");
////        sourceMethods.add("<source_method_2>");
//        // ...
//
//        Set<String> sinkMethods = new HashSet<>();
//        sinkMethods.add("foo");
////        sinkMethods.add("<sink_method_2>");
//
//        // Create a Call Graph to identify entry points
//        CallGraph cg = Scene.v().getCallGraph();
//
//        // Find all entry points (start nodes) of the control flow graph
//        List<Unit> startNodes = new ArrayList<>();
//        List<Unit> endNodes = new ArrayList<>();
////        for (SootMethod sootMethod : Scene.v().getEntryPoints()) {
////            SootMethod entryPointMethod = sootMethod.method();
////            if (cg.edgesOutOf(entryPointMethod).hasNext()) {
////                startNodes.add(entryPointMethod.getActiveBody().getUnits().getFirst());
////            }
////        }
//
//        for (SootClass sc : Scene.v().getApplicationClasses()) {
//            for (SootMethod sm : sc.getMethods()) {
////                Body b = sm.retrieveActiveBody();
//
//                // Create an ExceptionalUnitGraph for the method
////                UnitGraph cfg = new ExceptionalUnitGraph(b);
//
//                // Check if the method is a source method
//                if (sourceMethods.contains(sm.getName())) {
//                    // Perform your analysis to find reachable sink methods
//                    for (Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(sm)); targets.hasNext(); ) {
//                        SootMethod target = (SootMethod) targets.next();
//                        if (sinkMethods.contains(target.getName())) {
//                            System.out.println("Sink method " + target + " is reachable from source method " + sm.getSignature());
//                        }
//                    }
//                }
//
//                // Find all exit points (end nodes) of the control flow graph
////                for (Unit unit : cfg) {
////                    if (cfg.getSuccsOf(unit).isEmpty()) {
////                        endNodes.add(unit);
////                    }
////                }
//            }
//        }

        // Build the control flow graph
//        UnitGraph cfg = new ExceptionalUnitGraph(startNodes);

//        // Find all exit points (end nodes) of the control flow graph
//        List<Unit> endNodes = new ArrayList<>();
//        for (Unit unit : cfg) {
//            if (cfg.getSuccsOf(unit).isEmpty()) {
//                endNodes.add(unit);
//            }
//        }

//        System.out.println("Start nodes (entry points):");
//        for (Unit startNode : startNodes) {
//            System.out.println(startNode);
//        }
//
//        System.out.println("\nEnd nodes (exit points):");
//        for (Unit endNode : endNodes) {
//            System.out.println(endNode);
//        }

        String sourceMethodName = "main";
        String sinkMethodName = "println";

        // Set up the classpath
        String sootClasspath = pathToJar + ":" + Scene.v().defaultClassPath();
        Scene.v().setSootClassPath(sootClasspath);

        // Set up the options
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

        // Find the source method
        SootMethod sourceMethod = null;
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.declaresMethodByName(sourceMethodName)) {
                sourceMethod = sc.getMethodByName(sourceMethodName);
                break;
            }
        }

        if (sourceMethod == null) {
            System.out.println("Source method not found");
            return;
        }

        // Perform reachability analysis from source to sink
        boolean isReachable = isReachableFromSourceToSink(cg, sourceMethod, sinkMethodName);

        System.out.println("Is sink method reachable from source method? " + isReachable);
    }


    private static boolean isReachableFromSourceToSink(CallGraph cg, SootMethod sourceMethod, String sinkMethod) {
        Set<SootMethod> visited = new HashSet<>();
        Queue<SootMethod> worklist = new LinkedList<>();

        worklist.add(sourceMethod);

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

            // Add successors to the worklist
            Iterator<Edge> edges = cg.edgesOutOf(currentMethod);
            while (edges.hasNext()) {
                Edge edge = edges.next();
                worklist.add(edge.tgt());
            }
        }

        return false;
    }

    private static void performControlFlowAnalysis(SootMethod method) {
        Body body = method.retrieveActiveBody();

        // Create a CFG
        UnitGraph cfg = new ExceptionalUnitGraph(body);

        for (Unit unit : cfg) {
            System.out.println(unit);

            // Print the predecessors and successors of each statement
            List<Unit> preds = cfg.getPredsOf(unit);
            List<Unit> succs = cfg.getSuccsOf(unit);

            System.out.println("Predecessors: " + preds);
            System.out.println("Successors: " + succs);
        }
    }
}
