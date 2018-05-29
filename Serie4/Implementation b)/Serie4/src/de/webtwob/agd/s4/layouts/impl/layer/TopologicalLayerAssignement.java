package de.webtwob.agd.s4.layouts.impl.layer;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class TopologicalLayerAssignement implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("TopologicalLayerAssignement", graph.getChildren().size());
        
        List<ElkNode> notAssigned = new LinkedList<>(graph.getChildren());
        
        
        LinkedList<ElkNode> candidates
        = notAssigned.stream().filter(Util::allPredecessorsHaveAnAssignedLayer).collect(Collectors.toCollection(LinkedList::new));
        
        int currentLayer = 0;
        while(!notAssigned.isEmpty()) {
            if (progressMonitor.isCanceled()) {
                progressMonitor.done();
                return;
            }
            ElkNode node = candidates.poll();
            
            if(node!=null) {
                node.setProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER, currentLayer++);
                notAssigned.remove(node);
                candidates.addAll(
                    node.getOutgoingEdges().stream()
                        .map(Util::getTarget)
                        .filter(Util::allPredecessorsHaveAnAssignedLayer)
                        .filter(n->!candidates.contains(n))
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                );
                progressMonitor.worked(1);
            }else {
                throw new IllegalStateException("Assumed Acyclic Graph, but no Source found!");
            }
       }
        
        graph.setProperty(LayerBasedMetaDataProvider.OUTPUTS_LAYER_COUNT, currentLayer);
        progressMonitor.done();
    } 

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum,ElkNode>create();
    }

}
