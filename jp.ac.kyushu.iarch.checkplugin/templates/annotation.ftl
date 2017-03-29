<#-- FreeMarker template to define annotation.
     Now this is generated in test support function. -->
<#assign aDateTime = .now>
/*
Generated by iArch
Time: ${aDateTime?iso_local}
*/

package ${PACKAGE_NAME};

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ${SUPPRESS_ANNOTATION_NAME} {
    String[] value();
}