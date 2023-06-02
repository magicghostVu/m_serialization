package m_serialization

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.annotations.MSerialization

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

        allClassWillProcess.forEach {
            val classDeclaration = it as KSClassDeclaration

            val name = classDeclaration.qualifiedName?.asString()


            // get các props được khai báo tại class hiện tại
            classDeclaration.getDeclaredProperties()


            // get các prop được khai báo cả ở class cha
            val allProps = classDeclaration.getAllProperties()

            val listAllPropData = mutableListOf<MPropData>()
            allProps.forEach { prop ->
                listAllPropData.add(
                    MPropData(prop.simpleName.asString(), prop.hasBackingField)
                )
            }

            logger.warn("class $name, props $listAllPropData")
        }
        // verify source


        // gen code


        return emptyList()
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