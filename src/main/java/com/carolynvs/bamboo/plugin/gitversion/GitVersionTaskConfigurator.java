package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GitVersionTaskConfigurator extends AbstractTaskConfigurator
{
    public static final String REPO_PATH = "repoPath";

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(ActionParametersMap params, TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put(REPO_PATH, params.getString(REPO_PATH));

        return config;
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        context.put(REPO_PATH, taskDefinition.getConfiguration().get(REPO_PATH));
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        context.put(REPO_PATH, taskDefinition.getConfiguration().get(REPO_PATH));
    }
}
