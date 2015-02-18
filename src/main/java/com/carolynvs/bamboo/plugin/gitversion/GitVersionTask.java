package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.plugins.git.GitCapabilityTypeModule;
import com.atlassian.bamboo.process.BambooProcessHandler;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.utils.process.*;
import com.google.common.collect.Lists;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GitVersionTask implements TaskType
{
    private static final String PREFIX = "GitVersion";
    private final CapabilityContext capabilityContext;
    private final ProcessService processService;

    public GitVersionTask(CapabilityContext capabilityContext, ProcessService processService)
    {
        this.capabilityContext = capabilityContext;
        this.processService = processService;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        TaskResultBuilder resultBuilder = TaskResultBuilder.create(taskContext);
        BuildLogger buildLogger = taskContext.getBuildLogger();

        GitVersionTaskConfiguration config = readConfiguration(taskContext);

        boolean fixDetachedHeadSucceeded = fixDetachedHead(config, taskContext);
        if (!fixDetachedHeadSucceeded)
            return resultBuilder.failed().build();

        GitVersionOutput gitVersionOutput = executeGitVersion(config, buildLogger);
        if(!gitVersionOutput.Succeeded)
        {
            buildLogger.addErrorLogEntry(gitVersionOutput.ErrorMessage, gitVersionOutput.Exception);
            return resultBuilder.failed().build();
        }

        Map<String, String> buildMetadata = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        saveVariables(gitVersionOutput.Variables, buildMetadata, buildLogger);

        return resultBuilder.success().build();
    }

    private boolean fixDetachedHead(GitVersionTaskConfiguration input, TaskContext taskContext)
    {
        taskContext.getBuildLogger().addBuildLogEntry("Ensuring that we are not in a detached HEAD state...");

        ExternalProcess gitCheckout = executeGitCommand(input.Repository, taskContext, "checkout", "--force", input.Branch);
        if(!gitCheckout.getHandler().succeeded())
            return false;

        ExternalProcess gitReset = executeGitCommand(input.Repository, taskContext, "reset", "--hard", input.Revision);
        if(!gitReset.getHandler().succeeded())
            return false;

        return true;
    }

    private GitVersionOutput executeGitVersion(GitVersionTaskConfiguration input, BuildLogger buildLogger)
    {
        StringOutputHandler processOutputHandler = new StringOutputHandler();
        ExternalProcess process = executeGitVersionCommand(input, processOutputHandler, buildLogger);
        process.execute();
        return new GitVersionOutput(process.getHandler(), processOutputHandler);
    }

    private GitVersionTaskConfiguration readConfiguration(TaskContext taskContext)
    {
        ConfigurationMap taskConfig = taskContext.getConfigurationMap();
        String repoPath =  taskConfig.get(GitVersionTaskConfigurator.REPO_PATH);
        File buildDirectory = taskContext.getWorkingDirectory();
        File repo = new File(buildDirectory, repoPath);

        // todo: Make this configurable, currently assumes we are only working with the default repository
        Map<String, String> buildMetaData = taskContext.getBuildContext().getCurrentResult().getCustomBuildData();
        String branch = buildMetaData.get("planRepository.branchName");
        String revision = buildMetaData.get("planRepository.revision");

        return new GitVersionTaskConfiguration(repo, branch, revision);
    }

    /**
     * Persist the GitVersion variables to the build's metadata
     */
    private void saveVariables(JsonNode variables, Map<String, String> buildMetadata, BuildLogger buildLogger)
    {
        for(Iterator<Map.Entry<String, JsonNode>> results = variables.getFields(); results.hasNext();)
        {
            Map.Entry<String, JsonNode> result = results.next();
            String key = String.format("%s.%s", PREFIX, result.getKey());

            JsonNode jsonValue = result.getValue();
            String value = jsonValue.isTextual() ? jsonValue.getTextValue() : jsonValue.toString();

            buildLogger.addBuildLogEntry(String.format("%s=%s", key, value));
            buildMetadata.put(key, value);
        }
    }

    @Nullable
    private String getGitVersionExecutable()
    {
        return capabilityContext.getCapabilityValue(GitVersionCapability.CAPABILITY_KEY);
    }

    @Nullable
    public String getGitExecutable()
    {
        return capabilityContext.getCapabilityValue(GitCapabilityTypeModule.GIT_CAPABILITY);
    }

    private ExternalProcess executeGitVersionCommand(GitVersionTaskConfiguration input, OutputHandler outputHandler, BuildLogger buildLogger)
    {
        String gitversionExecutable = getGitVersionExecutable();

        final PluggableProcessHandler handler = new BambooProcessHandler(outputHandler, outputHandler);
        ExternalProcess process = new com.atlassian.utils.process.ExternalProcessBuilder()
                .command(Lists.newArrayList(gitversionExecutable, input.Repository.getAbsolutePath()))
                .handler(handler)
                .build();
        buildLogger.addBuildLogEntry(String.format("Executing %s", process.getCommandLine()));

        return process;
    }

    private ExternalProcess executeGitCommand(File workingDirectory, TaskContext taskContext, String... commandArgs)
    {
        List<String> command = new ArrayList<String>();
        command.add(getGitExecutable());
        command.addAll(Arrays.asList(commandArgs));

        com.atlassian.bamboo.process.ExternalProcessBuilder processBuilder = new com.atlassian.bamboo.process.ExternalProcessBuilder()
                .command(command)
                .workingDirectory(workingDirectory);

        return processService.executeExternalProcess(taskContext, processBuilder);
    }

    private class GitVersionTaskConfiguration
    {
        public final File Repository;
        public final String Branch;
        public final String Revision;

        public GitVersionTaskConfiguration(File repository, String branch, String revision)
        {
            Repository = repository;
            Branch = branch;
            Revision = revision;
        }
    }

    private class GitVersionOutput
    {
        public boolean Succeeded;
        public String ErrorMessage;
        public Exception Exception;
        public JsonNode Variables;

        public GitVersionOutput(ProcessHandler processHandler, StringOutputHandler processOutputHandler)
        {
            String processOutput = processOutputHandler.getOutput();
            Succeeded = processHandler.succeeded();

            if(!Succeeded)
            {
                ErrorMessage = String.format("Error executing GitVersion\r\n%s", processOutput);
                return;
            }

            ObjectMapper jsonParser = new ObjectMapper();
            try
            {
                Variables = jsonParser.readTree(processOutput);
            }
            catch (IOException e)
            {
                ErrorMessage = "Error parsing json processOutput of GitVersion";
                Exception = e;
                Succeeded = false;
                return;
            }
        }
    }
}