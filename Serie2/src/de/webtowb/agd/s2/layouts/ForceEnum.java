package de.webtowb.agd.s2.layouts;

public enum ForceEnum {
    
    DEFAULT{
        public double attractionForce(double distanced, double k){
            return distanced*distanced/k;
        }
        public double repulsionForce(double distance, double k) {
            return k*k/distance;
        }
    },
    LINEAR{

        @Override
        public double attractionForce(double distance, double k) {
            return distance/k;
        }

        @Override
        public double repulsionForce(double distance, double k) {
            return k/distance;
        }
        
    };
    
    /**
     * Absolute attraction Force based on Distance and optimal Distance k
     * */
    public abstract double attractionForce(double distance, double k);

    /**
     * Absolute repulsion Force based on Distance and optimal Distance k
     * */
    public abstract double repulsionForce(double distance, double k);

}
