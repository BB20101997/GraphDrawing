package de.webtwob.agd.s4.layouts.enums;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.alg.ILayoutProcessorFactory;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public enum ProcessorEnum implements ILayoutProcessorFactory<ElkNode> {

    // before Cycle Break Phase
    INIT(ProcessorEnum::prepareGraph),
    // after Layer Assignment

    // after LayerAssignement Phase
    UNDO_CYCLE_BREAK(ProcessorEnum::undoCycleBreak),

    // before Crossing Minimization
    DUMMY_PLACEMENT(ProcessorEnum::placeDummyNode),

    // after Edge Route Phase
    UNDO_DUMMY_PLACEMENT(ProcessorEnum::undoDummyNodes),

    // after Edge Route Phase
    POST(ProcessorEnum::postProcess);

    private final ILayoutProcessor<ElkNode> processor;

    private ProcessorEnum(ILayoutProcessor<ElkNode> node) {
        processor = node;
    }

    @Override
    public ILayoutProcessor<ElkNode> create() {
        return processor;
    };

    private static void prepareGraph(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("InitProcessor", 1);
        
        // TODO e.g. convert hyper-edges to multiple simple-edges
        if (graph.getContainedEdges().stream()
                .anyMatch(e -> (e.getSources().size() != 1) || (e.getTargets().size() != 1))) {
            System.err.println("None Simple Edge!");
        }
        
        monitor.done();
    }

    /**
     * Reverses all Edges marked as reversed
     * */
    private static void undoCycleBreak(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("UndoCycleBreakProcessor", graph.getContainedEdges().size());
        
        for (ElkEdge edge : new LinkedList<>(graph.getContainedEdges())) {
            if (edge.getProperty(LayerBasedMetaDataProvider.OUTPUTS_EDGE_REVERSED)) {
                Util.reverseEdge(edge);
            }
            monitor.worked(1);
        }
        
        monitor.done();
    }

    /**
     * Adds dummy nodes for edges spanning multiple layers
     */
    private static void placeDummyNode(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("DummyPlaceProcessor", 1);
        
        for (ElkEdge edge : new LinkedList<>(graph.getContainedEdges())) {
            Util.breakUpEdge(edge);
        }
        
        monitor.done();
    }

    private static void undoDummyNodes(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("RemoveDummyNodesProcessor", 1);
        
        // create re-route Edges that previously routed over dummy nodes
        List<ElkEdge> origEdges =
                graph.getChildren().stream()
                        .filter(n -> !n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) //only consider none Dummy Nodes
                        .flatMap(n -> n.getOutgoingEdges().stream()) //look at all OutgoingEdges
                        .filter(e -> Util.getTarget(e).getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) //which target Dummy Nodes
                        .collect(Collectors.toList());
        
        origEdges.forEach(Util::restoreBrokenEdge);

        // remove dummy nodes and edges from graph 
        graph.getChildren().forEach(n->{
            n.getOutgoingEdges().removeIf(e->e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
            n.getIncomingEdges().removeIf(e->e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
            });
        
        graph.getChildren().removeIf(n->n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
        graph.getContainedEdges().removeIf(e->e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
        
       
        
        monitor.done();

    }

    private static void postProcess(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("PostPhase",   1);
        
        double minX = 0, minY = 0, maxX = 0, maxY = 0;
        for (ElkNode node : graph.getChildren()) {
            // TODO make margin configurable (and take labels into account)
            minX = Math.min(minX, node.getX() - 20);
            minY = Math.min(minY, node.getY() - 20);
            maxX = Math.max(maxX, node.getX() + node.getWidth() + 20);
            maxY = Math.max(maxY, node.getY() + node.getHeight() + 20);
        }

        for (ElkNode node : graph.getChildren()) {
            node.setX(node.getX() - minX);
            node.setY(node.getY() - minY);
        }

        graph.setDimensions(maxX - minX, maxY - minY);

        monitor.done();
    }

}
