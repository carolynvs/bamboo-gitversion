package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.process.BambooProcessHandler;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.utils.process.*;
import com.google.common.collect.Lists;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;

public class GitVersionTask implements TaskType
{
    private final CapabilityContext capabilityContext;

    public GitVersionTask(CapabilityContext capabilityContext)
    {
        this.capabilityContext = capabilityContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext)
            throws TaskException
    {
        StringOutputHandler outputHandler = new StringOutputHandler();
        ExternalProcess gitVersionProcess = buildGitVersionProcess(outputHandler);
        gitVersionProcess.execute();

        if(gitVersionProcess.getHandler().succeeded())
        {
            Map<String, String> metadata = taskContext.getBuildContext().getBuildResult().getCustomBuildData();

            String output = outputHandler.getOutput();
            GitVersionOutputParser parser = new GitVersionOutputParser(output);

            String assemblyVersion = parser.getAssemblySemVer();
            if (assemblyVersion != null)
            {
                taskContext.getBuildLogger().addBuildLogEntry(String.format("GitVersion.AssemblySemVer=%s", assemblyVersion));
                metadata.put("GitVersion.AssemblySemVer", assemblyVersion);
            }
        }

        return TaskResultBuilder.create(taskContext).success().build();
    }

    @Nullable
    private String getGitVersionCapability()
    {
        return capabilityContext.getCapabilityValue(GitVersionCapability.CAPABILITY_KEY);
    }

    private ExternalProcess buildGitVersionProcess(OutputHandler outputHandler)
    {
        String gitversionExecutable = getGitVersionCapability();

        final PluggableProcessHandler handler = new BambooProcessHandler(outputHandler, outputHandler);
        ExternalProcess process = new ExternalProcessBuilder()
                .command(Lists.newArrayList(gitversionExecutable))
                .handler(handler)
                .build();

        return process;
    }
}

class GitVersionOutputParser
{
    private String assemblySemVer;

    public GitVersionOutputParser(String output)
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(output);
            assemblySemVer = jsonNode.get("AssemblySemVer").getTextValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getAssemblySemVer() {
        return assemblySemVer;
    }
}