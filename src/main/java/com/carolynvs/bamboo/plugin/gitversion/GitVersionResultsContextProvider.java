package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.chains.ChainResultsSummaryImpl;
import com.atlassian.bamboo.plan.cache.ImmutableChainImpl;
import com.atlassian.bamboo.plan.cache.ImmutableJob;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GitVersionResultsContextProvider implements ContextProvider
{
    @Override
    public void init(Map<String, String> map) throws PluginParseException
    {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> map)
    {
        HashMap<String, String> variables = new HashMap<String, String>();

        final ImmutableChainImpl plan = (ImmutableChainImpl)map.get("plan");
        final ChainResultsSummaryImpl planSummary = (ChainResultsSummaryImpl) map.get("resultSummary");
        Set<String> savedVars = getSavedVars(plan);

        for (ResultsSummary jobSummary : planSummary.getOrderedJobResultSummaries())
        {
            for (Map.Entry<String, String> entry : jobSummary.getCustomBuildData().entrySet())
            {
                if(!entry.getKey().startsWith(GitVersionTask.PREFIX))
                    continue;

                String key = entry.getKey().substring(GitVersionTask.PREFIX.length() + 1);

                if(savedVars.isEmpty() || savedVars.contains(key))
                    variables.put(key, entry.getValue());
            }
        }

        map.put("variables", variables);

        return map;
    }

    private Set<String> getSavedVars(ImmutableChainImpl plan)
    {
        final HashSet<String> savedVars = new HashSet<String>();

        for (ImmutableJob job : plan.getAllJobs())
        {
            for (TaskDefinition task : job.getBuildDefinition().getTaskDefinitions())
            {
                if (task.getPluginKey().equals("com.carolynvs.gitversion:GitVersionTask"))
                {
                    final String savedVarsList = task.getConfiguration().get(GitVersionTaskConfigurator.SAVED_VARIABLES);
                    for (String variable : savedVarsList.split(" "))
                    {
                        if(variable.isEmpty())
                            continue;

                        savedVars.add(variable);
                    }
                    return savedVars;
                }
            }
        }
        return savedVars;
    }
}