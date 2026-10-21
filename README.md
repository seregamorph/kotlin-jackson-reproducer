# kotlin-jackson-reproducer

Since **Kotlin 2.5.0-Beta2**, `kotlin-reflect` throws a `ClassCastException` from `KType.classifier` when the type
is the return type of a **Java** method that returns a collection and is annotated with JSR-305
`@javax.annotation.Nullable`.

This breaks `jackson-module-kotlin` when it serializes such Java beans, because
`KotlinAnnotationIntrospector.findNullSerializer` → `ReflectionCache.getValueClassReturnType` reads
`KType.classifier` for every getter.

## Reproduce

```shell
mvn -q compile exec:java                          # Kotlin 2.5.0-Beta2: fails
mvn -q compile exec:java -Dkotlin.version=2.4.21  # Kotlin 2.4.21: works
```

## Code

```java
public class JavaBean {
    public Collection<String> getPlain() { ... }

    @javax.annotation.Nullable
    public Collection<String> getNullable() { ... }
}
```

```kotlin
JavaBean::class.java.getMethod("getNullable").kotlinFunction!!.returnType.classifier
```

## Result

Kotlin 2.5.0-Beta2:

```
getPlain: kotlin.collections.(Mutable)Collection<kotlin.String!>! -> classifier = class kotlin.collections.Collection
getNullable: kotlin.collections.(Mutable)Collection<kotlin.String!>? -> kotlin-reflect FAILED: java.lang.ClassCastException: class kotlin.reflect.jvm.internal.types.MutableCollectionKClassImpl cannot be cast to class kotlin.jvm.internal.ClassBasedDeclarationContainer
jackson FAILED: java.lang.ClassCastException: class kotlin.reflect.jvm.internal.types.MutableCollectionKClassImpl cannot be cast to class kotlin.jvm.internal.ClassBasedDeclarationContainer
```

Kotlin 2.4.21:

```
getPlain: kotlin.collections.(Mutable)Collection<kotlin.String!>! -> classifier = class kotlin.collections.Collection
getNullable: kotlin.collections.(Mutable)Collection<kotlin.String!>! -> classifier = class kotlin.collections.Collection
jackson: {"nullable":["b"],"plain":["a"]}
```

## Analysis

- Kotlin 2.5 now honors JSR-305 `@Nullable` in reflected Java types: `(Mutable)Collection<String!>?` instead of
  `(Mutable)Collection<String!>!`.
- For this nullable flexible type, `FlexibleKType.getClassifier` calls `javaObjectType` on a
  `MutableCollectionKClassImpl`, which is not a `ClassBasedDeclarationContainer`, so the cast fails.
- The failure needs no Jackson (see the first part of `Main.kt`), so this is a `kotlin-reflect` regression.
  `jackson-module-kotlin` only exposes it.

Stack trace via Jackson:

```
java.lang.ClassCastException: class kotlin.reflect.jvm.internal.types.MutableCollectionKClassImpl cannot be cast to class kotlin.jvm.internal.ClassBasedDeclarationContainer
	at kotlin.jvm.JvmClassMappingKt.getJavaObjectType(JvmClassMapping.kt:70)
	at kotlin.reflect.jvm.internal.types.FlexibleKType.getClassifier(FlexibleKType.kt:25)
	at com.fasterxml.jackson.module.kotlin.ReflectionCache.getValueClassReturnType(ReflectionCache.kt:107)
	at com.fasterxml.jackson.module.kotlin.ReflectionCache.findValueClassReturnType(ReflectionCache.kt:121)
	at com.fasterxml.jackson.module.kotlin.KotlinAnnotationIntrospector.findValueClassReturnType(KotlinAnnotationIntrospector.kt:162)
	at com.fasterxml.jackson.module.kotlin.KotlinAnnotationIntrospector.findNullSerializer(KotlinAnnotationIntrospector.kt:97)
	at com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair.findNullSerializer(AnnotationIntrospectorPair.java:354)
	at com.fasterxml.jackson.databind.ser.PropertyBuilder.buildWriter(PropertyBuilder.java:237)
	...
```

## Versions

- Kotlin (stdlib, reflect, compiler): 2.5.0-Beta2 (works on 2.4.21)
- jackson-module-kotlin: 2.22.4
- JDK 25
