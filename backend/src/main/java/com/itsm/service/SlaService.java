package com.itsm.service;

import com.itsm.config.ItsmProperties;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.exception.SlaConfigurationMissingException;
import com.itsm.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/** Finds the applicable SLA (service-specific first, then default) and starts the incident's clock. */
@Service
public class SlaService {

    private final SlaPolicyRepository policies;
    private final ItsmProperties props;

    public SlaService(SlaPolicyRepository policies, ItsmProperties props) {
        this.policies = policies;
        this.props = props;
    }

    public SlaPolicy resolvePolicy(ITService service, Priority priority) {
        return findPolicy(service, priority).orElseThrow(() -> new SlaConfigurationMissingException(
                "No SLA policy configured for " + priority + (service == null ? "" : " on " + service.getName())
                        + ". Ask an administrator to configure SLA policies."));
    }

    public Optional<SlaPolicy> findPolicy(ITService service, Priority priority) {
        Optional<SlaPolicy> specific = service == null || service.getId() == null ? Optional.empty()
                : policies.findByServiceIdAndPriority(service.getId(), priority);
        return specific.or(() -> policies.findByServiceIsNullAndPriority(priority));
    }

    public void apply(Incident incident) {
        SlaPolicy policy = resolvePolicy(incident.getService(), incident.getPriority());
        incident.applySla(policy, scaled(policy.responseTarget()), scaled(policy.resolutionTarget()));
    }

    /** Wall-clock duration for a policy duration, honouring the demo time-scale. */
    public Duration scaled(Duration policyDuration) {
        double scale = props.getSla().getTimeScale();
        return Duration.ofMillis(Math.max(1000, (long) (policyDuration.toMillis() / scale)));
    }

    public double timeScale() { return props.getSla().getTimeScale(); }
}
