package de.webtwob.agd.s4.layouts.enums.phases;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.ILayoutPhaseFactory;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.properties.AdvancedPropertyValue;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.impl.layer.LongestPathLayerAssignement;
import de.webtwob.agd.s4.layouts.impl.layer.TopologicalLayerAssignement;
import de.webtwob.agd.s4.layouts.impl.noop.NoopPhase;

public enum LayerAssignementEnum implements ILayoutPhaseFactory<LayoutPhasesEnum, ElkNode>{
    
    @AdvancedPropertyValue
    NOOP(NoopPhase::new),
    TOPOLOGICAL(TopologicalLayerAssignement::new),
    LONGESTPATH(LongestPathLayerAssignement::new);
    
    private final ILayoutPhaseFactory<LayoutPhasesEnum, ElkNode> factory;

    private LayerAssignementEnum(ILayoutPhaseFactory<LayoutPhasesEnum, ElkNode> factory) {
        this.factory = factory;
    }
    
    @Override
    public ILayoutPhase<LayoutPhasesEnum, ElkNode> create() {
        return factory.create();
    }
    
}
