/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */
package edu.umd.cs.findbugs.plugin.eclipse.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * A complicated scheduling rule for mutually exclusivity, derived from:
 * http://help.eclipse.org/help30/topic/org.eclipse.platform.doc.isv/guide/
 * runtime_jobs_rules.htm
 *
 * This rule takes the available cores into account and also allows to run
 * FB independently on different resources (if there are enough cores available)
 */
public class MutexSchedulingRule implements ISchedulingRule {

    // enable multicore
    private static final int MAX_JOBS = Runtime.getRuntime().availableProcessors();
    private static final boolean MULTICORE = MAX_JOBS > 1;

    private final IResource resource;

    public MutexSchedulingRule(IResource resource) {
        super();
        this.resource = resource;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        if (!(rule instanceof MutexSchedulingRule)) {
            return false;
        }
        MutexSchedulingRule mRule = (MutexSchedulingRule) rule;
        if (resource == null || mRule.resource == null) {
            // we don't know the resource, so better to say we have conflict
            return true;
        }
        if (MULTICORE) {
            return resource.contains(mRule.resource) || tooManyJobsThere();
        }
        return true;
    }

    private static boolean tooManyJobsThere() {
        Job[] fbJobs = Job.getJobManager().find(FindbugsPlugin.class);
        int runningCount = 0;
        for (Job job : fbJobs) {
            if (job.getState() == Job.RUNNING && job.getRule() instanceof MutexSchedulingRule) {
                runningCount++;
                // TODO made this condition configurable
                if( runningCount > MAX_JOBS) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean contains(ISchedulingRule rule) {
//        if (rule instanceof IResource && resource != null) {
//            return resource.contains(rule);
//        }
        return isConflicting(rule);
        /*
         * from the URL above: "If you do not need to create hierarchies of
         * locks, you can implement the contains method to simply call
         * isConflicting."
         */
    }

    @Override
    public String toString() {
        return "MutexSchedulingRule, resource: " + resource;
    }

}
