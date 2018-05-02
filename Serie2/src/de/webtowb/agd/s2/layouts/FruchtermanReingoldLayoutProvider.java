package de.webtowb.agd.s2.layouts;

import de.webtwob.adg.s2.layouts.options.FruchtermanReingoldOptions;
import org.eclipse.elk.core.AbstractLayoutProvider;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FruchtermanReingoldLayoutProvider extends AbstractLayoutProvider {

    // used for randomness
    private final Random rand = new Random();
    // used for deterministic randomness
    private final Random detRand = new Random(42);
    private RepulsionEnum repulsionMode = RepulsionEnum.RADIUS2K;
    private ForceEnum forceFunctions = ForceEnum.DEFAULT;
    private InitialLayoutEnum initLayout = InitialLayoutEnum.CIRCLE;
    private double C = 1;
    private int iterations = 50;
    private int earlyStop = 0;
    private double width = 1000;
    private double height = 1000;
    private boolean runParallel = true;
    private CoolingFunctionEnum coolingFunction = CoolingFunctionEnum.QUENCH_AND_SIMMER;

    private double getArea() {
        return width * height;
    }

    private void loadOptions(ElkNode layoutGraph) {
        repulsionMode = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_REPULSION_MODE);
        initLayout = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_INIT_LAYOUT);
        C = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_C_PARAMETER);
        iterations = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_ITERATIONS);
        earlyStop = layoutGraph.getProperty(FruchtermanReingoldOptions.DEBUG_STOP_EARLY);
        width = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_WIDTH);
        height = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_HEIGHT);
        coolingFunction = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_COOLING_FUNCTION);
        detRand.setSeed(layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_SEED));
        forceFunctions = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FORCE_FUNCTIONS);
        runParallel = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_PARALLEL);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void layout(ElkNode layoutGraph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Begin GraphLayout", iterations + 1);

        loadOptions(layoutGraph);

        // k optimal vertex distance
        double k = C * Math.sqrt(getArea() / layoutGraph.getChildren().size());
        List<ElkNode>[][] grid;

        grid = (LinkedList<ElkNode>[][]) new LinkedList<?>[(int) (width / (2 * k)) + 1][(int) (height / (2 * k)) + 1];

        initSetup(layoutGraph, progressMonitor.subTask(1), grid, k);

        /*
         * Neighbors attract divide into grid boxes, gridBoxes = |V|/4 gridbox side length = 2k = 2 sqrt(WL/|V|) all in
         * a 2k radius repulse in a 3x3 of grid boxes around a vertex
         *
         **/

        if (!layoutGraph.getProperty(FruchtermanReingoldOptions.DEBUG_SKIP_LAYOUT)) {
            IElkProgressMonitor subTask;
            int iterationSize;

            double temperature;
            if (!runParallel) {
                iterationSize = layoutGraph.getChildren().size() * 2 + layoutGraph.getContainedEdges().size();

                for (int i = 0; i < iterations - earlyStop; i++) {
                    subTask = progressMonitor.subTask(1);
                    subTask.begin(String.format("Iteration %d", i), iterationSize);

                    temperature = coolingFunction.temperature(layoutGraph, i);
                    serialIteration(layoutGraph, subTask, temperature, grid, k);

                    subTask.done();
                }
            } else {
                ForkJoinPool pool = new ForkJoinPool();
                iterationSize = layoutGraph.getChildren().size() * 4;
                for (int i = 0; i < iterations - earlyStop; i++) {
                    subTask = progressMonitor.subTask(1);
                    subTask.begin(String.format("Iteration %d", i), iterationSize);

                    temperature = coolingFunction.temperature(layoutGraph, i);
                    parallelIteration(layoutGraph, subTask, temperature, grid, k, pool);

                    subTask.done();
                }
                pool.shutdown();
            }
        }

        positionGraph(layoutGraph);

        // TODO edge routing

        progressMonitor.done();
    }

    private void serialIteration(
            ElkNode layoutGraph, IElkProgressMonitor subTask, double temperature, List<ElkNode>[][] grid, double k) {
        layoutGraph.getChildren()
                .forEach(n -> n.getProperty(FruchtermanReingoldOptions.OUTPUTS_DISPLACEMENT_VECTOR).reset());

        calculateRepulsionSerial(layoutGraph, subTask.subTask(layoutGraph.getChildren().size()), grid, k);
        calculateAttractionSerial(layoutGraph, subTask.subTask(layoutGraph.getContainedEdges().size()), k);

        performMovementSerial(layoutGraph, subTask.subTask(layoutGraph.getChildren().size()), temperature);

        calculateGrid(layoutGraph, grid, k);
    }

    private void parallelIteration(
            ElkNode layoutGraph, IElkProgressMonitor subTask, double temperature, List<ElkNode>[][] grid, double k,
            ForkJoinPool pool) {

        IElkProgressMonitor phase1 = subTask.subTask(2 * layoutGraph.getChildren().size());
        IElkProgressMonitor phase2 = subTask.subTask(2 * layoutGraph.getChildren().size());

        phase1.begin("Parallel-Phase1", 2*layoutGraph.getChildren().size());
        layoutGraph.getChildren()
                .parallelStream()
                .map(node -> (Runnable) (() -> parallelPhase1(node, phase1, grid, k)))
                .map(pool::submit) // submit runnable
                .collect(Collectors.toList())
                .forEach(ForkJoinTask::join); // wait for Phase 1 to finish
        phase1.done();

        phase2.begin("Parallel-Phase2", 2*layoutGraph.getChildren().size());
        layoutGraph.getChildren()
                .parallelStream()
                .map(node -> (Runnable) (() -> parallelPhase2(node, phase2, temperature)))
                .map(pool::submit)
                .collect(Collectors.toList())
                .forEach(ForkJoinTask::join);// wait for Phase 2 to finish
        calculateGrid(layoutGraph, grid, k);
        phase2.done();

    }

    private void parallelPhase1(ElkNode current, IElkProgressMonitor subTask, List<ElkNode>[][] grid, double k) {
        calculateRepulsionParallel(current, grid, k);
        subTask.worked(1);
        calculateAttractionParallel(current, k);
        subTask.worked(1);
    }

    private void parallelPhase2(
            ElkNode node, IElkProgressMonitor subTask, double temperature) {
        performMovementParallel(node, temperature);
        subTask.worked(1);
    }

    /**
     * Resized the parent to fit our result graph and centers the result graph in it's parent
     */
    private void positionGraph(ElkNode layoutGraph) {
        // Determine the size the parent should have and center the subgraph

        double maxX = 0, maxY = 0, minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        for (ElkNode node : layoutGraph.getChildren()) {
            if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
                System.err.println("At last one Coordinate of a Node where Nan!");
                continue;
            }
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX() + node.getWidth());
            maxY = Math.max(maxY, node.getY() + node.getHeight());

        }

        double dimX = maxX - minX;
        double dimY = maxY - minY;
        double radius = Math.sqrt(dimX * dimX + dimY * dimY) / 2;

        for (ElkNode node : layoutGraph.getChildren()) {
            node.setLocation(node.getX() - minX + (radius - dimX / 2), node.getY() - minY + (radius - dimY / 2));
        }

        layoutGraph.setDimensions(radius * 2, radius * 2);

    }

    private void initSetup(ElkNode layoutGraph, IElkProgressMonitor subTask, List<ElkNode>[][] grid, double k) {
        subTask.begin("InitPosition", layoutGraph.getChildren().size());

        for (List<ElkNode>[] sub : grid) {
            Arrays.setAll(sub, notUsed -> new LinkedList<ElkNode>());
        }

        switch (initLayout) {
        default: // default to CIRCLE
        case CIRCLE: // Place nodes on a circle around the center of our area
            double radius = Math.min(width, height) / 2.0;
            double angle = Math.PI * 2 / layoutGraph.getChildren().size();
            int count = 0;
            for (ElkNode node : layoutGraph.getChildren()) {
                node.setLocation(Math.sin(angle * count) * radius + width / 2,
                        Math.cos(angle * count) * radius + height / 2);
                count++;
            }
            break;
        case RANDOM_NON_DETERMINISTIC: // place Nodes Randomly
            // might produce two vertices with the same position and same neighbors being stuck together, unlikely
            // though
            for (ElkNode node : layoutGraph.getChildren()) {
                node.setLocation(width / 4 + rand.nextDouble() * width / 2,
                        height / 4 + rand.nextDouble() * height / 2);
            }
            break;
        }

        calculateGrid(layoutGraph, grid, k);

        subTask.done();
    }

    /**
     * Moves each node by their displacement vector, but no more than temperature and not outside of x in [0,width] and
     * y in [0,height]
     */
    private void performMovementSerial(ElkNode layoutGraph, IElkProgressMonitor subTask, double temperature) {
        subTask.begin("Performing Movement", layoutGraph.getChildren().size());
        for (ElkNode node : layoutGraph.getChildren()) {
            performMovementParallel(node, temperature);
            subTask.worked(1);
        }
        subTask.done();
    }

    private void performMovementParallel(ElkNode current, double temperature) {
        KVector disp = current.getProperty(FruchtermanReingoldOptions.OUTPUTS_DISPLACEMENT_VECTOR);
        double displacementDistance = disp.length();

        // limit displacement by temperature
        disp.normalize().scale(Math.min(temperature, displacementDistance));

        // when hitting wall normal to wall
        current.setX(Math.min(width, Math.max(0, current.getX() + disp.x)));
        current.setY(Math.min(height, Math.max(0, current.getY() + disp.y)));
    }

    /**
     * Calculate the repulsion for each edge and adds it to the displacement vector of the nodes
     */
    private void calculateAttractionSerial(
            ElkNode layoutGraph, IElkProgressMonitor subTask, double k) {
        subTask.begin("Calculating Attraction", layoutGraph.getContainedEdges().size());

        for (ElkEdge edge : layoutGraph.getContainedEdges()) {
            for (ElkConnectableShape s1 : edge.getSources()) {
                for (ElkConnectableShape s2 : edge.getTargets()) {
                    // assumption 1 only edges between nodes
                    if (s1 instanceof ElkNode && s2 instanceof ElkNode && !s1.equals(s2)) {
                        KVector disp = calculateAttraction((ElkNode) s1, (ElkNode) s2, k);
                        s1.getProperty(FruchtermanReingoldOptions.OUTPUTS_DISPLACEMENT_VECTOR).add(disp);
                        s2.getProperty(FruchtermanReingoldOptions.OUTPUTS_DISPLACEMENT_VECTOR).sub(disp);
                    }
                }
            }
        }

        subTask.done();
    }

    private void calculateAttractionParallel(final ElkNode current, double k) {
        Stream<ElkConnectableShape> sources = current.getIncomingEdges().stream().flatMap(e -> e.getSources().stream());
        Stream<ElkConnectableShape> targets = current.getOutgoingEdges().stream().flatMap(e -> e.getTargets().stream());
        KVector disp = current.getProperty(
                FruchtermanReingoldOptions.OUTPUTS_DISPLACEMENT_VECTOR);
        Stream.concat(sources, targets)
                .sequential()
                .filter(e -> e instanceof ElkNode)
                .map(e -> (ElkNode) e)
                .map(node -> calculateAttraction(current, node, k))
                .forEach(disp::add);
    }

    @SuppressWarnings("ConstantConditions")
    private KVector calculateAttraction(ElkNode node, ElkNode neighbour, double k) {
        KVector dist = difference(neighbour, node);
        // act as if we have a small offset if we are at distance 0
        if (dist.length() == 0) {
            dist.x = detRand.doubles().map(d -> d - 0.5).filter(d -> d != 0).findFirst().getAsDouble();
            dist.y = detRand.doubles().map(d -> d - 0.5).filter(d -> d != 0).findFirst().getAsDouble();
        }
        double force = forceFunctions.attractionForce(dist.length(), k);

        dist.normalize().scale(force);
        return dist;
    }

    /**
     * For each node resets displacement vector and than calculates repulsion summed up into displacement vector
     */
    private void calculateRepulsionSerial(
            ElkNode layoutGraph, IElkProgressMonitor subTask, List<ElkNode>[][] grid, double k) {
        subTask.begin("Calculating Repulsion", layoutGraph.getChildren().size());

        for (ElkNode node : layoutGraph.getChildren()) {
            calculateRepulsionParallel(node, grid, k);
            subTask.worked(1);
        }

        subTask.done();
    }

    private void calculateRepulsionParallel(
            final ElkNode node, List<ElkNode>[][] grid, double k) {
        KVector nodeDisp = node.getProperty(FruchtermanReingoldOptions.OUTPUTS_DISPLACEMENT_VECTOR);
        nodeDisp.reset();
        getNodesInVicinity(node, grid, k).sequential()
                .map(neighbour -> calculateRepulsion(node, neighbour, k))
                .forEach(nodeDisp::add);
    }

    @SuppressWarnings("ConstantConditions")
    private KVector calculateRepulsion(ElkNode node, ElkNode neighbour, double k) {
        KVector dist = difference(node, neighbour);
        // act as if we have a small offset if we are at distance 0
        if (dist.length() == 0) {
            dist.x = detRand.doubles().map(d -> d - 0.5).filter(d -> d != 0).findFirst().getAsDouble();
            dist.y = detRand.doubles().map(d -> d - 0.5).filter(d -> d != 0).findFirst().getAsDouble();
        }
        double force = forceFunctions.repulsionForce(dist.length(), k);
        dist.normalize().scale(force);
        return dist;
    }

    /**
     * Get all nodes that node is repulsed by
     *
     * @param node
     *            the node we are observing from
     * @param grid
     *            the grid with the contained nodes in each sector
     * @param k
     *            the optimal distance for our nodes
     *
     * @return all nodes node gets repulsed from
     */
    private Stream<ElkNode> getNodesInVicinity(ElkNode node, List<ElkNode>[][] grid, double k) {

        switch (repulsionMode) {
        case RADIUS2K:
        case GRID3X3:
            KVector gridPos = node.getProperty(FruchtermanReingoldOptions.OUTPUTS_GRID_SECTION);

            // gather all nodes in the 3x3 sections around the grid section node is in
            Stream.Builder<ElkNode> builder = Stream.builder();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (gridPos.x + x < 0 || gridPos.x + x >= grid.length) {
                        continue;
                    }
                    if (gridPos.y + y < 0 || gridPos.y + y >= grid[(int) (gridPos.x + x)].length) {
                        continue;
                    }
                    List<ElkNode> section = grid[(int) (gridPos.x + x)][(int) (gridPos.y + y)];
                    section.forEach(builder);
                }
            }

            // remove node itself
            Stream<ElkNode> tmp = builder.build().filter(e -> !e.equals(node));

            if (repulsionMode == RepulsionEnum.RADIUS2K) {
                // filter nodes that are further from node than 2k
                tmp = tmp.filter(n -> difference(node, n).length() <= 2 * k);
            }

            return tmp;
        case REPULSE_ALL:
            // gather all nodes but node
            return node.getParent().getChildren().stream().filter(e -> !node.equals(e));
        default:
            // new mode unknown to us returning empty Stream
            return Stream.empty();
        }
    }

    /**
     * @param a
     *            Node who's position to subtract from
     * @param b
     *            Node who's position to subtract
     *
     * @return vector of a.pos - b.pos
     */
    private KVector difference(ElkNode a, ElkNode b) {
        return new KVector(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private void calculateGrid(ElkNode layoutGraph, List<ElkNode>[][] grid, double k) {
        KVector p;
        for (ElkNode node : layoutGraph.getChildren()) {
            p = node.getProperty(FruchtermanReingoldOptions.OUTPUTS_GRID_SECTION);
            grid[(int) p.x][(int) p.y].remove(node);
            grid[(int) (p.x = node.getX() / (2 * k))][(int) (p.y = node.getY() / (2 * k))].add(node);
        }
    }

}
