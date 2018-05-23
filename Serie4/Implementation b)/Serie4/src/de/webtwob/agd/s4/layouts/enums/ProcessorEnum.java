package de.webtwob.agd.s4.layouts.enums;

import java.util.LinkedList;
import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.alg.ILayoutProcessorFactory;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
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
        if (graph.getContainedEdges().stream().anyMatch(e -> (e.getSources().size() != 1) || (e.getTargets().size() != 1))) {
            System.err.println("None Simple Edge!");
        }

        monitor.done();
    }

    /**
     * Reverses all Edges marked as reversed
     */
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

        new LinkedList<>(graph.getContainedEdges()).forEach(Util::restoreBrokenEdge);

        // remove dummy nodes and edges from graph
        graph.getChildren().forEach(n -> {
            n.getOutgoingEdges().removeIf(e -> e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
            n.getIncomingEdges().removeIf(e -> e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
        });

        graph.getChildren().removeIf(n -> n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));
        graph.getContainedEdges().removeIf(e -> e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));

        monitor.done();

    }

    private static void postProcess(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("PostPhase", 1);

        ElkPadding pad = graph.getProperty(LayerBasedLayoutMetadata.PADDING);

        double minX = 0, minY = 0, maxX = 0, maxY = 0;
        for (ElkNode node : graph.getChildren()) {
            // TODO make padding configurable (and take labels into account)
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX() + node.getWidth());
            maxY = Math.max(maxY, node.getY() + node.getHeight());
        }

        for (ElkEdge edge : graph.getContainedEdges()) {
            for (ElkEdgeSection sect : edge.getSections()) {
                minX = Math.min(minX, sect.getStartX());
                minY = Math.min(minY, sect.getStartY());
                maxX = Math.max(maxX, sect.getStartX());
                maxY = Math.max(maxY, sect.getStartY());
                minX = Math.min(minX, sect.getEndX());
                minY = Math.min(minY, sect.getEndY());
                maxX = Math.max(maxX, sect.getEndX());
                maxY = Math.max(maxY, sect.getEndY());

                for (ElkBendPoint bp : sect.getBendPoints()) {
                    minX = Math.min(minX, bp.getX());
                    minY = Math.min(minY, bp.getY());
                    maxX = Math.max(maxX, bp.getX());
                    maxY = Math.max(maxY, bp.getY());
                }
            }
        }
        
        final double offsetX = pad.left-minX;
        final double offsetY = pad.top-minY;
        
        for (ElkNode node : graph.getChildren()) {
            node.setX(node.getX() + offsetX);
            node.setY(node.getY() + offsetY);
        }

        for (ElkEdge edge : graph.getContainedEdges()) {
            edge.getSections().forEach(sect -> {
                sect.setStartLocation(sect.getStartX() + offsetX, sect.getStartY() +offsetY);
                sect.setEndLocation(sect.getEndX() + offsetX, sect.getEndY() + offsetY);
                sect.getBendPoints().forEach(bend -> {
                    bend.set(bend.getX() +offsetX, bend.getY() + offsetY);
                });
            });
        }

        graph.setDimensions(maxX - minX+pad.getHorizontal(), maxY - minY+pad.getVertical());

        monitor.done();
    }

}
