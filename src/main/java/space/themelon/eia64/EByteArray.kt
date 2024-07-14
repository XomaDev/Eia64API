package space.themelon.eia64

class EByteArray {

    private var array = ByteArray(50)
    private var size = 50
    private var index = 0

    fun get(): ByteArray {
        return array.copyOfRange(0, index)
    }

    fun put(byte: Byte) {
        if (index == size) {
            resize()
        }
        array[index++] = byte
    }

    fun delete() {
        if (index != 0) index--
    }

    private fun resize() {
        val newCap = size * 2
        val allocation = ByteArray(newCap)
        for (i in 0..<size) {
            allocation[i]  = array[i]
        }
        size = newCap
        array = allocation
    }
}