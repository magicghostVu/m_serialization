package m_serialization

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient

class MSerializationSymbolProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = env.logger

    init {
        logger.warn("init ksp logic")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allClassWillProcess = resolver.getSymbolsWithAnnotation(
            MSerialization::class
                .qualifiedName
                .toString()
        )

        val setAllClass = allClassWillProcess.toSet()

        allClassWillProcess.forEach {
            val classDeclaration = it as KSClassDeclaration

            val name = classDeclaration.qualifiedName?.asString()


            // get các props được khai báo tại class hiện tại
            //classDeclaration.getDeclaredProperties()


            // get các prop được khai báo cả ở class cha
            val allProps = classDeclaration.getAllProperties()

            val listAllPropData = mutableListOf<MPropData>()
            allProps.forEach { prop ->
                listAllPropData.add(
                    MPropData(prop.simpleName.asString(), prop.hasBackingField)
                )

                // chú ý phương thức này để tìm kiểu của type parameter
                //prop.asMemberOf()
                //prop.asMemberOf()
            }

            logger.warn("class $name, props $listAllPropData")
        }
        // verify source


        // gen code


        return emptyList()
    }


    private fun verifyClassConstructor(clazz: KSClassDeclaration) {
        val primaryConstructor = clazz.primaryConstructor
        val className = clazz.qualifiedName?.asString()
        primaryConstructor
            ?: throw IllegalArgumentException("class $className not had primary constructor")

        val allParams = primaryConstructor.parameters
        val allParamIsProp = allParams.all {
            it.isVal || it.isVar
        }
        if (!allParamIsProp) throw IllegalArgumentException("class $className at primary constructor had a param not a property")
    }

    // tất cả các prop phải là
    private fun verifyAllPropSerializable(clazz: KSClassDeclaration) {


        val allProps = clazz.getAllProperties()
        val propsWillVerify = allProps
            .asSequence()
            .filter {
                it.hasBackingField
            }
            .filter {

                it.annotations.asSequence().
            }
    }

}


// nếu là 1 trong 8 kiểu cơ bản
// : Int, Long, Short, Float, Bool, Double, Byte,String
fun KSPropertyDeclaration.isPrimitiveProp(): Boolean {
    val type = type
    val trueType = type.resolve()

    return false
}


data class MPropData(val name: String, val hadBackingField: Boolean) {

}