package de.webtwob.agd.s4.layouts.impl.place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.enums.ProcessorEnum;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class WorkingDummyNodePlacementPhase implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    //our second NodePlacementPhase Implementation
    
    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("WorkingDummyNodePlacementPhase", graph.getChildren().size());
        Map<Integer, List<ElkNode>> layers = Util.getLayers(graph);

        double maxX = 0;
        double maxY = 0;
        double maxWidth = 0;
        ArrayList<LinkedList<ElkNode>> dummyNodePositions = new ArrayList<LinkedList<ElkNode>>();
        ArrayList<LinkedList<ElkNode>> dummyNodeConnections = new ArrayList<LinkedList<ElkNode>>();

        for (int i = 0; i < graph.getProperty(LayerBasedMetaDataProvider.OUTPUTS_LAYER_COUNT); i++) {
            maxY = 0;
            maxWidth = 0;
            List<ElkNode> layer = layers.getOrDefault(i, Collections.<ElkNode> emptyList());
            ArrayList<LinkedList<ElkNode>> dummyNodePositionsInLayer = new ArrayList<LinkedList<ElkNode>>();
            

            layer.sort(Util.COMPARE_POS_IN_LAYER);

            for (ElkNode node : layer) {
                // Special case for dummy nodes
                if (node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                    Boolean set = false;

                    for (ElkEdge e : node.getIncomingEdges()) {
                        if (Util.getSource(e).getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {

                            // Has to be at the same Y as the last Dummy connected to
                            if (Util.getSource(e).getY() < maxY) {
                                node.setY(maxY);
                                for (LinkedList<ElkNode> dummylines : dummyNodeConnections) {
                                    if (dummylines.peekLast() == Util.getSource(e)) {
                                        changeAllBefore(dummylines, dummyNodePositions, dummyNodeConnections, maxY);
                                    }
                                }
                                maxY += node.getHeight() + 20;
                            } else {
                                node.setY(Util.getSource(e).getY());
                                maxY = node.getY() + 20; // TODO make the margin an optiopn
                            }
                            set = true;
                            for (LinkedList<ElkNode> dummylines : dummyNodeConnections) {
                                if (dummylines.peekLast() == Util.getSource(e)) {
                                    dummylines.add(node);
                                }
                            }
                        }
                    }
                    if (!set) {
                        node.setY(maxY);
                        maxY += node.getHeight() + 20; // TODO make the margin an option
                        LinkedList<ElkNode> l = new LinkedList<ElkNode>();
                        l.add(node);
                        dummyNodeConnections.add(l);
                    }
                    dummyNodePositionsInLayer.add(new LinkedList<ElkNode>());

                } else {
                    node.setY(maxY);
                    maxY += node.getHeight() + 20; // TODO make the margin an option
                }
                node.setX(maxX);
                maxWidth = Math.max(maxWidth, node.getWidth());
                for (LinkedList<ElkNode> following : dummyNodePositionsInLayer) {
                    following.add(node);
                }
            }

            for (ElkNode node : layer) {
                if (node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                    node.setWidth(maxWidth);
                }
                progressMonitor.worked(1);
            }
            dummyNodePositions.addAll(dummyNodePositionsInLayer);
            maxX += maxWidth + 20; // TODO make the margin an option

        }
        
        progressMonitor.done();

    }

    /**
     * Go every connected node of the dummynode that has already been places once and move them
     * 
     * @param dummylines
     *            The dummy nodes that make up one connection from two nodes
     * @param dummyNodePositions
     *            List of a list of dummy nodes and the nodes that come in the same layer afterwards
     * @param dummyNodeConnetions
     *            List of all connections that are made of dummy nodes
     * @param maxY
     */
    private void changeAllBefore(LinkedList<ElkNode> dummylines, ArrayList<LinkedList<ElkNode>> dummyNodePositions,
            ArrayList<LinkedList<ElkNode>> dummyNodeConnetions, double maxY) {

        // Look at all nodes from the dummyline from the end to the beginning
        for (int i = dummylines.size() - 1; i >= 0; i--) {

            // If another dummy node hasn't already moved that node move it
            if (dummylines.get(i).getY() < maxY) {
                dummylines.get(i).setY(maxY);
            

            // Find the List that shows what is behind the current dummynode
            for (LinkedList<ElkNode> dummyAndAfter : dummyNodePositions) {
                if (dummyAndAfter.peekFirst() == dummylines.get(i)) {
                    // Change the nodes in the layer of the dummynode
                    changeLowerOfTheNode(dummylines.get(i), dummyAndAfter, dummyNodePositions, dummyNodeConnetions,
                            maxY);
                }
            }
            }
        }
    }

    private void changeLowerOfTheNode(ElkNode elkNode, LinkedList<ElkNode> dummyAndAfter,
            ArrayList<LinkedList<ElkNode>> dummyNodePositions, ArrayList<LinkedList<ElkNode>> dummyNodeConnetions,
            double maxY) {
        for (ElkNode node : dummyAndAfter) {
            // Change the first node and the normal nodes normal
            if (!node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY) || node == dummyAndAfter.peekFirst()) {
                if (node.getY() < maxY) {
                    node.setY(maxY);
                    maxY += node.getHeight() + 20; // TODO make the margin an option
                } else {
                    // If another dummyline has changed the Y already
                    maxY = node.getY() + node.getHeight() + 20; // TODO make the margin an option
                }
            } else {
                // If it is part of a Dummyline change the Line
                for (LinkedList<ElkNode> l : dummyNodeConnetions) {
                    if (l.peekLast() == node) {
                        changeAllBefore(l, dummyNodePositions, dummyNodeConnetions, maxY);

                    }
                }
            }
        }
    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create()
                .before(LayoutPhasesEnum.CROSSING_MINIMIZATION).add(ProcessorEnum.DUMMY_PLACEMENT)
                .after(LayoutPhasesEnum.EDGE_ROUTING).add(ProcessorEnum.UNDO_DUMMY_PLACEMENT);
    }

}
