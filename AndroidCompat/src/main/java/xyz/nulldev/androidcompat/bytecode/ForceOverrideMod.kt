package xyz.nulldev.androidcompat.bytecode

import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException
import mu.KotlinLogging
import java.lang.reflect.Modifier

/**
 * Override some final methods in Context
 */

class ForceOverrideMod {

    val logger = KotlinLogging.logger {}

    fun apply(clazz: String): List<CtClass> {
        fun recursiveFindSuperClasses(clazz: CtClass, supers: MutableList<CtClass>) {
            val theSuper = clazz.superclass
            if(theSuper != null && theSuper != clazz && !supers.contains(theSuper)) {
                supers += theSuper
                recursiveFindSuperClasses(theSuper, supers)
            }
        }
        logger.info { "Forcibly overriding methods in $clazz!" }

        // get the subclass
        val resolvedClazz = ClassPool.getDefault().get(clazz)
        val superClasses = mutableListOf<CtClass>().apply {
            recursiveFindSuperClasses(resolvedClazz, this)
        }

        val modifiedClasses = mutableListOf<CtClass>()

        resolvedClazz.declaredMethods.filter {
            it.hasAnnotation(ForceOverride::class.java)
        }.forEach {
            logger.info { "Overriding method: ${it.name}..." }
            val annotation = it.getAnnotation(ForceOverride::class.java) as ForceOverride

            for(superClass in superClasses) {
                logger.info { "Searching for super method in class: ${superClass.name}..." }
                try {
                    //Find super method
                    val superMethod = superClass.getDeclaredMethod(annotation.targetMethod, it.parameterTypes)

                    logger.info { "Super method found! Patching..." }

                    //Make super method public
                    superMethod.modifiers = Modifier.PUBLIC
                    //Rename our method to override the super method
                    it.name = annotation.targetMethod

                    //Flag classes as modified
                    if (!modifiedClasses.contains(superClass)) modifiedClasses += superClass
                    if (!modifiedClasses.contains(it.declaringClass)) modifiedClasses += it.declaringClass

                    return@forEach
                } catch (e: NotFoundException) {
                    //This super does not have the method to override, keep trying
                }
            }

            logger.warn { "Could not find super method for method: ${it.name}!" }
        }

        return modifiedClasses
    }
}