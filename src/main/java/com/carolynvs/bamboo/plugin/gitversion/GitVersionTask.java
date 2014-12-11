package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.task.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class GitVersionTask implements TaskType
{

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext)
            throws TaskException
    {
        Map<String, String> metadata = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        metadata.put("gitversion", "1.2.3");

        return TaskResultBuilder.create(taskContext).success().build();
    }
}