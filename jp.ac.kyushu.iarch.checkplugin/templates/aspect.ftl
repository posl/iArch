<#-- FreeMarker template to generate Aspect. -->
<#assign SAFE_MODE = false>
<#include "macros.ftl">
<#assign aDateTime = .now>
<#-- comment -->
/*
Generated by iArch
Time: ${aDateTime?iso_local}
*/

<#-- package -->
<#if !containsDefaultPackage>
package ${ASPECTGEN_PACKAGE_NAME};
</#if>

<#-- import -->
import ${PACKAGE_NAME}.SuppressArchfaceWarnings;
import ${PACKAGE_NAME}.${ABSTRACT_CLASS_NAME};

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.SuppressAjWarnings;

<#list importClasses?keys as key>
import ${importClasses[key]}.${key};
</#list>

<#-- class -->
<#if !SAFE_MODE>
@SuppressWarnings({"all"})
@SuppressAjWarnings({"adviceDidNotMatch"})
@${SUPPRESS_ANNOTATION_NAME}({"all"})
</#if>
@Aspect
public class ${ASPECT_CLASS_NAME_PREFIX}${label} extends ${ABSTRACT_CLASS_NAME} {

    <#-- members -->
    <#list methods as method>
    private static double ${weightVar(method)} = ${method.weight?c}d;
    </#list>
    
    <#-- methods -->
    <#list methods as method>
    public static double ${weightGetter(method)}() {return ${weightVar(method)};}
    public static void ${weightSetter(method)}(double w) {${weightVar(method)} = w;}
    </#list>

    <#-- BEGIN aspect -->
    <#list methods as method><#if !method.isEmpty>
    <#-- advice -->
    @Around("<#rt>
        <#-- call -->
        call(<@interTypeMethodDecl method />)<#t>
        <#-- this -->
        <#if !method.isStatic>
            <#lt> && target(${THIS_VAR_NAME})<#rt>
        </#if>
        <#-- args -->
        <#if method.params?size != 0>
            <#lt> && args(<@paramNameCommaSep method />)<#rt>
        </#if>
        <#-- withincode -->
        <#if whereCalled?size != 0>
            <#lt> && withincode(<#rt>
                <#list whereCalled as where>
                    <@interTypeMethodDecl where /><#sep>|| </#sep><#t>
                </#list>
            )<#t>
        </#if>
        <#lt> && !cflow(adviceexecution())<#rt>
    <#lt>")
    <#-- method -->
    public <@aroundMethodDecl method /> {
        switch (weightSwitch(<#rt>
            <#list methods as repl>
                ${weightVar(repl)}<#sep>, </#sep><#t>
            </#list>
        <#lt>)) {
            <#list methods as repl>
            case ${repl_index}:
                <#-- call -->
                <#if repl == method>
                    return ${PJP_VAR_NAME}.proceed();
                <#elseif !repl.isEmpty>
                    <#if repl.type != "void">return </#if><#rt>
                    <#if repl.isStatic>
                        <@fullName repl /><#t>
                    <#else>
                        __this.${repl.name}<#t>
                    </#if>(<#t>
                        <@combinedParamNameCommaSep method repl /><#t>
                    <#lt>);
                    <#if method.type == "void">return null;</#if>
                <#else>
                    return null;
                </#if>
            </#list>
        }
        throw new IllegalStateException();
    }
    </#if></#list>
    <#-- END aspect -->

}