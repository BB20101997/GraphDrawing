package de.webtwob.agd.s4.layouts.impl.cycle;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;

public class GreedzCycleBreakPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        // TODO Break Cycles Greedy
        
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> conf = 
                LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode>create()
                .before(LayoutPhasesEnum.CYCLE_BREAK)
                .add(ProcessorEnum.INIT) //run init first
                .after(LayoutPhasesEnum.LAYER_ASSIGNEMENT)
                .add(ProcessorEnum.UNDO_CYCLE_BREAK); //undo cycle break after layers are assigned
        
        return conf;
    }

}
