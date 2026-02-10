package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.MethodCall;
import domain.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Principle Check: Program to Interface Check
 * Detects violations of "Program to Interface, not Implementation".
 * For example: using ArrayList instead of List, HashMap instead of Map.
 * 
 * @author Sophia
 */
public class ProgramToInterfaceCheck implements PrincipleCheck {

    // Maps concrete implementations to their preferred interfaces
    private static final Map<String, String> IMPLEMENTATION_TO_INTERFACE = new HashMap<>();
    
    static {
        // Collection framework
        IMPLEMENTATION_TO_INTERFACE.put("java.util.ArrayList", "java.util.List");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.LinkedList", "java.util.List");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.Vector", "java.util.List");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.HashSet", "java.util.Set");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.LinkedHashSet", "java.util.Set");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.TreeSet", "java.util.Set");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.HashMap", "java.util.Map");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.LinkedHashMap", "java.util.Map");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.TreeMap", "java.util.Map");
        IMPLEMENTATION_TO_INTERFACE.put("java.util.Hashtable", "java.util.Map");
    }

    @Override
    public String getName() {
        return "ProgramToInterfaceCheck";
    }

    @Override
    public String getDescription() {
        return "Detects violations of 'Program to Interface, not Implementation'";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();
        Set<String> reportedFields = new HashSet<>();

        // Check field types
        for (FieldInfo field : classInfo.getFields()) {
            String fieldType = field.getTypeName();
            if (IMPLEMENTATION_TO_INTERFACE.containsKey(fieldType)) {
                String preferredInterface = IMPLEMENTATION_TO_INTERFACE.get(fieldType);
                String location = classInfo.getName() + "." + field.getName();
                
                if (!reportedFields.contains(location)) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        String.format("Field type '%s' should use interface '%s' instead",
                            fieldType, preferredInterface),
                        location
                    ));
                    reportedFields.add(location);
                }
            }
        }

        // Check method return types
        for (MethodInfo method : classInfo.getMethods()) {
            String returnType = method.getReturnTypeName();
            if (IMPLEMENTATION_TO_INTERFACE.containsKey(returnType)) {
                String preferredInterface = IMPLEMENTATION_TO_INTERFACE.get(returnType);
                String location = classInfo.getName() + "." + method.getName() + "()";
                
                issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    String.format("Return type '%s' should use interface '%s' instead",
                        returnType, preferredInterface),
                    location
                ));
            }

            // Check parameter types
            for (String paramType : method.getParameterTypeNames()) {
                if (IMPLEMENTATION_TO_INTERFACE.containsKey(paramType)) {
                    String preferredInterface = IMPLEMENTATION_TO_INTERFACE.get(paramType);
                    String location = classInfo.getName() + "." + method.getName() + "()";
                    
                    issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        String.format("Parameter type '%s' should use interface '%s' instead",
                            paramType, preferredInterface),
                        location
                    ));
                }
            }
        }

        return issues;
    }
}
