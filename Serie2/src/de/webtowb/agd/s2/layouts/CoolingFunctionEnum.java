package de.webtowb.agd.s2.layouts;

import org.eclipse.elk.graph.ElkNode;

import de.webtwob.adg.s2.layouts.options.FruchtermanReingoldOptions;

public enum CoolingFunctionEnum {

    LINEAR{
        @Override
        public double temperature(ElkNode layoutGraph, int iteration) {
              double width = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_WIDTH);
              double height = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_HEIGHT);
              int iterations = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_ITERATIONS);
              return (Math.min(width, height) / 10) * (1  - iteration/iterations);
        }
    },
    QUENCH_AND_SIMMER{

        @Override
        public double temperature(ElkNode layoutGraph, int iteration) {
            double width = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_WIDTH);
            double height = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_HEIGHT);
            int iterations = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_ITERATIONS);
            //TODO option for ratio quenching/simmer
            if(iteration < iterations/2) {
                //quench, steady and rapidly
                //TODO option for constant 2
                return (Math.min(width, height) / 2) * (1  - iteration/iterations*2);
            }else {
                //simmer, constant low
                return layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_SIMMER_VALUE);
            }
        }

    }
    ;

    abstract public double temperature(ElkNode layoutGraph, int iteration) ;


}
