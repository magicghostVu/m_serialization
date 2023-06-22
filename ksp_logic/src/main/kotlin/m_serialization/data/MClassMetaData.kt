package m_serialization.data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import m_serialization.data.prop_meta_data.AbstractPropMetadata


// mỗi class được đánh dấu là m_serialization sẽ sinh ra một object này
class MClassMetaData(
    val constructorProps: List<AbstractPropMetadata>,
    val otherProps: List<AbstractPropMetadata>,
    val classDec: KSClassDeclaration
) {
}