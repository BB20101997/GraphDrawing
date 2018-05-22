package de.webtwob.agd.s4.layouts.impl.crossing;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class BarycenterCrossingMinimizationPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    // TODO Option for iterations
    private static int ITERATIONS = 50;

    /**
     * assigns their Position in their layer based on their barycenter in the list.
     */
    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        Map<Integer, List<ElkNode>> layers = Util.getLayers(graph);
        giveValuesFirst(layers.get(0));
        // More than 1 Sweep
        for (int j = 0; j < ITERATIONS; j++) {
            // Forwards
            downSweep(layers);
            // Backwards
            upSweep(layers);
        }
    }

    private void downSweep(Map<Integer, List<ElkNode>> layers) {
        for (int i = 1; i < layers.size() - 2; i++) {
            List<ElkNode> lower = layers.get(i);
            TreeMap<Double, ElkNode> barycenter = new TreeMap<Double, ElkNode>();
            for (ElkNode n : lower) {
                double max = 1;
                int connectedNodes = 0;
                int sumOfNodes = 0;
                for (ElkEdge e : n.getIncomingEdges()) {
                    sumOfNodes +=
                            ElkGraphUtil.getSourceNode(e).getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER);
                    connectedNodes++;
                }
                double barycenterOfNode = (connectedNodes != 0 ? sumOfNodes / connectedNodes : max);
                barycenter.put(barycenterOfNode, n);
                max += connectedNodes != 0 ? barycenterOfNode : 1;
            }
            int pos = 0;
            while (!barycenter.isEmpty()) {
                barycenter.firstEntry().getValue().setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, pos++);
                barycenter.remove(barycenter.firstEntry().getKey(), barycenter.firstEntry().getValue());
            }
        }
    }

    private void upSweep(Map<Integer, List<ElkNode>> layers) {
        for (int i = layers.size() - 2; i >= 0; i--) {
            List<ElkNode> upper = layers.get(i);
            TreeMap<Double, ElkNode> barycenter = new TreeMap<Double, ElkNode>();
            for (ElkNode n : upper) {
                double max = 1;
                int connectedNodes = 0;
                int sumOfNodes = 0;
                for (ElkEdge e : n.getIncomingEdges()) {
                    sumOfNodes +=
                            ElkGraphUtil.getSourceNode(e).getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER);
                    connectedNodes++;
                }
                double barycenterOfNode = (connectedNodes != 0 ? sumOfNodes / connectedNodes : max);
                barycenter.put(barycenterOfNode, n);
                max += connectedNodes != 0 ? barycenterOfNode : 1;
            }
            int pos = 0;
            while (!barycenter.isEmpty()) {
                barycenter.firstEntry().getValue().setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, pos++);
                barycenter.remove(barycenter.firstEntry().getKey(), barycenter.firstEntry().getValue());
            }
        }
    }

    private Map<ElkNode, Integer> giveValuesFirst(List<ElkNode> first) {
        int i = 1;
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
