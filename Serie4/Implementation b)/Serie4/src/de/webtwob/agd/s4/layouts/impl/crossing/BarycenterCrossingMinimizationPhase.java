package de.webtwob.agd.s4.layouts.impl.crossing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.function.Function;

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
    
    private static Function<Map<ElkNode,Double>,Comparator<ElkNode>> comparatorFactory = map ->{
        return Comparator
                .<ElkNode>comparingDouble(map::get)
                .thenComparingDouble(n->n.getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER));
                };

    /**
     * assigns their Position in their layer based on their barycenter in the list.
     */
    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        Map<Integer, List<ElkNode>> layers = Util.getLayers(graph);
        
        giveValuesFirst(layers.get(0));
        // More than 1 Sweep
        for (int j = 0; j < ITERATIONS; j++) {
           if (j%1==0) {
               // Forwards
               downSweep(layers);
           } else {
               // Backwards
               upSweep(layers);
           }
        }
    }
    /**Sweeps from top to bottom
     * 
     * @param layers The Layers of the Graph
     */
    private void downSweep(Map<Integer, List<ElkNode>> layers) {
        for (int i = 1; i < layers.size() - 2; i++) {
            List<ElkNode> lower = layers.get(i);

            Map<ElkNode,Double> barycenterMap = new HashMap<>();
            
            double max = 1;
            
            for (ElkNode n : lower) {
                
                //calculate barycenter of current node
                double barycenterOfNode = n.getIncomingEdges().stream()
                    .map(e->Util.getSource(e))
                    .mapToInt(node -> node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER))
                    .average()
                    .orElse(max);

                barycenterMap.put(n, barycenterOfNode);
                //TODO is it correct that max should normally be increased by the Nodes barycenter and not set to it?
                max += n.getIncomingEdges().isEmpty() ? 1 : barycenterOfNode;
            }
            
            List<ElkNode> tmp = new ArrayList<>(lower);
            tmp.sort(comparatorFactory.apply(barycenterMap));
            
            lower.forEach(n->n.setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, tmp.indexOf(n)));
       
        }
    }

    /**Sweeps from bottom to top
     * 
     * @param layers The Layers of the Graph
     */
    private void upSweep(Map<Integer, List<ElkNode>> layers) {
        for (int i = layers.size() - 2; i >= 0; i--) {
            
            List<ElkNode> upper = layers.get(i);
            
            Map<ElkNode,Double> barycenterMap = new HashMap<>();
            
            double max = 1;
            
            for (ElkNode n : upper) {
                
                //calculate barycenter of current node
                double barycenterOfNode = n.getOutgoingEdges().stream()
                    .map(e->Util.getTarget(e))
                    .mapToInt(node -> node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER))
                    .average()
                    .orElse(max);
                
                barycenterMap.put(n, barycenterOfNode);
                
                max += n.getOutgoingEdges().isEmpty() ? 1 :  barycenterOfNode;
            }
            
            List<ElkNode> tmp = new ArrayList<>(upper);
            tmp.sort(comparatorFactory.apply(barycenterMap));
            
            upper.forEach(n->n.setProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER, tmp.indexOf(n)));
            
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
