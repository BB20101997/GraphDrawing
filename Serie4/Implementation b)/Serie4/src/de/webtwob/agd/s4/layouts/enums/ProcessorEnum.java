package de.webtwob.agd.s4.layouts.enums;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.elk.core.alg.ILayoutProcessor;
import org.eclipse.elk.core.alg.ILayoutProcessorFactory;
import org.eclipse.elk.core.math.KVectorChain;
import org.eclipse.elk.core.util.ElkUtil;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

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
        // TODO e.g. convert hyper-edges to multiple simple-edges
        // maybe add an undo phase at the end
    }

    private static void undoCycleBreak(ElkNode graph, IElkProgressMonitor monitor) {
        for (ElkEdge edge : new LinkedList<>(graph.getContainedEdges())) {
            if (edge.getProperty(LayerBasedMetaDataProvider.OUTPUTS_EDGE_REVERSED)) {
                Util.reverseEdge(edge);
            }
        }
    }

    /**
     * Adds dummy nodes for edges spanning multiple layers
     */
    private static void placeDummyNode(ElkNode graph, IElkProgressMonitor monitor) {
        for (ElkEdge edge : new LinkedList<>(graph.getContainedEdges())) {

            ElkNode source = Util.getSource(edge);
            ElkNode target = Util.getTarget(edge);

            int sourceLayer = Util.getLayer(source);
            int targetLayer = Util.getLayer(target);

            int dir = (int) Math.signum(targetLayer - sourceLayer);

            if (Math.abs(sourceLayer - targetLayer) <= 1) {
                continue;
            }

            ElkNode dummy;

            for (int layer = sourceLayer + 1; layer * dir < targetLayer * dir; layer += dir) {

                // create dummy
                dummy = ElkGraphUtil.createNode(graph);
                dummy.copyProperties(source);
                dummy.setProperty(LayerBasedLayoutMetadata.OUTPUTS_IN_LAYER, layer);
                dummy.setProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY, true);

                // create dummy edge between last source and current dummy
                ElkEdge dummyEdge = ElkGraphUtil.createEdge(graph);
                dummyEdge.copyProperties(edge);
                dummyEdge.getSources().add(source);
                dummyEdge.getTargets().add(dummy);

                source = dummy;
            }

            // create edge between last dummy and target
            ElkEdge dummyEdge = ElkGraphUtil.createEdge(graph);
            dummyEdge.copyProperties(edge);
            dummyEdge.getSources().add(source);
            dummyEdge.getTargets().add(target);

            // remove old Edge
            Util.deleteEdge(edge);
        }
    }

    private static void undoDummyNodes(ElkNode graph, IElkProgressMonitor monitor) {

        // create re-route Edges that previously routed over dummy nodes
        List<ElkEdge> dummyEdges =
                graph.getChildren().stream().filter(n -> !n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY))
                        .flatMap(n -> n.getOutgoingEdges().stream())
                        .filter(e -> Util.getTarget(e).getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY))
                        .collect(Collectors.toList());

        dummyEdges.forEach(e -> {
            // Chain of points to route the edge over
            KVectorChain chain = new KVectorChain();

            final ElkNode target = Util.getTarget(e);
            final ElkNode source = Util.getSource(e);

            List<ElkEdgeSection> sections = e.getSections();

            // add starting point
            if (sections.isEmpty()) {
                chain.add(source.getX(), source.getY());
            } else {
                chain.add(sections.get(0).getStartX(), sections.get(0).getStartY());
            }

            ElkNode next = Util.getTarget(e);

            // add intermediate points
            while (next.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                chain.add(next.getX(), next.getY());

                next = Util.getTarget(next.getOutgoingEdges().get(0));
            }

            // add end point
            if (sections.isEmpty()) {
                chain.add(target.getX(), target.getY());
            } else {
                chain.add(sections.get(sections.size() - 1).getStartX(), sections.get(sections.size() - 1).getStartY());

            }
            // create new edge
            ElkEdge oldEdge = ElkGraphUtil.createSimpleEdge(source, target);
            ElkUtil.applyVectorChain(chain, ElkGraphUtil.firstEdgeSection(oldEdge, false, true));
        });

        // remove dummy nodes from graph
        new LinkedList<>(graph.getChildren()).stream()
                .filter(n -> n.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY))
                .forEach(node -> {
                    node.setParent(null);
                    Iterator<ElkEdge> it = ElkGraphUtil.allIncidentEdges(node).iterator();
                    while(it.hasNext()) {
                        ElkEdge edge = it.next();
                        it.remove();//so we don't cause a ConcurrentModificationException while deleting the edge
                        Util.deleteEdge(edge);
                    }
                });

    }

    private static void postProcess(ElkNode graph, IElkProgressMonitor monitor) {
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

    }

}
