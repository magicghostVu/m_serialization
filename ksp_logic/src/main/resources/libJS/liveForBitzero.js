
var JReaderBuffer = engine.InPacket.extend({

    ctor: function (pkg) {
        this._super();
        this.init(pkg);
    },

    readBool: function () {
        return this.getByte() != 0;
    },

    readInt:function(){
        return this.getInt();
    },

    readShort:function(){
        return this.getShort();
    },

    readDouble:function(){
        return this.getDouble();
    },

    readByte:function(){
        return this.getByte();
    },

    readFloat:function(){
        throw new Error("not support float");
    },

    readLong:function(){
        return +this.getLong();
    },
    readString:function(){
        return this.getString();
    },
    readBytes:function(){
        return this.getBytes(this.readSize());
    },
    readSize:function(){
        return this.readShort();
    },
    readEnum:function(){
        return this.readShort();
    },
    readJavaClass:function(){
        return this.readShort();
    }
});



var JWriterBuffer = engine.OutPacket.extend({

    writeBool: function (b) {
        return this.putByte(b?1:0);
    },

    writeInt:function(i){
        return this.putInt(i);
    },

    writeShort:function(s){
        return this.putShort(s);
    },

    writeDouble:function(d){
        return this.putDouble(d);
    },

    writeByte:function(b){
        return this.putByte(b);
    },

    writeFloat:function(f){
        throw new Error("not support float");
    },

    writeLong:function(l){
        return this.putLong(l);
    },
    writeString:function(s){
        return this.putString(s);
    },
    writeBytes:function(bs){
        this.writeSize(bs.length)
        return this.putBytes(bs);
    },
    writeSize:function(s){
        return this.putShort(s);
    },
    writeEnum:function(e){
        return this.putShort(e);
    },
    writeJavaClass:function(c){
        return this.putShort(c);
    }
});