package m_serialization.annotations

// field này sẽ không được serialize/deserialize
// field này chắc chắn phải có giá trị mặc định (cần check lúc build)
//  vì nếu không có thì sau này lúc khởi tạo lại object sẽ không có giá trị để fill vào field này
annotation class MTransient()
