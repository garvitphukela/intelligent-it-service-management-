package com.itsm.service;

import com.itsm.assignment.AssignmentStrategy;
import com.itsm.assignment.SmartRecommendationStrategy;
import com.itsm.domain.settings.SystemSetting;
import com.itsm.exception.InvalidInputException;
import com.itsm.priority.PriorityStrategy;
import com.itsm.priority.WeightedScoringPriorityStrategy;
import com.itsm.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Holds every available strategy (discovered by Spring) and the administrator's current
 * choice, persisted in {@code system_settings}. Swapping strategy is a configuration change,
 * not a code change.
 */
@Service
public class StrategyRegistry {

    public static final String ASSIGNMENT_KEY = "assignment.strategy";
    public static final String PRIORITY_KEY = "priority.strategy";
    public static final String AUTO_ASSIGN_KEY = "assignment.auto";

    private final Map<String, AssignmentStrategy> assignment = new LinkedHashMap<>();
    private final Map<String, PriorityStrategy> priority = new LinkedHashMap<>();
    private final SystemSettingRepository settings;

    public StrategyRegistry(List<AssignmentStrategy> assignmentStrategies, List<PriorityStrategy> priorityStrategies,
                            SystemSettingRepository settings) {
        assignmentStrategies.forEach(s -> assignment.put(s.key(), s));
        priorityStrategies.forEach(s -> priority.put(s.key(), s));
        this.settings = settings;
    }

    public AssignmentStrategy activeAssignment() {
        return assignment.getOrDefault(get(ASSIGNMENT_KEY, SmartRecommendationStrategy.KEY), assignment.get(SmartRecommendationStrategy.KEY));
    }

    public PriorityStrategy activePriority() {
        return priority.getOrDefault(get(PRIORITY_KEY, WeightedScoringPriorityStrategy.KEY), priority.get(WeightedScoringPriorityStrategy.KEY));
    }

    public AssignmentStrategy assignment(String key) {
        AssignmentStrategy s = assignment.get(key);
        if (s == null) throw new InvalidInputException("Unknown assignment strategy: " + key);
        return s;
    }

    public boolean autoAssignEnabled() {
        return Boolean.parseBoolean(get(AUTO_ASSIGN_KEY, "true"));
    }

    public Collection<AssignmentStrategy> assignmentStrategies() { return assignment.values(); }

    public Collection<PriorityStrategy> priorityStrategies() { return priority.values(); }

    @Transactional
    public void update(String assignmentKey, String priorityKey, Boolean autoAssign) {
        if (assignmentKey != null) {
            if (!assignment.containsKey(assignmentKey)) throw new InvalidInputException("Unknown assignment strategy: " + assignmentKey);
            put(ASSIGNMENT_KEY, assignmentKey);
        }
        if (priorityKey != null) {
            if (!priority.containsKey(priorityKey)) throw new InvalidInputException("Unknown priority strategy: " + priorityKey);
            put(PRIORITY_KEY, priorityKey);
        }
        if (autoAssign != null) put(AUTO_ASSIGN_KEY, autoAssign.toString());
    }

    private String get(String key, String fallback) {
        return settings.findById(key).map(SystemSetting::getValue).orElse(fallback);
    }

    private void put(String key, String value) {
        SystemSetting s = settings.findById(key).orElse(new SystemSetting(key, value));
        s.setValue(value);
        settings.save(s);
    }
}
