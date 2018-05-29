package de.webtwob.agd.s4.layouts;

import java.util.List;

import org.eclipse.elk.core.AbstractLayoutProvider;
import org.eclipse.elk.core.alg.AlgorithmAssembler;
import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;

public class LayerBasedLayoutProvider extends AbstractLayoutProvider {
    
    private final AlgorithmAssembler<LayoutPhasesEnum, ElkNode> algAssembler = AlgorithmAssembler.create(LayoutPhasesEnum.class);

    @Override
    public void layout(ElkNode layoutGraph, IElkProgressMonitor progressMonitor) {
        List<ILayoutProcessor<ElkNode>> alg = assemleAlgorithm(layoutGraph);
        
        progressMonitor.begin("LayerBased Layout", alg.size());
        
        for(ILayoutProcessor<ElkNode> proc: alg) {
            if(progressMonitor.isCanceled())
                return;
            if(proc!=null)
            proc.process(layoutGraph, progressMonitor.subTask(1));
        }
        
        progressMonitor.done();
        
    }

    private List<ILayoutProcessor<ElkNode>> assemleAlgorithm(ElkNode layoutGraph) {
        algAssembler.reset();

        //Configure Phases
        algAssembler.setPhase(LayoutPhasesEnum.CYCLE_BREAK,layoutGraph.getProperty(LayerBasedLayoutMetadata.SETTINGS_CYCLE_BREAK_PHASE));
        algAssembler.setPhase(LayoutPhasesEnum.LAYER_ASSIGNEMENT,layoutGraph.getProperty(LayerBasedLayoutMetadata.SETTINGS_LAYER_ASSIGNEMENT_PHASE));
        algAssembler.setPhase(LayoutPhasesEnum.CROSSING_MINIMIZATION,layoutGraph.getProperty(LayerBasedLayoutMetadata.SETTINGS_CROSSING_MINIMIZATION_PHASE));
        algAssembler.setPhase(LayoutPhasesEnum.NODE_PLACEMENT,layoutGraph.getProperty(LayerBasedLayoutMetadata.SETTINGS_NODE_PLACEMENT_PHASE));
        algAssembler.setPhase(LayoutPhasesEnum.EDGE_ROUTING,layoutGraph.getProperty(LayerBasedLayoutMetadata.SETTINGS_EDGE_ROUTING_PHASE));
        
        //add mandatory Processors
        LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> procConf;
        procConf = LayoutProcessorConfiguration.<LayoutPhasesEnum,ElkNode>create()
                .addBefore(LayoutPhasesEnum.CROSSING_MINIMIZATION, ProcessorEnum.INIT)
                .addAfter(LayoutPhasesEnum.EDGE_ROUTING, ProcessorEnum.POST);
        
        algAssembler.addProcessorConfiguration(procConf);
        
        return algAssembler.build(layoutGraph);
    }

}
