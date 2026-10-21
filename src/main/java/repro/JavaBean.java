package repro;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/** Plain Java bean with collection getters. */
public class JavaBean {

    /** Not annotated: flexible type (Mutable)Collection<String!>! -> works. */
    public Collection<String> getPlain() {
        return List.of("a");
    }

    /** @Nullable (JSR-305): nullable flexible type (Mutable)Collection<String!>? -> classifier throws. */
    @Nullable
    public Collection<String> getNullable() {
        return List.of("b");
    }
}
