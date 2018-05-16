package de.webtwob.agd.s4.layouts.interfaces.phases;

import java.util.function.Function;

import de.webtwob.agd.s4.layouts.enums.LayoutPhasesEnum;
import de.webtwob.agd.s4.layouts.interfaces.info.IGraphInfo;

//Base Interface for the LayoutPhases
public interface ILayoutPhase<I extends IGraphInfo ,O> extends Function<I, O>, org.eclipse.elk.core.alg.ILayoutPhase<LayoutPhasesEnum, O>{
    
}
