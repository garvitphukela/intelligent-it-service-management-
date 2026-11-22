package com.itsm.domain.incident;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps the persisted {@link IncidentStatus} to its (stateless, shared) state object.
 * States are stateless so a single instance per status (Flyweight) is safe.
 */
public final class IncidentStateFactory {

    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        register(new NewState());
        register(new ClassifiedState());
        register(new AssignedState());
        register(new AcknowledgedState());
        register(new InProgressState());
        register(new AwaitingInfoState());
        register(new ResolvedState());
        register(new VerifiedState());
        register(new ClosedState());
        register(new CancelledState());
    }

    private IncidentStateFactory() {}

    private static void register(IncidentState s) { STATES.put(s.status(), s); }

    public static IncidentState forStatus(IncidentStatus status) {
        IncidentState s = STATES.get(status);
        if (s == null) throw new IllegalStateException("No state registered for " + status);
        return s;
    }
}
