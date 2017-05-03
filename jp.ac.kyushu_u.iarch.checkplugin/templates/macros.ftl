<#assign THIS_VAR_NAME = "__this">
<#assign PJP_VAR_NAME = "__pjp">

<#macro paramTypeCommaSep method>
    <#list method.params as param>
        ${param.type}<#sep>, </#sep><#t>
    </#list>
</#macro>

<#macro paramNameCommaSep method>
    <#list method.params as param>
        ${param.name}<#sep>, </#sep><#t>
    </#list>
</#macro>

<#macro paramTypeNameCommaSep method>
    <#list method.params as param>
        ${param.type} ${param.name}<#sep>, </#sep><#t>
    </#list>
</#macro>

<#macro combinedParamNameCommaSep origMethod calledMethod>
    <#list calledMethod.params as param>
        <#if origMethod.params[param_index]??>
            ${origMethod.params[param_index].name}<#t>
        <#else>
            null<#t>
        </#if>
        <#sep>, </#sep><#t>
    </#list>
</#macro>

<#macro fullClassName method>
    <#if method.packageName?has_content>${method.packageName}.</#if>${method.className}<#t>
</#macro>

<#macro fullName method>
    <@fullClassName method />.${method.name}<#t>
</#macro>

<#macro interTypeMethodDecl method>
    ${method.type} <@fullName method />(<@paramTypeCommaSep method />)<#t>
</#macro>

<#function aroundMethodName method>
    <#return "around${weightVar(method)?cap_first}">
</#function>

<#macro aroundMethodDecl method>
    Object ${aroundMethodName(method)}(<#t>
    ProceedingJoinPoint ${PJP_VAR_NAME}<#t>
    <#if !method.isStatic>
        <#lt>, <@fullClassName method /> ${THIS_VAR_NAME}<#rt>
    </#if>
    <#if method.params?size != 0>
        <#lt>, <@paramTypeNameCommaSep method /><#rt>
    </#if>
    ) throws Throwable<#t>
</#macro>

<#function weightVar method>
    <#return "weight${method.label?cap_first}">
</#function>

<#function weightGetter method>
    <#return "get${weightVar(method)?cap_first}">
</#function>

<#function weightSetter method>
    <#return "set${weightVar(method)?cap_first}">
</#function>