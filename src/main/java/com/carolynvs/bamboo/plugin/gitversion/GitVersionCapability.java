package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractExecutableCapabilityTypeModule;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GitVersionCapability extends AbstractExecutableCapabilityTypeModule
{
    public static final String CAPABILITY_KEY = "system.executable.gitversion";
    private static final String EXECUTABLE_KEY = "gitversionExecutable";
    private static final String CAPABILITY_UNDEFINED_KEY = "gitversion.error.undefinedExecutable";

    @Override
    public String getMandatoryCapabilityKey()
    {
        return CAPABILITY_KEY;
    }

    @Override
    public String getExecutableKey()
    {
        return EXECUTABLE_KEY;
    }

    @Override
    public String getCapabilityUndefinedKey()
    {
        return CAPABILITY_UNDEFINED_KEY;
    }

    @Override
    public List<String> getDefaultWindowPaths()
    {
        return Lists.newArrayList();
    }

    @Override
    public String getExecutableFilename()
    {
        return "gitversion";
    }
}
