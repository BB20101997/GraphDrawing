package de.webtowb.agd.s2.layouts;

import org.eclipse.elk.core.AbstractLayoutProvider;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

public class FruchtermanReingoldLayoutProvider extends AbstractLayoutProvider {

    @Override
    public void layout(ElkNode layoutGraph, IElkProgressMonitor progressMonitor) {
        try {
            progressMonitor.begin("Begin GraphLayout", IElkProgressMonitor.UNKNOWN_WORK);
            
            
        }finally {
            progressMonitor.done();
        }
    }
    

}
