package de.webtwob.agd.s4.layouts.impl.layer;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class TopologicalLayerAssignement implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        List<ElkNode> notAssigned = new LinkedList<>(graph.getChildren());
        
        int currentLayer = 0;
        while(!notAssigned.isEmpty()) {
            ElkNode next = notAssigned.stream()
                    //only take those with all incoming edge sources assigned
                .filter(n->n.getIncomingEdges().stream().flatMap(e->e.getSources().stream()).noneMatch(s->s.getProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER)==-1))
                    //should always exists since the graph is acyclic and we still have un assigned nodes
                .findFirst().get();
            
            next.setProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER, currentLayer++);
            notAssigned.remove(next);
        }
        
        graph.setProperty(LayerBasedMetaDataProvider.OUTPUTS_LAYER_COUNT, currentLayer);
        
    } 

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum,ElkNode>create()
                .after(LayoutPhasesEnum.LAYER_ASSIGNEMENT)
                .add(ProcessorEnum.UNDO_CYCLE_BREAK);
    }

}
