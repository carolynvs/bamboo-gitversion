package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskRequirementSupport;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class GitVersionTaskConfigurator extends AbstractTaskConfigurator implements TaskRequirementSupport
{
    public static final String REPO_PATH = "repoPath";
    public static final String SAVED_VARIABLES = "savedVars";
    public static final String ARGS = "args";

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(ActionParametersMap params, TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put(REPO_PATH, params.getString(REPO_PATH));
        config.put(SAVED_VARIABLES, params.getString(SAVED_VARIABLES));
        config.put(ARGS, params.getString(ARGS));

        return config;
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        context.put(REPO_PATH, taskDefinition.getConfiguration().get(REPO_PATH));
        context.put(SAVED_VARIABLES, taskDefinition.getConfiguration().get(SAVED_VARIABLES));
        context.put(ARGS, taskDefinition.getConfiguration().get(ARGS));
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        context.put(REPO_PATH, taskDefinition.getConfiguration().get(REPO_PATH));
        context.put(SAVED_VARIABLES, taskDefinition.getConfiguration().get(SAVED_VARIABLES));
        context.put(ARGS, taskDefinition.getConfiguration().get(ARGS));
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition)
    {
        Set<Requirement> requirements = Sets.newHashSet();
        requirements.add(new RequirementImpl(GitVersionTask.CAPABILITY_KEY, true, ".*"));
        return requirements;
    }
}
