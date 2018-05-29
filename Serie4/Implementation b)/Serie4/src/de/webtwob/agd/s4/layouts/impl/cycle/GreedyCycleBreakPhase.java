package de.webtwob.agd.s4.layouts.impl.cycle;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;

public class GreedyCycleBreakPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("GreedyCycleBreakPhase", graph.getChildren().size()+graph.getContainedEdges().size());
        //order nodes
        
        //copy childlist so we can remove already sorted ones
        List<ElkNode> children = new LinkedList<>(graph.getChildren());
        
        //sources at the beginning add to the end
        LinkedList<ElkNode> sourceList = new LinkedList<>();
        //sinks at the end add to the beginning
        LinkedList<ElkNode> sinkList = new LinkedList<>();
        
        while(!children.isEmpty()) {
            boolean found;
            
            //sort out source
            do {
                found = false;
                for(Iterator<ElkNode> iter = children.iterator();iter.hasNext();) {
                    ElkNode node = iter.next();
                    //is node a Source given the currently present nodes in children
                    if(node.getIncomingEdges().parallelStream().map(Util::getSource).noneMatch(children::contains)) {
                        sourceList.addLast(node);
                        iter.remove(); //avoid ConcurrentModificationException
                        found = true;
                        progressMonitor.worked(1);
                    }
                    
                }
                
            }while(found);//stop when an iteration didn't found sinks
            
            //sort out sink
            do {
                found = false;
                for(Iterator<ElkNode> iter = children.iterator();iter.hasNext();) {
                    ElkNode node = iter.next();
                    
                    //is node a Source given the currently present nodes in children
                    if(node.getOutgoingEdges().parallelStream().map(Util::getTarget).noneMatch(children::contains)) {
                        sinkList.addFirst(node);
                        iter.remove(); //avoid ConcurrentModificationException
                        found = true;
                        progressMonitor.worked(1);
                    }
                    
                }
                
            }while(found);//stop when an iteration didn't found sinks
            
            //find edge with max in-degree to out-degree difference
            ElkNode maxNode = null;
            int maxDiff = Integer.MIN_VALUE;
            for(Iterator<ElkNode> iter = children.iterator();iter.hasNext();) {
                ElkNode curNode = iter.next();
                int curVal = curNode.getOutgoingEdges().size()-curNode.getIncomingEdges().size();
                if(curVal>maxDiff) {
                    maxDiff = curVal;
                    maxNode = curNode;
                }
            }
            
            //if we still had nodes add the one with max out to in diff to source list
            if(maxNode!=null) {
                sourceList.addFirst(maxNode);
                children.remove(maxNode);
                progressMonitor.worked(1);
            }
            
        }
        
        //remove cycles
        List<ElkNode> combinedList = new LinkedList<>();
        combinedList.addAll(sourceList);
        combinedList.addAll(sinkList);

        graph.getContainedEdges().stream().forEach(e -> {
            // reverse all edges where the source Node index is higher than the target node index
            if (combinedList.indexOf(Util.getSource(e)) > combinedList.indexOf(Util.getTarget(e))) {
                Util.reverseEdge(e);
            }
            progressMonitor.worked(1);
        });
        
        progressMonitor.done();
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> conf =
                LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create()
                .before(LayoutPhasesEnum.EDGE_ROUTING)
                    .add(ProcessorEnum.UNDO_CYCLE_BREAK);
        return conf;
    }

}
