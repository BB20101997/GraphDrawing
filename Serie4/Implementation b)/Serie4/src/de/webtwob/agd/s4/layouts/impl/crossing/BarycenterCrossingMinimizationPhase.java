package de.webtwob.agd.s4.layouts.impl.crossing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.EList;

import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class BarycenterCrossingMinimizationPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    private static Comparator<ElkNode> getComparator(Map<ElkNode, Double> map) {
        return Comparator.<ElkNode> comparingDouble(map::get)
                .thenComparingDouble(n -> n.getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER));
    }

    /**
     * assigns their Position in their layer based on their barycenter in the list.
     */
    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        int iterations = graph.getProperty(LayerBasedMetaDataProvider.SETTINGS_CROSSING_MINIMIZATION_ITERATIONS);
        int permutationCount = graph.getProperty(LayerBasedMetaDataProvider.SETTINGS_CROSSING_MINIMIZATION_PERMUTATIONS); // TODO Option for how many permutations
        progressMonitor.begin("BarycenterCrossingMinimizationPhase", permutationCount * iterations);

        Random r = new Random(42007); // Seed

        Map<Integer, List<ElkNode>> layers = Util.getLayers(graph);
        ArrayList<ElkNode[]> permutations = new ArrayList<ElkNode[]>();
        int[] crossingsPerPermutation = new int[permutationCount];

        for (int i = 0; i < permutationCount; i++) {
            giveValuesFirst(layers.get(0), r);
            permutations.add(givePermutation(layers.get(0)));

            // More than 1 Sweep
            for (int j = 0; j < iterations; j++) {
                if (j % 2 == 0) {
                    // Forwards
                    downSweep(layers);
                } else {
                    // Backwards
                    upSweep(layers);
                }
                progressMonitor.worked(1);
            }

            crossingsPerPermutation[i] = countAllCrossings(layers);
        }
        int i = getBestPermutation(crossingsPerPermutation);
        setPermutationFirstLayer(permutations, i);
        for (int j = 0; j < iterations; j++) {
            if (j % 2 == 0) {
                // Forwards
                downSweep(layers);
            } else {
                // Backwards
                upSweep(layers);
            }
            progressMonitor.worked(1);
        }
        progressMonitor.done();
    }

    private void setPermutationFirstLayer(ArrayList<ElkNode[]> permutations, int i) {
        ElkNode[] permutation = permutations.get(1);
        for (int j = 0; j < permutation.length; j++) {
            permutation[j].setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, j);
        }
    }

    private int getBestPermutation(int[] l) {
        int best = 0;
        for (int i = 1; i < l.length; i++) {
            if (l[best] > l[i]) {
                best = i;
            }
        }
        return best;
    }

    private int countAllCrossings(Map<Integer, List<ElkNode>> layers) {
        int count = 0;
        for (int i = 1; i < layers.size(); i++) {
            count += testCrossings(layers.get(i - 1), layers.get(i));
        }
        return count;
    }

    private ElkNode[] givePermutation(List<ElkNode> list) {
        ElkNode[] l = new ElkNode[list.size()];
        for (ElkNode n : list) {
            l[n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER)] = n;
        }
        return l;
    }

    /**
     * Sweeps from top to bottom
     * 
     * @param layers
     *            The Layers of the Graph
     */
    private void downSweep(Map<Integer, List<ElkNode>> layers) {
        Map<ElkNode, Double> barycenterMap = new HashMap<>();
        Comparator<ElkNode> cmp = getComparator(barycenterMap);

        for (int i = 1; i < layers.size(); i++) {
            List<ElkNode> lower = layers.get(i);

            // safe positions if the sweep increases crossings
            ElkNode[] oldPositions = new ElkNode[lower.size()];
            int oldCrossings = testCrossings(layers.get(i - 1), lower);
            for (ElkNode n : lower) {
                if (n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER) != Util.UNASSIGNED) {
                    oldPositions[n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER)] = n;
                }
            }

            double max = 1;
            // calculate barycenter for each node
            for (ElkNode n : lower) {

                // calculate barycenter of current node
                double barycenterOfNode = n.getIncomingEdges().stream().map(e -> Util.getSource(e))
                        .mapToInt(node -> node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER)).average()
                        .orElse(max);

                // update barycenter in the map
                barycenterMap.put(n, barycenterOfNode);

                max += n.getIncomingEdges().isEmpty() ? 1 : barycenterOfNode;
            }

            // make a copy we can sort
            List<ElkNode> tmp = new ArrayList<>(lower);
            tmp.sort(cmp);

            // update Position in layer
            lower.forEach(n -> n.setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, tmp.indexOf(n)));

            // test if the crossings increased
            int newCrossings = testCrossings(layers.get(i - 1), lower);
            if (oldCrossings < newCrossings) {
                for (int j = 0; j < oldPositions.length; j++) {
                    oldPositions[j].setProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER, j);
                }
            }
        }
    }

    /**
     * Sweeps from bottom to top
     * 
     * @param layers
     *            The Layers of the Graph
     */
    private void upSweep(Map<Integer, List<ElkNode>> layers) {
        Map<ElkNode, Double> barycenterMap = new HashMap<>();
        Comparator<ElkNode> cmp = getComparator(barycenterMap);

        for (int i = layers.size() - 2; i >= 0; i--) {
            List<ElkNode> upper = layers.get(i);
            double max = 1;

            // safe positions if the sweep increases crossings
            ElkNode[] oldPositions = new ElkNode[upper.size()];
            int oldCrossings = testCrossings(upper, layers.get(i + 1));
            for (ElkNode n : upper) {
                if (n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER) != Util.UNASSIGNED) {
                    oldPositions[n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER)] = n;
                }
            }

            // calculate barycenter for each node
            for (ElkNode n : upper) {

                // calculate barycenter of current node
                double barycenterOfNode = n.getOutgoingEdges().stream().map(e -> Util.getTarget(e))
                        .mapToInt(node -> node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER)).average()
                        .orElse(max);

                // update barycenter in the map
                barycenterMap.put(n, barycenterOfNode);

                max += n.getOutgoingEdges().isEmpty() ? 1 : barycenterOfNode;
            }

            List<ElkNode> tmp = new ArrayList<>(upper);
            tmp.sort(cmp);

            // update Position in layer
            upper.forEach(n -> n.setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, tmp.indexOf(n)));

            // test if the crossings increased
            int newCrossings = testCrossings(upper, layers.get(i + 1));
            if (oldCrossings < newCrossings) {
                for (int j = 0; j < oldPositions.length; j++) {
                    oldPositions[j].setProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER, j);
                }
            }

        }
    }

    private int testCrossings(List<ElkNode> upper, List<ElkNode> lower) {
        upper.sort(
                Comparator.comparingInt((ElkNode n) -> n.getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER)));
        List<ElkEdge> edges = upper.get(0).getOutgoingEdges();
        if (!edges.isEmpty()) {
            edges = sortEdges(edges);
        }
        for (int i = 1; i < upper.size(); i++) {
            List<ElkEdge> tempEdges = upper.get(i).getOutgoingEdges();
            if (!tempEdges.isEmpty()) {
                tempEdges = sortEdges(tempEdges);
                edges.addAll(tempEdges);
            }
        }
        int[] positions = new int[edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            positions[i] = Util.getTarget(edges.get(i)).getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER);
        }
        return inversions(positions);
    }

    private List<ElkEdge> sortEdges(List<ElkEdge> edges) {
        LinkedList<ElkEdge> l = new LinkedList<ElkEdge>();
        l.add(edges.get(0));
        for (int i = 1; i < edges.size(); i++) {
            boolean set = false;
            for (int j = 0; !set && j < l.size(); j++) {
                if (Util.getTarget(l.get(j)).getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER) > Util
                        .getTarget(edges.get(i)).getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER)) {
                    l.add(j, edges.get(i));
                    set = true;
                }
            }
        }
        return l;
    }

    private int inversions(int[] positions) {
        int inversions = 0;
        for (int i = 0; i < positions.length; i++) {
            for (int j = i + 1; j < positions.length; j++) {
                if (positions[i] == Util.UNASSIGNED || positions[j] < positions[i]) {
                    inversions++;
                }
            }
        }
        return inversions;
    }

    private Map<ElkNode, Integer> giveValuesFirst(List<ElkNode> first, Random r) {
        Collections.shuffle(first, r);
        int i = 0;
        for (ElkNode node : first) {
            node.setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, i++);
        }
        return null;
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create()
                .before(LayoutPhasesEnum.CROSSING_MINIMIZATION).add(ProcessorEnum.DUMMY_PLACEMENT)
                .after(LayoutPhasesEnum.EDGE_ROUTING).add(ProcessorEnum.UNDO_DUMMY_PLACEMENT);
    }
}
