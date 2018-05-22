package de.webtwob.agd.s4.layouts.impl.place;

import java.util.Collections;
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

        for (int i = 0; i < graph.getProperty(LayerBasedMetaDataProvider.OUTPUTS_LAYER_COUNT); i++) {
            maxY = 0;
            maxWidth = 0;
            List<ElkNode> layer = layers.getOrDefault(i, Collections.<ElkNode> emptyList());
            layer.sort(Util.COMPARE_POS_IN_LAYER);
            for (ElkNode node : layer) {
                // Special case for dummy nodes
                if (node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                    Boolean set = false;
                    for (ElkEdge e : node.getIncomingEdges()) {
                        if (Util.getSource(e).getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                            // Has to be at the same Y as the last Dummy connected to
                            node.setY(Util.getSource(e).getY());
                            maxY = node.getY() +20; //TODO make the margin an option
                            set = true;
                        } 
                    }
                    if (!set) {
                        node.setY(maxY);
                        maxY += node.getHeight() + 20; // TODO make the margin an option
                    }
                    
                } else {
                    node.setY(maxY);
                    maxY += node.getHeight() + 20; // TODO make the margin an option
                }
                node.setX(maxX);
                maxWidth = Math.max(maxWidth, node.getWidth());
            }

            maxX += maxWidth + 20; // TODO make the margin an option

        }

    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create()
                .before(LayoutPhasesEnum.CROSSING_MINIMIZATION).add(ProcessorEnum.DUMMY_PLACEMENT)
                .after(LayoutPhasesEnum.EDGE_ROUTING).add(ProcessorEnum.UNDO_DUMMY_PLACEMENT);
    }

}
