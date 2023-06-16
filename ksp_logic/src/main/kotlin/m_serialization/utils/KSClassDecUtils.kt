package m_serialization.utils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import java.util.*

object KSClassDecUtils {

    // get all con, cháu ...
    // nếu con(cháu...) là sealed thì không add??

    lateinit var logger: KSPLogger

    fun KSClassDeclaration.getAllChildRecursive(context: KSClassDeclaration, propName: String): List<KSClassDeclaration> {
        //val className = qualifiedName?.asString();
        if (!this.modifiers.contains(Modifier.SEALED)) {
            return emptyList()
        }

        val result = mutableListOf<KSClassDeclaration>()

        val q = LinkedList<KSClassDeclaration>()
        q.add(this)
        while (q.isNotEmpty()) {
            val tmp = q.removeFirst()
            val allChildren = tmp.getSealedSubclasses().toList()
            allChildren.forEach {
                if (it.modifiers.contains(Modifier.SEALED)) {
                    q.add(it)
                } else {
                    /*logger.warn("at context ${context.qualifiedName?.asString()}, prop name $propName," +
                            "add child ${it.qualifiedName?.asString()}, " +
                            "parent is ${tmp.qualifiedName?.asString()}")*/
                    result.add(it)
                }
            }
        }
        return result
    }

}