package repro

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.jvm.kotlinFunction

fun main() {
    println("Kotlin ${KotlinVersion.CURRENT}")

    // 1. Pure kotlin-reflect: KType.classifier of a Java getter's return type.
    for (getter in listOf("getPlain", "getNullable")) {
        val returnType = JavaBean::class.java.getMethod(getter).kotlinFunction!!.returnType
        try {
            println("$getter: $returnType -> classifier = ${returnType.classifier}")
        } catch (e: Throwable) {
            println("$getter: $returnType -> kotlin-reflect FAILED: $e")
        }
    }

    // 2. Same failure via jackson-module-kotlin:
    //    KotlinAnnotationIntrospector.findNullSerializer -> ReflectionCache.getValueClassReturnType -> KType.classifier
    try {
        println("jackson: ${jacksonObjectMapper().writeValueAsString(JavaBean())}")
    } catch (e: Throwable) {
        println("jackson FAILED: $e")
    }
}
