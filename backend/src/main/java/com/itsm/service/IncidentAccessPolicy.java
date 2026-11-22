package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.exception.UnauthorizedOperationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Role- and ownership-based authorisation rules for incidents, kept in one place
 * (single responsibility) instead of being scattered through controllers.
 */
@Component
public class IncidentAccessPolicy {

    public boolean canView(User u, Incident i) {
        return u.getRole().isStaff() || isReporter(u, i);
    }

    public boolean canSeeInternalNotes(User u) { return u.getRole().isStaff(); }

    public boolean canEdit(User u, Incident i) {
        if (i.getStatus().isTerminal()) return false;
        if (u.getRole().isManagement() || u.getRole() == Role.TEAM_LEAD || isAssignee(u, i)) return true;
        return isReporter(u, i) && EnumSet.of(IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED).contains(i.getStatus());
    }

    public boolean canAssign(User u) { return u.getRole().isManagement() || u.getRole() == Role.TEAM_LEAD; }

    /** Acknowledge / start / request info / resolve. */
    public boolean canWork(User u, Incident i) {
        return isAssignee(u, i) || u.getRole() == Role.TEAM_LEAD || u.getRole().isManagement();
    }

    public boolean canProvideInfo(User u, Incident i) { return isReporter(u, i) || u.getRole().isStaff(); }

    public boolean canVerify(User u, Incident i) { return isReporter(u, i) || u.getRole().isManagement(); }

    public boolean canClose(User u, Incident i) {
        return isAssignee(u, i) || u.getRole() == Role.TEAM_LEAD || u.getRole().isManagement();
    }

    public boolean canCancel(User u, Incident i) { return isReporter(u, i) || u.getRole().isManagement(); }

    public boolean canGiveFeedback(User u, Incident i) { return isReporter(u, i); }

    public boolean canLinkDuplicates(User u) { return u.getRole().isStaff(); }

    public void require(boolean allowed, String operation) {
        if (!allowed) throw new UnauthorizedOperationException("You are not allowed to " + operation);
    }

    /** Actions the UI should offer: permission AND state-machine legality. */
    public List<String> allowedActions(User u, Incident i) {
        List<String> a = new ArrayList<>();
        if (i.can(IncidentAction.ASSIGN) && canAssign(u)) a.add("ASSIGN");
        if (i.can(IncidentAction.ACKNOWLEDGE) && canWork(u, i)) a.add("ACKNOWLEDGE");
        if (i.can(IncidentAction.START_WORK) && canWork(u, i)) a.add("START");
        if (i.can(IncidentAction.REQUEST_INFO) && canWork(u, i)) a.add("REQUEST_INFO");
        if (i.can(IncidentAction.PROVIDE_INFO) && canProvideInfo(u, i)) a.add("PROVIDE_INFO");
        if (i.can(IncidentAction.RESOLVE) && canWork(u, i)) a.add("RESOLVE");
        if (i.can(IncidentAction.VERIFY) && canVerify(u, i)) a.add("VERIFY");
        if (i.can(IncidentAction.REOPEN) && canVerify(u, i)) a.add("REOPEN");
        if (i.can(IncidentAction.CLOSE) && canClose(u, i)) a.add("CLOSE");
        if (i.can(IncidentAction.CANCEL) && canCancel(u, i)) a.add("CANCEL");
        if (canEdit(u, i)) a.add("EDIT");
        if (canGiveFeedback(u, i) && i.getFeedback() == null
                && EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED).contains(i.getStatus())) {
            a.add("FEEDBACK");
        }
        if (canLinkDuplicates(u) && i.getMasterIncident() == null && i.getStatus().isOpen()) a.add("LINK_DUPLICATE");
        if (!i.getStatus().isTerminal()) a.add("COMMENT");
        return a;
    }

    private static boolean isReporter(User u, Incident i) { return i.getReporter().getId().equals(u.getId()); }

    private static boolean isAssignee(User u, Incident i) {
        return i.getAssignedTechnician() != null && i.getAssignedTechnician().getId().equals(u.getId());
    }
}
