import java.util.concurrent.atomic.*

class Solution(val env: Environment) : Lock<Solution.Node> {
    val firstTail: Node = Node()
    val tail: AtomicReference<Node> = AtomicReference<Node>(firstTail)

    override fun lock(): Node {
        val my = Node() // сделали узел
        val pred: Node = tail.getAndSet(my)
        if (pred != firstTail) {
            pred.next.set(my)
            while (my.locked.get()) {
                env.park()
            }
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, firstTail)) {
                return
            } else {
                while (node.next.get() == null) {
//                    env.park()
                }
            }
        }
        node.next.value.locked.set(false)
        env.unpark(node.next.value.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked: AtomicReference<Boolean> = AtomicReference<Boolean>(true)
        val next: AtomicReference<Node> = AtomicReference<Node>(null)
    }


}

/*
cl.ass QNode:
    boolean locked // shared, atomic
    QNode next = null

class CLHLock:
    tail = QNode() // shared, atomic
    treadlocal my = null

    def lock():
        my = QNode()
        pred = tail.getAndSet(my)
        if pred != null:
            pred.next = my
            while my.locked: pass


    def unlock():
        if my.next == null:
            if tail.CAS(my, null): return
            else:
                while my.next == null: pass
        my.next.locked = false
 */

