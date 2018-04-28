package de.webtowb.agd.s2.layouts;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.eclipse.elk.core.AbstractLayoutProvider;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.adg.s2.layouts.options.FruchtermanReingoldOptions;

public class FruchtermanReingoldLayoutProvider extends AbstractLayoutProvider {

    RepulsionEnum repulsionMode = RepulsionEnum.RADIUS2K;
    InitialLayoutEnum initLayout = InitialLayoutEnum.CIRCLE;
    double C = 1;
    int iterations = 50;
    double width = 1000;// TODO find out where to get the real values from
    double height = 1000;// TODO if no real values exist make these options

    CoolingFunctionEnum coolingFunction = CoolingFunctionEnum.QUENCH_AND_SIMMER;

    Random rand = new Random();

    private double getArea() {
        return width * height;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void layout(ElkNode layoutGraph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Begin GraphLayout", iterations + 1);

        // load options
        repulsionMode = layoutGraph.getProperty(FruchtermanReingoldOptions.REPULSION_MODE);
        initLayout = layoutGraph.getProperty(FruchtermanReingoldOptions.INIT_LAYOUT);
        C = layoutGraph.getProperty(FruchtermanReingoldOptions.C_PARAMETER);
        iterations = layoutGraph.getProperty(FruchtermanReingoldOptions.ITERATIONS);
        int earlyStop = layoutGraph.getProperty(FruchtermanReingoldOptions.STOP_EARLY);
        width = layoutGraph.getProperty(FruchtermanReingoldOptions.FRAME_WIDTH);
        height = layoutGraph.getProperty(FruchtermanReingoldOptions.FRAME_HEIGHT);
        coolingFunction = layoutGraph.getProperty(FruchtermanReingoldOptions.COOLING_FUNCTION);

        // kGrid the section size for the grid
        double kGrid = Math.sqrt(getArea() / layoutGraph.getChildren().size());
        // optimal distance k = C sqrt(area/|vertices|) ,C found experimentally
        double kC = C * kGrid;

        List<ElkNode>[][] grid = null;

        grid = (LinkedList<ElkNode>[][]) new LinkedList<?>[(int) (width / (2 * kGrid)) + 1][(int) (height / (2 * kGrid))
                + 1];

        initSetup(layoutGraph, progressMonitor.subTask(1), grid, kGrid);

        /*
         * Neighbors attract divide into grid boxes, gridBoxes = |V|/4 gridbox side length = 2k = 2 sqrt(WL/|V|) all in
         * a 2k radius repulse in a 3x3 of grid boxes around a vertex
         * 
         **/

        if (!layoutGraph.getProperty(FruchtermanReingoldOptions.SKIP_LAYOUT)) {
            IElkProgressMonitor subTask;
            int iterationSize = layoutGraph.getChildren().size() * 2 + layoutGraph.getContainedEdges().size();
            for (int i = 0; i < iterations - earlyStop; i++) {
                subTask = progressMonitor.subTask(1);
                subTask.begin(String.format("Iteration %d", i), iterationSize);

                double temperature = coolingFunction.temperatur(layoutGraph, i);
                layoutGraph.getChildren().stream()
                        .forEach(n -> n.getProperty(FruchtermanReingoldOptions.DISPLACEMENT_VECTOR).reset());

                calculateRepulsion(layoutGraph, subTask.subTask(layoutGraph.getChildren().size()), grid, kC, kGrid);
                calculateAttraction(layoutGraph, subTask.subTask(layoutGraph.getContainedEdges().size()), grid, kC);
                performMovement(layoutGraph, subTask.subTask(layoutGraph.getChildren().size()), temperature);
                calculateGrid(layoutGraph, grid, kGrid);
                subTask.done();
            }
        }

        // Determine the size the parent should have and center the subgraph

        double maxX = 0, maxY = 0, minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        for (ElkNode node : layoutGraph.getChildren()) {
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

        // TODO edge routing

        progressMonitor.done();
    }

    private void initSetup(ElkNode layoutGraph, IElkProgressMonitor subTask, List<ElkNode>[][] grid, double kGrid) {
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
        case RANDOM_NON_DETERMANISTIC: // place Nodes Randomly
            // might produce two vertices with the same position and same neighbors being stuck together, unlikely
            // though
            for (ElkNode node : layoutGraph.getChildren()) {
                node.setLocation(width / 4 + rand.nextDouble() * width / 2,
                        height / 4 + rand.nextDouble() * height / 2);
            }
            break;
        }

        calculateGrid(layoutGraph, grid, kGrid);

        subTask.done();
    }

    private void calculateGrid(ElkNode layoutGraph, List<ElkNode>[][] grid, double kGrid) {
        KVector p;
        for (ElkNode node : layoutGraph.getChildren()) {
            p = node.getProperty(FruchtermanReingoldOptions.GRID_SECTION);
            grid[(int) p.x][(int) p.y].remove(node);
            grid[(int) (p.x = node.getX() / (2 * kGrid))][(int) (p.y = node.getY() / (2 * kGrid))].add(node);
        }
    }

    /**
     * Moves each node by their displacement vector, but no more than temperature and not outside of x in [0,width] and
     * y in [0,height]
     */
    private void performMovement(ElkNode layoutGraph, IElkProgressMonitor subTask, double temperature) {
        subTask.begin("Performing Movement", layoutGraph.getChildren().size());
        for (ElkNode node : layoutGraph.getChildren()) {
            KVector disp = node.getProperty(FruchtermanReingoldOptions.DISPLACEMENT_VECTOR);
            double displacementDistance = disp.length();

            // limit displacement by temperature
            disp.normalize().scale(Math.min(temperature, displacementDistance));

            // when hitting wall normal to wall
            node.setX(Math.min(width, Math.max(0, node.getX() + disp.x)));
            node.setY(Math.min(height, Math.max(0, node.getY() + disp.y)));
            subTask.worked(1);
        }
        subTask.done();
    }

    /**
     * Calculate the repulsion for each edge and adds it to the displacement vector of the nodes
     */
    private void calculateAttraction(ElkNode layoutGraph, IElkProgressMonitor subTask, List<ElkNode>[][] grid,
            double kC) {
        subTask.begin("Calculating Attraction", layoutGraph.getContainedEdges().size());

        for (ElkEdge edge : layoutGraph.getContainedEdges()) {
            for (ElkConnectableShape s1 : edge.getSources()) {
                for (ElkConnectableShape s2 : edge.getTargets()) {
                    // assumption 1 only edges between nodes
                    if (s1 instanceof ElkNode && s2 instanceof ElkNode && !s1.equals(s2)) {
                        ElkNode u = (ElkNode) s1;
                        ElkNode v = (ElkNode) s2;
                        KVector dist = difference(v, u);
                        double force = atractionForce(dist.length(), kC);
                        dist.normalize().scale(force);
                        v.getProperty(FruchtermanReingoldOptions.DISPLACEMENT_VECTOR).sub(dist);
                        u.getProperty(FruchtermanReingoldOptions.DISPLACEMENT_VECTOR).add(dist);
                    }
                }
            }
        }

        subTask.done();
    }

    /**
     * For each node resets displacement vector and than calculates repulsion summed up into displacement vector
     */
    private void calculateRepulsion(ElkNode layoutGraph, IElkProgressMonitor subTask, List<ElkNode>[][] grid, double kC,
            double kGrid) {
        subTask.begin("Calculating Repulsion", layoutGraph.getChildren().size());
        for (ElkNode node : layoutGraph.getChildren()) {
            KVector nodeDisp = node.getProperty(FruchtermanReingoldOptions.DISPLACEMENT_VECTOR);
            getNodesInVecinity(node, grid, kGrid).forEach(neighbour -> {
                KVector dist = difference(node, neighbour);
                double force = repulsionForce(dist.length(), kC);
                nodeDisp.add(dist.normalize().scale(force));
                subTask.worked(1);
            });
        }
        subTask.done();
    }

    /**
     * @param distance
     *            the distance for which to calculate the repulsion
     * @param kC
     *            the optimal distance for which to calculate the repulsion
     * 
     * @return the absolute force of the repulsion based on length and k
     */
    private double repulsionForce(double distance, double kC) {
        // TODO option for a selection of repulsion functions

        // f_r(d) = -k²/d
        return kC * kC / distance;
    }

    /**
     * @param distance
     *            the distance for wich to calculate the attraction
     * @param kC
     *            the optimal distance for which to calculate the attraction
     * 
     * @return the absolute force of the repulsion based on length and k
     */
    private double atractionForce(double distance, double kC) {
        // TODO option for a selection of attraction functions

        // f_a(d) = d²/k
        return distance * distance / kC;
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
     * 
     * @return all nodes node gets repulsed from
     */
    private Stream<ElkNode> getNodesInVecinity(ElkNode node, List<ElkNode>[][] grid, double kGrid) {

        switch (repulsionMode) {
        case RADIUS2K:
        case GRID3X3:
            KVector gridPos = node.getProperty(FruchtermanReingoldOptions.GRID_SECTION);

            // gather all nodes in the 3x3 sections around the grid section node is in
            Stream.Builder<ElkNode> builder = Stream.builder();
            for (int x = -1; x <= 1; x++) {
                in: for (int y = -1; y <= 1; y++) {
                    if (gridPos.x + x < 0 || gridPos.x + x >= grid.length)
                        continue in;
                    if (gridPos.y + y < 0 || gridPos.y + y >= grid[(int) (gridPos.x + x)].length)
                        continue in;
                    grid[(int) (gridPos.x + x)][(int) (gridPos.y + y)].forEach(builder::accept);
                }
            }
            // remove node itself
            Stream<ElkNode> tmp = builder.build().filter(e -> !e.equals(node));

            if (repulsionMode == RepulsionEnum.RADIUS2K) {
                // filter nodes that are further from node than 2k
                tmp = tmp.filter(n -> difference(node, n).length() <= 2 * kGrid);
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
     * @return vector of a.pos - b.pos
     */
    private KVector difference(ElkNode a, ElkNode b) {
        return new KVector(a.getX() - b.getX(), a.getY() - b.getY());
    }

}
