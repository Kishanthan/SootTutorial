package dev.navids.soottutorial.cfa;

import dev.navids.soottutorial.android.AndroidUtil;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class CFGAnalysisTake1 {

    public static void main(String[] args) {
//        String apkPath = "/Users/kishanthan/Work/research/clones/joern/example/MixologyExplorer/BarAppCompose/app/build/outputs/apk/release/app-release-unsigned.apk";
        String apkPath = "/Users/kishanthan/Work/research/clones/joern/example/apks/sipdroid/app-release.apk";
//        String pathToJar = "/Users/kishanthan/Work/research/clones/joern/example/MixologyExplorer/BarAppCompose/app/build/outputs/apk/release/app-release-unsigned-dex2jar.jar";
        String pathToJar = "/Users/kishanthan/Work/research/clones/joern/example/apks/sipdroid/app-release-dex2jar.jar";

        String sourceIdentifierName = "var0";
//        String sourceIdentifierName = "args";
        String sourceMethod = "onCreate";
        String sourceType = "android.os.Bundle";
//        String sourceType = "androidx.compose.runtime.Composer";
//        String sourceType = "androidx.navigation.NavController";
//        String sourceType = "java.lang.String[]";
        String sinkMethodName = "addPreferences";
//        String sinkMethodName = "println";

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
//        Options.v().setPhaseOption("cg.spark", "on");

        // Load the classes
        Scene.v().loadNecessaryClasses();

        // Build the call graph
//        PackManager.v().runPacks();

        // Get the call graph
//        CallGraph cg = Scene.v().getCallGraph();

        String pkgName = AndroidUtil.getPackageName(apkPath);
        // Find the source identifier
        Local sourceIdentifier = null;
        SootMethod containingMethod = null;
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (!sc.getName().contains(pkgName)) {
                continue;
            }
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete()) {
                    continue;
                }

                Body body = sm.retrieveActiveBody();
                for (Local local : body.getLocals()) {
                    if (/*local.getName().equals(sourceIdentifierName) && */ local.getType().toString()
                        .equals(sourceType)) {
                        sourceIdentifier = local;
//                            Body body = containingMethod.retrieveActiveBody();
                        UnitGraph graph = new ExceptionalUnitGraph(body);
                        Set<Unit> cfgPath = new LinkedHashSet<>();
                        for (Unit unit : graph) {
                            // Check if this unit is a source
                            if (isSourceUnit(unit, sourceIdentifier)) {
                                cfgPath.add(unit);
                                // Perform a forward flow analysis from this unit to find sinks
                                Set<Unit> sinks = performForwardFlowAnalysis(graph, unit, sinkMethodName, cfgPath);
                                // Process the sinks
                                processSinks(sinks);
                                cfgPath.forEach(node -> System.out.println("Node '" + node + " -> "));
                                break;
                            }
                        }
                    }
                }
            }
