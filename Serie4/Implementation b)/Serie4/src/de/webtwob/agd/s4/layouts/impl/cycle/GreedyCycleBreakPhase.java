package de.webtwob.agd.s4.layouts.impl.cycle;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.EList;

import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;

public class GreedyCycleBreakPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        EList<ElkNode> children = graph.getChildren();
        
        children.stream().flatMap(n->n.getOutgoingEdges().stream()).forEach(e->{
            if(children.indexOf(e.getSources().get(0))>children.indexOf(e.getTargets().get(0))) {
                //TODO editing edges while iterating over them may obset the Iterator
                Util.reverseEdge(e);
            }
        });
        
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> conf = 
                LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode>create()
                .before(LayoutPhasesEnum.CYCLE_BREAK)
                .add(ProcessorEnum.INIT) //run init first
                .after(LayoutPhasesEnum.CROSSING_MINIMIZATION)
                .add(ProcessorEnum.UNDO_CYCLE_BREAK) //undo cycle break after layers are assigned
                .after(LayoutPhasesEnum.EDGE_ROUTING)
                .add(ProcessorEnum.POST); //TODO move to Edge Routing Implementation once implemented
        return conf;
    }

}
