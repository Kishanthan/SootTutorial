package dev.navids.soottutorial.cfa;

import dev.navids.soottutorial.android.AndroidUtil;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class CFGAnalysisTake2 {

    private final static String USER_HOME = System.getProperty("user.home");
    static String apkPath = "/Users/kishanthan/Work/research/clones/joern/example/apks/sipdroid/app-release.apk";
    static String pathToJar = "/Users/kishanthan/Work/research/clones/joern/example/apks/sipdroid/app-release-dex2jar.jar";
    static String androidJar = USER_HOME + "/Library/Android/sdk/platforms";

    public static void main(String[] args) {
        String sinkMethodName = "getAction";
        // Set Soot's internal options
//        Options.v().set_src_prec(Options.src_prec_apk);
//        Options.v().set_process_dir(Collections.singletonList("/Users/kishanthan/Work/research/clones/joern/example/apks/sipdroid/app-release.apk"));
//        Options.v().set_android_jars(androidJar);
//        Options.v().set_whole_program(true);
//        Options.v().set_allow_phantom_refs(true);

        String sootClasspath = pathToJar + ":" + Scene.v().defaultClassPath();
        Scene.v().setSootClassPath(sootClasspath);

        // Set up the options
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Collections.singletonList(pathToJar));

        // Initialize Soot
        Scene.v().loadNecessaryClasses();

        String pkgName = AndroidUtil.getPackageName(apkPath);
        // Iterate over all application classes
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (!sc.getName().contains(pkgName)) {
                continue;
            }
            // Iterate over all methods in the class
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete()) {
                    continue;
                }

//                Body body = sm.retrieveActiveBody();

                String name = sm.getName();
                // Check if the method is a potential user input handler
                if (name.equals("onClick") ||
                    name.equals("onLongClick") ||
                    name.equals("onTouch") ||
                    name.equals("onFocusChange") ||
                    name.equals("onKey") ||
                    name.equals("onMenuItemClick") ||
                    name.equals("onOptionsItemSelected") ||
                    name.equals("onContextItemSelected") ||
                    name.equals("onCreateContextMenu") ||
                    name.startsWith("onCreateDialog") || // includes onCreateDialog and onCreateDialogView
                    name.equals("onTrackballEvent") ||
                    name.equals("onTouchEvent") ||
                    name.equals("onSearchRequested") ||
                    name.startsWith(
                        "onActivityResult")) { // includes onActivityResult and onActivityResultFromFragment

                    // Check if the method is concrete (i.e., it has a body)
                    // Get the method's active body
                    Body b = sm.retrieveActiveBody();

                    // Create a graph of the method's control flow
                    UnitGraph graph = new ExceptionalUnitGraph(b);

                    Set<Unit> cfgPath = new LinkedHashSet<>();

                    // Iterate over each unit in the graph
                    for (Unit unit : graph) {
                        cfgPath.add(unit);
                        // Perform a forward flow analysis from this unit to find sinks
                        Set<Unit> sinks = performForwardFlowAnalysis(graph, unit, sinkMethodName, cfgPath);
                        processSinks(sinks);
                        cfgPath.forEach(node -> System.out.println("Node '" + node + " -> "));
                    }
                }
            }
        }
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

//            Stmt stmt = (Stmt) u;
//
//            // Check if the statement contains an InvokeExpr
//            if (stmt.containsInvokeExpr()) {
//                InvokeExpr invokeExpr = stmt.getInvokeExpr();
//
//                // Check if the invoked method is getAction()
//                if (invokeExpr.getMethod().getName().equals("getAction")) {
//                    System.out.println("Found a path from " + name + " to getAction()");
//                }
//            }

            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                if (invokeStmt.toString().contains(sinkMethodName)) {
                    System.out.println("Found a path from " + unitOfInterest + " to " + sinkMethodName);
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
    }

    public static void processSinks(Set<Unit> sinks) {
        for (Unit sink : sinks) {
            // For example, we simply print the sinks
            System.out.println("Sink : " + sink);
        }
    }
}