//            if (sourceIdentifier != null) {
//                break;
//            }
        }

        if (sourceIdentifier == null) {
            System.out.println("Source identifier not found");
        }
    }

    // Checks if a unit is a source
    public static boolean isSourceUnit(Unit unit, Local sourceIdentifier) {
        // For example, we consider a unit as a source if it is an assignment statement
        return unit.toString().contains(sourceIdentifier.getName());
    }

    // Performs a forward flow analysis on a graph from a given unit and returns a list of sink units
    public static Set<Unit> performForwardFlowAnalysis(UnitGraph graph, Unit unitOfInterest, String sinkMethodName,
                                                       Set<Unit> cfgPath) {
        Set<Unit> sinks = new LinkedHashSet<>();
        // We use a simple worklist algorithm for the forward flow analysis
        LinkedList<Unit> worklist = new LinkedList<>();
        worklist.add(unitOfInterest);
        while (!worklist.isEmpty()) {
            Unit unit = worklist.removeFirst();
            // If this unit is a sink, add it to the list of sinks
//            if (isSink(unit, sinkMethodName)) {
//                sinks.add(unit);
//            }

            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                if (invokeStmt.toString().contains(sinkMethodName)) {
                    sinks.add(unit);
                } else {
                    SootMethod invokedMethod = invokeStmt.getInvokeExpr().getMethod();
                    if (invokedMethod.isConcrete() && invokedMethod.hasActiveBody()) {
                        UnitGraph invokeMethodGraph = new ExceptionalUnitGraph(invokedMethod.getActiveBody());
                        Unit invokeUnit = findStartUnitOfInvokedMethod(invokeStmt);

                        if (invokeUnit != null) {
                            cfgPath.add(invokeUnit);
                            Set<Unit> sinksFromInvokeMethod = performForwardFlowAnalysis(invokeMethodGraph, invokeUnit,
                                sinkMethodName, cfgPath);
                            //                List<Unit> sinksFromInvokeMethod = performForwardFlowAnalysis(invokeMethodGraph, unit, sinkMethodName);
                            sinks.addAll(sinksFromInvokeMethod);
                        }
                    }
                }
            }
            // Add successors of this unit to the worklist
            for (Unit successor : graph.getSuccsOf(unit)) {
                // Check if this unit is related to the source before adding it to the worklist
                if (isRelatedToSource(successor, unit)) {
                    worklist.add(successor);
                    cfgPath.add(successor);
                }
            }
        }
        return sinks;
    }

    public static Unit findStartUnitOfInvokedMethod(InvokeStmt invokeStmt) {
        // Get the invoked method from the invoke statement
        SootMethod invokedMethod = invokeStmt.getInvokeExpr().getMethod();

        // Check if the invoked method has an active body
        if (invokedMethod.isConcrete()) {
            // Get the active body of the invoked method
            Body invokedBody = invokedMethod.getActiveBody();

            // The start unit of the invoked method is the first unit in its active body
            return invokedBody.getUnits().getFirst();
        }

        return null;
    }

    public static boolean isRelatedToSource(Unit unit, Unit source) {
        return true;
//        if (unit instanceof Stmt) {
//            Stmt stmt = (Stmt) unit;
//            List<ValueBox> defBoxes = source.getDefBoxes();
//            if (source instanceof IfStmt){
//                defBoxes = ((JIfStmt) source).getTargetBox().getUnit().getDefBoxes();
//            }
//
//            for (ValueBox defBox : defBoxes) {
//                // Check if the statement uses the source
//                for (ValueBox useBox : stmt.getUseBoxes()) {
//                    if (useBox.getValue().equivTo(defBox.getValue())){
//                        return true;
//                    }
////                    if (useBox.getValue() instanceof Local) {
////                        Local local = (Local) useBox.getValue();
////                        if (defBox.getValue().equivTo(local)) {
////                            return true;
////                        }
////                    }
//                }
//                // Check if the statement is an assignment where the right-hand side is the source
//                if (stmt instanceof AssignStmt) {
//                    AssignStmt assignStmt = (AssignStmt) stmt;
//                    Value rhs = assignStmt.getRightOp();
//                    if (rhs.equivTo(defBox.getValue())) {
//                        return true;
//                    }
//                    Value lhs = assignStmt.getLeftOp();
//                    if (lhs.equivTo(defBox.getValue())) {
//                        return true;
//                    }
//                }
//                // Check if the statement is an invoke statement where one of the arguments is the source
//                if (stmt.containsInvokeExpr()) {
//                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
//                    for (Value arg : invokeExpr.getArgs()) {
//                        if (arg.equivTo(defBox.getValue())) {
//                            return true;
//                        }
//                    }
//                }
//            }
//        }
//        return false;
    }

    // Checks if a unit is a sink
    public static boolean isSink(Unit unit, String sinkMethodName) {
        // For example, we consider a unit as a sink if it is an invoke statement
        return unit instanceof InvokeStmt && unit.toString().contains(sinkMethodName);
    }

    // Processes the sinks
    public static void processSinks(Set<Unit> sinks) {
        for (Unit sink : sinks) {
            // For example, we simply print the sinks
            System.out.println("Sink : " + sink);
        }
    }

}
