package de.webtwob.agd.s4.layouts.impl.layer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.elk.core.alg.ILayoutPhase;
import org.eclipse.elk.core.alg.LayoutProcessorConfiguration;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import de.webtwob.agd.s4.layouts.LayerBasedLayoutMetadata;
import de.webtwob.agd.s4.layouts.Util;
import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.options.LayerBasedMetaDataProvider;

public class LongestPathLayerAssignement implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        /*
         * we start at the sources so we can skip finding the longest path which we would need to know the index of the last
         * layer
         */

        List<ElkNode> notAssigned = new LinkedList<>(graph.getChildren());

        int layerCount = notAssigned.isEmpty() ? 0 : 1;

        // Assign all sources to layer 0
        for (Iterator<ElkNode> iter = notAssigned.iterator(); iter.hasNext();) {
            ElkNode cur = iter.next();
            if (cur.getIncomingEdges().isEmpty()) {
                iter.remove();
                cur.setProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER, 0);
            }
        }

        // Assign all Nodes who's predecessor have been assigned a layer to the max + 1
        while (!notAssigned.isEmpty()) { // continue till all nodes have a layer
            out: for (Iterator<ElkNode> iter = notAssigned.iterator(); iter.hasNext();) {
                ElkNode cur = iter.next();
                int maxLayer = 0;
                // find the max layer of all predecessors
                for (ElkEdge e : cur.getIncomingEdges()) {
                    ElkNode n = Util.getSource(e);
                    int curLayer = n.getProperty(LayerBasedMetaDataProvider.OUTPUTS_IN_LAYER);
                    if (curLayer == -1) {
                        // a predessecor was unassigned try next node
                        continue out;
                    } else {
                        maxLayer = Math.max(maxLayer, curLayer);
                    }
                }
                iter.remove(); // we don't want no ConcurrentModificationException
                
                cur.setProperty(LayerBasedLayoutMetadata.OUTPUTS_IN_LAYER, maxLayer + 1);
                
                layerCount = Math.max(layerCount, maxLayer + 2);
            }
        }

        graph.setProperty(LayerBasedMetaDataProvider.OUTPUTS_LAYER_COUNT, layerCount);

    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create();
    }

}
