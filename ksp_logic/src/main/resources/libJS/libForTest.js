var textDecoder = new TextDecoder("utf-8");
var textEncoder = new TextEncoder("utf-8");

function WriteBuffer() {
    var defaultSize = 1000000;
    var pos = 0;
    var dataView = new DataView(new Int8Array(defaultSize).buffer);
    this.toBytes = function () {
        return Array(pos).fill(0).map(function(i,j){return dataView.getInt8(j)});
    }
    this.writeByte = function (value) {
        return dataView.setInt8(pos++, value);
    }
    this.writeBool = function (value) {
        return dataView.setInt8(pos++, value?1:0)
    }
    this.writeInt = function (value) {
        var val = dataView.setInt32(pos, value)
        pos += 4;
        return val;
    }
    this.writeShort = function (value) {
        var val = dataView.setInt16(pos, value);
        pos += 2;
        return val;
    }
    this.writeDouble = function (value) {
        var val = dataView.setFloat64(pos, value);
        pos += 8;
        return val;
    }
    this.writeFloat = function (value) {
        var val = dataView.setFloat32(pos, value);
        pos += 4;
        return val;
    }
    this.writeLong = function (value) {
        var val = dataView.setFloat64(pos, value);
        pos += 8;
        return val;
    }
    this.writeString = function (value) {
        return this.writeBytes(textEncoder.encode(value));
    }
    this.writeBytes = function (value) {
        this.writeShort(value.length);
        for (var i = 0; i < value.length; i++) {
            this.writeByte(value[i])
        }
    }
    this.writeJavaClass = function (value) {
        return this.writeShort(value)
    }
    this.writeEnum = function (value) {
        return this.writeShort(value)
    }
    this.writeSize = function (value) {
        return this.writeShort(value)
    }
}

function ReadBuffer(bytes) {
    var pos = 0;
    var dataView = new DataView(new Int8Array(bytes).buffer);
    this.readByte = function () {
        return dataView.getInt8(pos++);
    }
    this.readBool = function () {
        return Boolean(dataView.getInt8(pos++))
    }
    this.readInt = function () {
        var val = dataView.getInt32(pos)
        pos += 4;
        return val;
    }
    this.readShort = function () {
        var val = dataView.getInt16(pos);
        pos += 2;
        return val;
    }
    this.readDouble = function () {
        var val = dataView.getFloat64(pos);
        pos += 8;
        return val;
    }
    this.readFloat = function () {
        var val = dataView.getFloat32(pos);
        pos += 4;
        return val;
    }
    this.readLong = function () {
        var val = dataView.getFloat64(pos);
        pos += 8;
        return val;
    }
    this.readString = function () {
        return textDecoder.decode(new Int8Array(this.readBytes()))
    }
    this.readBytes = function () {
        var len = this.readSize() & 0xFFFF;
        var val = bytes.slice(pos, pos + len);
        pos += len;
        return val;
    }
    this.readJavaClass = function () {
        return this.readShort()
    }
    this.readEnum = function () {
        return this.readShort()
    }
    this.readSize = function () {
        return this.readShort()
    }

}