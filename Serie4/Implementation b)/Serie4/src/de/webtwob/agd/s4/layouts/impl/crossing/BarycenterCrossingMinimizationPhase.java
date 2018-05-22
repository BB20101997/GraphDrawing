package de.webtwob.agd.s4.layouts.impl.crossing;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
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
        //TODO an Iteration should only consist of either a forward or a backward sweep not both
    }

    private void downSweep(Map<Integer, List<ElkNode>> layers) {
        for (int i = 1; i < layers.size() - 2; i++) {
            List<ElkNode> lower = layers.get(i);
            TreeMap<Double, ElkNode> barycenter = new TreeMap<Double, ElkNode>();
            
            double max = 1;
            
            for (ElkNode n : lower) {
                
                //calculate barycenter of current node
                double barycenterOfNode = n.getIncomingEdges().stream()
                    .map(e->Util.getSource(e))
                    .mapToInt(node -> node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER))
                    .average()
                    .orElse(max);
               
                barycenter.put(barycenterOfNode, n);
                //TODO is it correct that max should normally be increased by the Nodes barycenter and not set to it?
                max += n.getIncomingEdges().isEmpty() ? 1 : barycenterOfNode;
            }
            
            int pos = 0;
            while (!barycenter.isEmpty()) {
                barycenter.pollFirstEntry().getValue().setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, pos++);
            }
        }
    }

    private void upSweep(Map<Integer, List<ElkNode>> layers) {
        for (int i = layers.size() - 2; i >= 0; i--) {
            List<ElkNode> upper = layers.get(i);
            TreeMap<Double, ElkNode> barycenter = new TreeMap<Double, ElkNode>();
            
            double max = 1;
            
            for (ElkNode n : upper) {
                
                //calculate barycenter of current node
                double barycenterOfNode = n.getOutgoingEdges().stream()
                    .map(e->Util.getTarget(e))
                    .mapToInt(node -> node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER))
                    .average()
                    .orElse(max);
                
                barycenter.put(barycenterOfNode, n);
                max += n.getOutgoingEdges().isEmpty() ? 1 :  barycenterOfNode;
            }
            
            int pos = 0;
            while (!barycenter.isEmpty()) {
                barycenter.pollFirstEntry().getValue().setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, pos++);
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
