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
              double scale = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_TEMPERATURE_SCALE);
              return scale * Math.min(width, height) * (1  - iteration/iterations);
        }
    },
    QUENCH_AND_SIMMER{

        @Override
        public double temperature(ElkNode layoutGraph, int iteration) {
            double width = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_WIDTH);
            double height = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_FRAME_HEIGHT);
            int iterations = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_ITERATIONS);
            double scale = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_TEMPERATURE_SCALE);
            double ratio = layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_QUENCH_SIMMER_RATIO);
            if(iteration < iterations * ratio) {
                //quench, steady and rapidly
                //if ratio is 0 we will never enter this case therefore it's save to divide by ratio
                return scale * Math.min(width, height)  * (1  - iteration/iterations/ratio);
            }else {
                //simmer, constant low
                return layoutGraph.getProperty(FruchtermanReingoldOptions.SETTINGS_SIMMER_VALUE);
            }
        }

    }
    ;

    abstract public double temperature(ElkNode layoutGraph, int iteration) ;


}
