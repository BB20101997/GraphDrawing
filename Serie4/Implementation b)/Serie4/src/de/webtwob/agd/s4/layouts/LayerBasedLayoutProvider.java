package de.webtwob.agd.s4.layouts;

import java.util.List;

import org.eclipse.elk.core.AbstractLayoutProvider;
import org.eclipse.elk.core.alg.AlgorithmAssembler;
import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.impl.noop.NoopPhase;

public class LayerBasedLayoutProvider extends AbstractLayoutProvider {
    
    private final AlgorithmAssembler<LayoutPhasesEnum, ElkNode> algAssembler = AlgorithmAssembler.create(LayoutPhasesEnum.class);

    @Override
    public void layout(ElkNode layoutGraph, IElkProgressMonitor progressMonitor) {
        List<ILayoutProcessor<ElkNode>> alg = assemleAlgorithm(layoutGraph);
        
        progressMonitor.begin("LayerBased Layout", alg.size());
        
        for(ILayoutProcessor<ElkNode> proc: alg) {
            if(proc!=null)
            proc.process(layoutGraph, progressMonitor.subTask(1));
        }
        
        progressMonitor.done();
        
    }

    private List<ILayoutProcessor<ElkNode>> assemleAlgorithm(ElkNode layoutGraph) {
        algAssembler.reset();

        //Config Phases
        //TODO replace NoopPhase with actuall implementation
        algAssembler.setPhase(LayoutPhasesEnum.CYCLE_BREAK,NoopPhase::new);
        algAssembler.setPhase(LayoutPhasesEnum.LAYER_ASSIGNEMENT,NoopPhase::new);
        algAssembler.setPhase(LayoutPhasesEnum.CROSSING_MINIMIZATION,NoopPhase::new);
        algAssembler.setPhase(LayoutPhasesEnum.NODE_PLACEMENT,NoopPhase::new);
        algAssembler.setPhase(LayoutPhasesEnum.EDGE_ROUTING,NoopPhase::new);
        
        return algAssembler.build(layoutGraph);
    }

}
