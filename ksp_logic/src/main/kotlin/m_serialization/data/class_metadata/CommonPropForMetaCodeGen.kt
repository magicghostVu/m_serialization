package m_serialization.data.class_metadata

import com.google.devtools.ksp.symbol.KSClassDeclaration
import m_serialization.data.prop_meta_data.AbstractPropMetadata

class CommonPropForMetaCodeGen(
    val constructorProps: List<AbstractPropMetadata>,
    val otherProps: List<AbstractPropMetadata>,
    val classDec: KSClassDeclaration,
    val protocolUniqueId: Short = 0,
    val globalUniqueTag: Map<KSClassDeclaration, Short>
) {
}