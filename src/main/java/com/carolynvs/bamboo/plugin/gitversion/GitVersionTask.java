package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.build.BuildOutputLogEntry;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.BambooProcessHandler;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.utils.process.*;
import com.google.common.collect.Lists;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GitVersionTask implements TaskType
{
    private static final String PREFIX = "GitVersion";
    private final CapabilityContext capabilityContext;

    public GitVersionTask(CapabilityContext capabilityContext)
    {
        this.capabilityContext = capabilityContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext)
    {
        TaskResultBuilder resultBuilder = TaskResultBuilder.create(taskContext);
        BuildLogger log = taskContext.getBuildLogger();

        StringOutputHandler gitVersionOutputHandler = new StringOutputHandler();
        ExternalProcess gitVersionProcess = buildGitVersionProcess(taskContext.getWorkingDirectory(), gitVersionOutputHandler);
        log.addBuildLogEntry(String.format("Executing %s", gitVersionProcess.getCommandLine()));
        gitVersionProcess.execute();

        String output = gitVersionOutputHandler.getOutput();
        if(!gitVersionProcess.getHandler().succeeded())
        {
            log.addErrorLogEntry(String.format("Error executing GitVersion\r\n%s", output));
            return resultBuilder.failed().build();
        }

        Map<String, String> metadata = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        ObjectMapper jsonParser = new ObjectMapper();

        JsonNode jsonNode;
        try
        {
            jsonNode = jsonParser.readTree(output);
        }
        catch (IOException e)
        {
            log.addErrorLogEntry("Error parsing json output of GitVersion", e);
            return resultBuilder.failedWithError().build();
        }

        for(Iterator<Map.Entry<String, JsonNode>> results = jsonNode.getFields(); results.hasNext();)
        {
            Map.Entry<String, JsonNode> result = results.next();
            String key = String.format("%s.%s", PREFIX, result.getKey());

            JsonNode jsonValue = result.getValue();
            String value = jsonValue.isTextual() ? jsonValue.getTextValue() : jsonValue.toString();

            log.addBuildLogEntry(String.format("%s=%s", key, value));
            metadata.put(key, value);
        }

        return resultBuilder.success().build();
    }

    @Nullable
    private String getGitVersionCapability()
    {
        return capabilityContext.getCapabilityValue(GitVersionCapability.CAPABILITY_KEY);
    }

    private ExternalProcess buildGitVersionProcess(File gitRepo, OutputHandler outputHandler)
    {
        String gitversionExecutable = getGitVersionCapability();

        final PluggableProcessHandler handler = new BambooProcessHandler(outputHandler, outputHandler);
        ExternalProcess process = new ExternalProcessBuilder()
                .command(Lists.newArrayList(gitversionExecutable, gitRepo.getAbsolutePath()))
                .handler(handler)
                .build();

        return process;
    }
}