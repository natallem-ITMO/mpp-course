import kotlinx.atomicfu.*
import java.lang.IndexOutOfBoundsException

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<Moving<E>>(INITIAL_CAPACITY))
    private val exploring = atomic(false)

    override fun get(index: Int): E {
//        print("get " + index  + "\n")
        if (size <= index){
            throw IllegalArgumentException("Index is too big")
        }
        val cur_array = core.value
        val cur_value = cur_array.array[index].value
        if (cur_value == null) {
            throw IllegalArgumentException("Index is too big")
        }
        return cur_value.value
        /*
        надо проверить размер, если размер больше чем текущий индекс, то он и дальше будет больше, просто берем,
        если у нас заглушка, то все равно возвращаем элемент и все.
         */
    }

    override fun put(index: Int, element: E) {
        val new_value = Moving<E>(element)
        while (true) {
//            print("put loop " + index +  " " + element + "\n")
            if (size <= index){
                throw IllegalArgumentException("Index is too big")
            }
            val cur_array = core.value
//            if (cur_array.capacity <= index) {
//                throw IllegalArgumentException("Index is too big")
//            }
            val cur_value = cur_array.array[index].value
            if (cur_value == null) {
                throw IllegalArgumentException("Index is too big")
            }
            if (cur_value.isMoving) {
                continue
            }
            if (cur_array.array[index].compareAndSet(cur_value, new_value)) {
                return
            }
        }
        /*
        надо проверить размер, если размер больше тем текущий, выкидываем IllegalArgumentException
        иначе считываем текущий элемент, если он перемещаемый, пробуем заново.
        если он не перемещаемый, то касом присваиваем новый элемент
         */
    }

    override fun pushBack(element: E) {
        val new_value = Moving(element)
        while (true) {
//            print("pushback loop " + element+ "\n")
            val cur_core = core.value
            val cur_size = cur_core.size.value
            if (cur_size < cur_core.capacity) {
                if (cur_core.array[cur_size].value != null) {
                    continue
                }
                if (cur_core.array[cur_size].compareAndSet(null, new_value)) {
                    if (!cur_core.size.compareAndSet(cur_size, cur_size + 1)) {
                        print("ERROR!!!(0)")
                    }
//                    print("LOG : pushed " + element+ "\n")
                    return
                }
            } else {
                val cur_exploring = exploring.value
                if (!cur_exploring) {
                    if (exploring.compareAndSet(false, true)) {
                        val new_core = Core<Moving<E>>(cur_core.capacity * 2)
                        if (cur_size != cur_core.capacity) {
                            print("ERROR!!!(1)")
                        }
                        for (i in 0 until cur_size) {
                            my_while@ while (true) {
//                                print("pushback_copy_"+i+" loop\n")
                                val cur_value_i = cur_core.array[i].value
                                if (cur_value_i != null) {
                                    val cur_value_i_moving = Moving(cur_value_i.value)
                                    cur_value_i_moving.isMoving = true
                                    if (cur_core.array[i].compareAndSet(cur_value_i, cur_value_i_moving)) {
                                        new_core.array[i].compareAndSet(null, cur_value_i)
                                        break@my_while
                                    }
                                }
                            }
                        }
                        new_core.size.compareAndSet(0, cur_size)
                        core.compareAndSet(cur_core, new_core)
                        exploring.compareAndSet(true, false)
                    }
                }
            }
        }
    }

    override val size: Int
        get() {
            return core.value.size.value
        }
}

private class Core<E>(
    init_capacity: Int
) {
    val array = atomicArrayOfNulls<E>(init_capacity)
    val capacity = init_capacity
    val size = atomic(0)
}

private class Moving<E>(val value: E) {
    var isMoving: Boolean = false
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME
