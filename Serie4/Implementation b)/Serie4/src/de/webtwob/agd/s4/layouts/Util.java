package de.webtwob.agd.s4.layouts;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class Util {
    
    public static final Comparator<ElkNode> COMPARE_POS_IN_LAYER = Comparator.<ElkNode>comparingInt((ElkNode n) ->n.getProperty(LayerBasedMetaDataProvider.OUTPUTS_POS_IN_LAYER));
    
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
    
    /**
     * Assumes Simple Edges
     * 
     * Returns true iff the source of all incoming edges have an assigned layer
     * */
    public static boolean allPredecessorsHaveAnAssignedLayer(ElkNode n) {
        return n.getIncomingEdges()
                .stream()
                .flatMap(e->e.getSources().stream())
                .noneMatch(s->(s.getProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER)==-1)&&(n.getParent()==ElkGraphUtil.connectableShapeToNode(s).getParent()));
    }
    
    /**
     * Assumes Layers to be Assigned
     * 
     * Returns a Map of Layers
     * */
    
    public static Map<Integer, List<ElkNode>> getLayers(ElkNode graph){
        return graph.getChildren().stream()
                .collect(Collectors.groupingBy((ElkNode n)->n.getProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER)));
    }
    
    /**
     * Assumes Layers to be Assigned
     * 
     * Returns the Layer of the Node
     * */
    
    public static int getLayer(ElkNode node){
           return node.getProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER);
    }
    
    /**
     * A version of ElkGraphUtil.getTargetNode which doesn't throw if more than one Target is present
     * */
    public static ElkNode getTarget(ElkEdge edge) {
        if(edge.getTargets().size()<1) {
            throw new IllegalArgumentException("Passed Egde does not have any Targets!");
        }
        
        return ElkGraphUtil.connectableShapeToNode(edge.getTargets().get(0));
        
    }
    
    /**
     * A version of ElkGraphUtil.getSourceNode which doesn't throw if more than one Target is present
     * */
    public static ElkNode getSource(ElkEdge edge) {
        if(edge.getSources().size()<1) {
            throw new IllegalArgumentException("Passed Egde does not have any Sources!");
        }
        
        return ElkGraphUtil.connectableShapeToNode(edge.getSources().get(0));
        
    }

}
