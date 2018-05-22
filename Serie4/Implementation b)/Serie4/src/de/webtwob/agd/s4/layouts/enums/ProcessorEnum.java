package de.webtwob.agd.s4.layouts.enums;

import java.util.LinkedList;

import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.alg.ILayoutProcessorFactory;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public enum ProcessorEnum implements ILayoutProcessorFactory<ElkNode>{
    
    //before Cycle Break Phase
    INIT(ProcessorEnum::prepareGraph),
    //after Layer Assignment
    
    //after LayerAssignement Phase
    UNDO_CYCLE_BREAK(ProcessorEnum::undoCycleBreak),
    
    //before Crossing Minimization
    DUMMY_PLACEMENT(ProcessorEnum::placeDummyNode),
    
    //after Edge Route Phase
    UNDO_DUMMY_PLACEMENT(ProcessorEnum::undoDummyNodes),
    
    //after Edge Route Phase
    POST(ProcessorEnum::postProcess);

    private final ILayoutProcessor<ElkNode> processor;
    
    private ProcessorEnum(ILayoutProcessor<ElkNode> node) {
        processor = node;
    }
    

    @Override
    public ILayoutProcessor<ElkNode> create(){
        return processor;
    };
    
    
    private static void prepareGraph(ElkNode graph, IElkProgressMonitor monitor) {
        //TODO e.g. convert hyper-edges to multiple simple-edges
        //maybe add an undo phase at the end
    }
    
    private static void undoCycleBreak(ElkNode graph,IElkProgressMonitor monitor) {
        for(ElkEdge edge : new LinkedList<>(graph.getContainedEdges())) {
            if(edge.getProperty(LayerBasedMetaDataProvider.OUTPUTS_EDGE_REVERSED)) {
               Util.reverseEdge(edge);
            }
        }
    }
   
    
    private static void placeDummyNode(ElkNode graph,IElkProgressMonitor monitor) {
        for(ElkEdge edge:graph.getContainedEdges()) {
            //TODO add check before cast
            ElkNode source = (ElkNode) edge.getSources().get(0);
            ElkNode target = (ElkNode) edge.getTargets().get(0);
            
            int sourceLayer = Util.getLayer(source);
            int targetLayer = Util.getLayer(target);
            
            int dir = (int) Math.signum(targetLayer-sourceLayer);
            
            if(Math.abs(sourceLayer-targetLayer)<=1) {
                continue;
            }
            
            for(int layer = sourceLayer;layer!=targetLayer;layer+=dir) {
                
            }
            
        }
        //TODO replace edges spanning more than one layer by dummies and sub-edges
    }
    
    private static void undoDummyNodes(ElkNode graph,IElkProgressMonitor monitor) {
        //TODO remove Dummy Nodes and re-route edges to/between/from dummy nodes
    }
    
    private static void postProcess(ElkNode graph, IElkProgressMonitor monitor) {
        double minX = 0,minY = 0,maxX = 0,maxY = 0;
        for(ElkNode node:graph.getChildren()) {
            //TODO make margin configurable (and take labels into account)
            minX = Math.min(minX, node.getX()-20);
            minY = Math.min(minY, node.getY()-20);
            maxX = Math.max(maxX, node.getX()+node.getWidth()+20);
            maxY = Math.max(maxY, node.getY()+node.getHeight()+20);
        }
        
        for(ElkNode node:graph.getChildren()) {
            node.setX(node.getX()-minX);
            node.setY(node.getY()-minY);
        }
        
        graph.setDimensions(maxX-minX, maxY-minY);
        
    }
    
}
