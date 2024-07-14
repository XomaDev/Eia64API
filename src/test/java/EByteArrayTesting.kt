import space.themelon.eia64.EByteArray

object EByteArrayTesting {
    @JvmStatic
    fun main(args: Array<String>) {
        val array = EByteArray()
        array.put(1)
        array.put(2)
        array.put(3)
        array.delete()
        println(array.get().contentToString())
    }
}