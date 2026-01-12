package visbox.logger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Annotation used to declare a log color for a class.
 */
@Retention(RetentionPolicy.RUNTIME)            // keep annotation at runtime
@Target(ElementType.TYPE)                       // only valid on classes
public @interface LogColor {
    LogColorEnum value();
}