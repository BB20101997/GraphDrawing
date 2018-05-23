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

    // This does not accommodate for dummy's being on the same y-Level!

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {

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

            layer.sort(Util.COMPARE_POS_IN_LAYER);
            ArrayList<LinkedList<ElkNode>> dummyNodePositionsinLayer = new ArrayList<LinkedList<ElkNode>>();

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
                                maxY = node.getY() + 20; // TODO make the margin an option
                            }
                            set = true;
                        }
                    }
                    if (!set) {
                        node.setY(maxY);
                        maxY += node.getHeight() + 20; // TODO make the margin an option
                    }
                    dummyNodePositionsinLayer.add(new LinkedList<ElkNode>());

                } else {
                    node.setY(maxY);
                    maxY += node.getHeight() + 20; // TODO make the margin an option
                }
                node.setX(maxX);
                maxWidth = Math.max(maxWidth, node.getWidth());
                for (LinkedList<ElkNode> following : dummyNodePositionsinLayer) {
                    following.add(node);
                }
            }
            
            for (ElkNode node : layer) {
                if (node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                    node.setWidth(maxWidth);
                }
            }

            maxX += maxWidth + 20; // TODO make the margin an option

        }

    }

    /**
     * Go every connected dummynode
     * 
     * @param dummylines
     * @param dummyNodePositions
     * @param dummyNodeConnetions
     * @param maxY
     */
    private void changeAllBefore(LinkedList<ElkNode> dummylines, ArrayList<LinkedList<ElkNode>> dummyNodePositions,
            ArrayList<LinkedList<ElkNode>> dummyNodeConnetions, double maxY) {
        
        for (int i = dummylines.size() - 1; i >= 0; i--) {
            
            // Falls eine andere Dummyline diese bereits nach oben geschoben hat.
            if (dummylines.get(i).getY()<maxY) {
            dummylines.get(i).setY(maxY);
            } 
        
        for (LinkedList<ElkNode> dummyAndAfter : dummyNodePositions) {
            if (dummyAndAfter.peekFirst() == dummylines.get(i)) {
                changeLowerOfTheNode(dummylines.get(i), dummyAndAfter, dummyNodePositions, dummyNodeConnetions, maxY);
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
                if (node.getY()<maxY) {
                node.setY(maxY);
                maxY += node.getHeight() + 20; // TODO make the margin an option
                } else {
                    // If another dummyline has changed the Y already
                    maxY = node.getY() + node.getHeight() + 20;
                }
            } else {
                //If it is part of a Dummyline change the Line 
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
