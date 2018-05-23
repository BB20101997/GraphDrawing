package de.webtwob.agd.s4.layouts.impl.cycle;

import java.util.LinkedList;

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

        new LinkedList<>(graph.getContainedEdges()).stream().forEach(e -> {
            // reverse all edges where the source Node index is higher than the target node index
            if (children.indexOf(Util.getSource(e)) > children.indexOf(Util.getTarget(e))) {
                Util.reverseEdge(e);
            }
        });

    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> conf =
                LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create().before(LayoutPhasesEnum.CYCLE_BREAK).add(ProcessorEnum.INIT) // run init first
                        .before(LayoutPhasesEnum.EDGE_ROUTING)
                        .add(ProcessorEnum.UNDO_CYCLE_BREAK);
        return conf;
    }

}
