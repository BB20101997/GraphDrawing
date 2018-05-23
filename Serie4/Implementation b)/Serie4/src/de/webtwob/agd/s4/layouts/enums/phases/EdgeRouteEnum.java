package de.webtwob.agd.s4.layouts.enums.phases;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.ILayoutPhaseFactory;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.properties.AdvancedPropertyValue;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.impl.noop.NoopPhase;
import de.webtwob.agd.s4.layouts.impl.route.PolyEdgeRoutingPhase;

public enum EdgeRouteEnum implements ILayoutPhaseFactory<LayoutPhasesEnum, ElkNode>{
    
    @AdvancedPropertyValue
    NOOP(NoopPhase::new),
    POLY(PolyEdgeRoutingPhase::new);
    
    private final ILayoutPhaseFactory<LayoutPhasesEnum, ElkNode> factory;
    
    private EdgeRouteEnum(ILayoutPhaseFactory<LayoutPhasesEnum, ElkNode> factory) {
        this.factory = factory;
    }
   

    @Override
    public ILayoutPhase<LayoutPhasesEnum, ElkNode> create() {
        return factory.create();
    }
    
}
