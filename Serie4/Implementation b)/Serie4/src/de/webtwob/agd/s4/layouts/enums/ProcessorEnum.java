package de.webtwob.agd.s4.layouts.enums;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
        monitor.begin("RemoveDummyNodesProcessor", graph.getContainedEdges().size() * 2 + graph.getChildren().size() * 2);

        IElkProgressMonitor sub = monitor.subTask(graph.getContainedEdges().size());
        List<ElkEdge> dummyEdges = new ArrayList<>(graph.getContainedEdges().size());

        sub.begin("UnbreakDummyEdges", graph.getContainedEdges().size());

        for (ElkEdge edge : graph.getContainedEdges()) {
            if (edge.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                dummyEdges.add(edge);
            } else {
                Util.restoreBrokenEdge(edge);
            }
            sub.worked(1);
        }

        sub.done();

        dummyEdges.forEach(e -> graph.getContainedEdges().remove(e));
        graph.getChildren().removeIf(e -> e.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY));

        monitor.done();

    }

    private static void postProcess(ElkNode graph, IElkProgressMonitor monitor) {
        monitor.begin("PostPhase", 1);

        if (!graph.getChildren().isEmpty()) {

            ElkPadding pad = graph.getProperty(LayerBasedLayoutMetadata.PADDING);

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
            for (ElkNode node : graph.getChildren()) {

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

            /*
             * // TODO maybe account for labels
             * 
             * double minLabelX = Double.MAX_VALUE; double minLabelY = Double.MAX_VALUE; double maxlabelX = Double.MIN_VALUE; double
             * maxlabelY = Double.MIN_VALUE;
             * 
             * for (ElkLabel label : graph.getLabels()) { // label.getProperty()//TODO }
             */

            final double offsetX = pad.left - minX;
            final double offsetY = pad.top - minY;

            for (ElkNode node : graph.getChildren()) {
                node.setX(node.getX() + offsetX);
                node.setY(node.getY() + offsetY);
            }

            for (ElkEdge edge : graph.getContainedEdges()) {
                edge.getSections().forEach(sect -> {
                    sect.setStartLocation(sect.getStartX() + offsetX, sect.getStartY() + offsetY);
                    sect.setEndLocation(sect.getEndX() + offsetX, sect.getEndY() + offsetY);
                    sect.getBendPoints().forEach(bend -> {
                        bend.set(bend.getX() + offsetX, bend.getY() + offsetY);
                    });
                });
            }

            graph.setDimensions(maxX - minX + pad.getHorizontal(), maxY - minY + pad.getVertical());
        }

        monitor.done();
    }

}
