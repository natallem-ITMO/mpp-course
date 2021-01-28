import Consensus
import java.lang.ThreadLocal
/**
 * @author : Лемешкова Наталья
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля
    private val root : Node = Node(0)
    private val last: ThreadLocal<Node> = ThreadLocal()
    init {
        last.set(root)
    }

    override fun getAndAdd(x: Int): Int {
        if (last.get() == null){
            last.set(root)
        }
        var prev = 0
        do {
            val prev_value : Int = last.get().value
            prev = prev_value
            val new_node : Node = Node(prev_value + x)
            last.set(last.get().next.decide(new_node))
        } while (last.get() != new_node)
        return prev
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(a : Int) {
        val value : Int  = a
        val next :  Consensus<Node> =  Consensus()
    }
}
