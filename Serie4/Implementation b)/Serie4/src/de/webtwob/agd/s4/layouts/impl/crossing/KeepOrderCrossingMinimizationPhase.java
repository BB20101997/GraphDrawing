package de.webtwob.agd.s4.layouts.impl.crossing;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;

public class KeepOrderCrossingMinimizationPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    /**
     * assigns their Position in their layer based on their current position in the list.
     */
    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("KeepOrderCrossingMinimizationPhase", graph.getChildren().size());
        
        Util.getLayers(graph).values().forEach(v -> {
            int i = 0;
            for(ElkNode node : v) {
                node.setProperty(LayerBasedLayoutMetadata.OUTPUTS_POS_IN_LAYER, i++);
                progressMonitor.worked(1);
            }
        });
        
        progressMonitor.done();
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode>create()
                .before(LayoutPhasesEnum.CROSSING_MINIMIZATION)
                .add(ProcessorEnum.DUMMY_PLACEMENT)
                .after(LayoutPhasesEnum.EDGE_ROUTING)
                .add(ProcessorEnum.UNDO_DUMMY_PLACEMENT);
    }

}
