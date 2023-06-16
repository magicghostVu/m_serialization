package m_serialization.data

import com.google.devtools.ksp.symbol.KSClassDeclaration

sealed class ComplexPropData() {
}

class ObjectPropData(val classDec: KSClassDeclaration) : ComplexPropData()
class ListPropData(val elementClass: KSClassDeclaration) : ComplexPropData()
class MapPropData(val valueClassDec: KSClassDeclaration) : ComplexPropData()
