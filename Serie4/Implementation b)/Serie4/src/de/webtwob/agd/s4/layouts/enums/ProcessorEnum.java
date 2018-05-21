package de.webtwob.agd.s4.layouts.enums;

import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.alg.ILayoutProcessorFactory;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import de.webtwob.adg.s4.layouts.options.LayerBasedMetaDataProvider;

public enum ProcessorEnum implements ILayoutProcessorFactory<ElkNode>{
    
    //before Cycle Break Phase
    INIT(ProcessorEnum::prepareGraph),
    //after Layer Assignment
    
    //after LayerAssignement Phase
    UNDO_CYCLE_BREAK(ProcessorEnum::undoCycleBreak),
    //before crossing minimization
    
    //before Crossing Minimization
    DUMMY_PLACEMENT(ProcessorEnum::placeDummyNode),
    //after Edge Rout Phase
    UNDO_DUMMY_PLACEMENT(ProcessorEnum::undoDummyNodes);

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
        for(ElkEdge edge : graph.getContainedEdges()) {
            if(edge.getProperty(LayerBasedMetaDataProvider.OUTPUTS_EDGE_REVERSED)) {
                //TODO check if iterator complains
               reverseEdge(edge);
            }
        }
    }
    
    /**
     * @param edge the edge to reverse
     * 
     * This will remove edge form the graph and add a reversed version
     * This method copies all Properties and updates OUTPUTS_EDGE_REVERSED
     * */
    public static void reverseEdge(ElkEdge edge) {
        //get info for new edge
        ElkNode container = edge.getContainingNode();
        ElkNode source = (ElkNode) edge.getTargets().get(0);
        ElkNode target = (ElkNode) edge.getSources().get(0);
                
        //create new edge
        ElkEdge nEdge = ElkGraphUtil.createEdge(edge.getContainingNode());
        nEdge.getSources().add(source);
        nEdge.getTargets().add(target);
        
        //copy and update properties
        nEdge.copyProperties(edge);
        nEdge.setProperty(LayerBasedMetaDataProvider.OUTPUTS_EDGE_REVERSED,!edge.getProperty(LayerBasedMetaDataProvider.OUTPUTS_EDGE_REVERSED));
        
        //update graph
        container.getContainedEdges().add(nEdge);
        container.getContainedEdges().remove(edge);
        edge.getSections().clear();
        edge.getTargets().clear();
    }
    
    private static void placeDummyNode(ElkNode graph,IElkProgressMonitor monitor) {

        //TODO replace edges spanning more than one layer by dummies and sub-edges
    }
    
    private static void undoDummyNodes(ElkNode graph,IElkProgressMonitor monitor) {
        //TODO remove Dummy Nodes and re-route edges to/between/from dummy nodes
    }
    
}
