[#-- @ftlvariable name="action" type="com.atlassian.bamboo.build.PlanResultsAction" --]
[#-- @ftlvariable name="variables" type="java.util.HashMap<java.lang.String,java.lang.String>" --]
[#-- @ftlvariable name="" type="com.atlassian.bamboo.build.PlanResultsAction" --]

<h2>Version Metadata</h2>

<dl class="details-list">
[#list variables.entrySet() as variable]
    <dt>${variable.key?html}</dt>
    <dd>${variable.value?html}</dd>
[/#list]
</dl>