package de.webtwob.agd.s4.layouts.impl.place;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

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

public class ConstraintNodePlace implements ILayoutPhase<LayoutPhasesEnum, ElkNode> {

    public static final YConstraint ZERO = new YConstraint(0);

    @Override
    public void process(ElkNode graph, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("ConstraintNodePlacePhase", 1);

        Map<Integer, List<ElkNode>> layers = Util.getLayers(graph);
        Map<ElkNode, YConstraint> yConstrained = new HashMap<>();

        double maxX = 0;
        double maxWidth;

        for (int i = 0; i < graph.getProperty(LayerBasedMetaDataProvider.OUTPUTS_LAYER_COUNT); i++) {
            if (progressMonitor.isCanceled()) {
                progressMonitor.done();
                return;
            }
            maxWidth = 0;
            List<ElkNode> layer = layers.getOrDefault(i, Collections.<ElkNode> emptyList());

            layer.sort(Util.COMPARE_POS_IN_LAYER);

            for (int l = 0; l < layer.size(); l++) {
                if (progressMonitor.isCanceled()) {
                    progressMonitor.done();
                    return;
                }
                ElkNode node = layer.get(l);

                if (node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                    ElkEdge edge = node.getIncomingEdges().get(0); //each dummy should have exactly 1 incomming edge
                        ElkNode prevDummy = Util.getSource(edge);
                        if (prevDummy.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                            YConstraint contr = yConstrained.get(prevDummy);
                            if (contr != null) { 
                                yConstrained.put(node, contr); // all connected dummy have the same yConstrained
                                contr.addNode(node);
                            }
                        
                    }
                }

                if (!yConstrained.containsKey(node) || yConstrained.get(node) == null) {
                    yConstrained.put(node, new YConstraint(ZERO));
                }

                YConstraint c = yConstrained.get(node);
                
                if (l != 0) {
                    ElkNode prev = layer.get(l - 1);
                    c.addConstrained(d -> d + prev.getHeight() + 20, yConstrained.get(prev));
                }
                
                c.addNode(node);
                

                node.setX(maxX);
                maxWidth = Math.max(maxWidth, node.getWidth());
            }

           

            for (ElkNode node : layer) {
                if (progressMonitor.isCanceled()) {
                    progressMonitor.done();
                    return;
                }
                if (node.getProperty(LayerBasedLayoutMetadata.OUTPUTS_IS_DUMMY)) {
                    node.setWidth(maxWidth);
                }
            }

            maxX += maxWidth + 20;
        }
        
        int size;
        while (!yConstrained.isEmpty()) {
            if (progressMonitor.isCanceled()) {
                progressMonitor.done();
                return;
            }
            size = yConstrained.size();
            yConstrained.entrySet().removeIf(e -> e.getValue().solve());
            if(size == yConstrained.size()) {
                break; //we have only cyclic constraints left, we just give up
            }
        }

        progressMonitor.done();

    }

    private static class YConstraint {

        private boolean solved = false;
        private double result;

        private Set<ElkNode> nodes = new HashSet<>();

        private Set<YConstraint> constraints = new HashSet<>();
        private Map<YConstraint, DoubleUnaryOperator> mapperMap = new HashMap<>();

        public YConstraint(double res) {
            solved = true;
            result = res;
        }

        public YConstraint(YConstraint requires) {
            addConstrained(requires);
        }

        public void addConstrained(DoubleUnaryOperator d, YConstraint requires) {
            if (solved) {
                throw new IllegalStateException("Can't add Constrained to solved Constrained");
            }
            constraints.add(requires);
            mapperMap.put(requires, d);
        }

        public void addConstrained(YConstraint constr) {
            if (solved) {
                throw new IllegalStateException("Can't add Constrained to solved Constrained");
            }
            constraints.add(constr);
        }

        public void addNode(ElkNode n) {
            nodes.add(n);
        }

        public boolean solve() {
            if (!solved && constraints.stream().allMatch(c -> c.solved)) {

                result = constraints.stream()
                        .mapToDouble(c -> 
                        mapperMap.getOrDefault(c, DoubleUnaryOperator.identity()).applyAsDouble(c.result)
                        )
                        .max().orElse(0);

                for (ElkNode node : nodes) {
                    node.setY(result);
                }

                solved = true;
            }

            return solved;
        }

    }

    @Override
    public LayoutProcessorConfiguration<LayoutPhasesEnum, ElkNode> getLayoutProcessorConfiguration(ElkNode graph) {
        return LayoutProcessorConfiguration.<LayoutPhasesEnum, ElkNode> create().before(LayoutPhasesEnum.CROSSING_MINIMIZATION).add(ProcessorEnum.DUMMY_PLACEMENT)
                .after(LayoutPhasesEnum.EDGE_ROUTING).add(ProcessorEnum.UNDO_DUMMY_PLACEMENT);
    }

}
