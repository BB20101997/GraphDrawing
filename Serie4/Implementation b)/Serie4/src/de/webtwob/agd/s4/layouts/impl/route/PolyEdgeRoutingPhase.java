package de.webtwob.agd.s4.layouts.impl.route;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;

public class PolyEdgeRoutingPhase implements ILayoutPhase<de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("PolyEdgeRoutingPhase", 1);
        
        /*
         * TODO edges should not all go to/start from the same point
         * Map<ElkNode,Integer> inEdgeCount = new HashMap<>();
         * Map<ElkNode,Integer> outEdgeCount = new HashMap<>();
         */
        
        for(ElkEdge edge:graph.getContainedEdges()) {
            if (progressMonitor.isCanceled()) {
                progressMonitor.done();
                return;
            }
            
            ElkNode source = ElkGraphUtil.connectableShapeToNode(Util.getSource(edge));            
            ElkNode target = ElkGraphUtil.connectableShapeToNode(Util.getTarget(edge));
            
            ElkEdgeSection sect = ElkGraphUtil.firstEdgeSection(edge, true, true);
            
            //source to the left of target
            if(source.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IN_LAYER)<target.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IN_LAYER)) {
                sect.setStartLocation(source.getX()+source.getWidth(), source.getY()+source.getWidth()/2);
                sect.setEndLocation(target.getX(), target.getY()+target.getHeight()/2);
            }else {
                sect.setStartLocation(source.getX(), source.getY()+source.getWidth()/2);
                sect.setEndLocation(target.getX()+target.getWidth(), target.getY()+target.getHeight()/2);    
            }
            
        }
        
        progressMonitor.done();
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create()// run init first
                .before(LayoutPhasesEnum.EDGE_ROUTING)
                .add(ProcessorEnum.UNDO_CYCLE_BREAK);
    }

}
