import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import java.lang.IllegalArgumentException
import java.util.*

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val head: AtomicRef<Node>
    private val tail: AtomicRef<Node>

    private inner class Node constructor(val x: E?, val cont: Continuation<Boolean>?, val isSender: Boolean) {
        val next: AtomicRef<Node?>
        val value: AtomicRef<E?> = atomic(x)

        init {
            next = atomic(null)
        }
    }

    init {
        val dummy: Node = Node(null, null, true)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    override suspend fun send(element: E) {
        while (true) {
            val curTail = tail.value
            var curHead = head.value
            if (curTail == curHead || curTail.isSender) {
                var toPutNode: Node?
                val res = suspendCoroutine<Boolean> sc@{ cont ->
                    toPutNode = Node(element, cont, true)
                    val shouldRetry = !(tryEnqueue(curTail, toPutNode!!))
                    if (shouldRetry) {
                        cont.resume(false)
                        return@sc
                    }
                }
                if (res){
                    curHead = head.value
                    if (toPutNode!! == curHead.next.value) {
                        head.compareAndSet(curHead, toPutNode!!)
                    }
                    return
                }
            } else {
                val takenNode = tryDequeue(curHead, curTail) ?: continue
                if( takenNode.value.compareAndSet(null, element)){
                    takenNode.cont!!.resume(true)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val curTail = tail.value
            var curHead = head.value
            if (curTail == curHead || !curTail.isSender) {
                var toPutNode: Node?
                val res = suspendCoroutine<Boolean> sc@{ cont ->
                    toPutNode = Node(null, cont, false)
                    val shouldRetry = !(tryEnqueue(curTail, toPutNode!!))
                    if (shouldRetry) {
                        cont.resume(false)
                        return@sc
                    }
                }
                if (res){
                    curHead = head.value
                    if (toPutNode!! == curHead.next.value) {
                        head.compareAndSet(curHead, toPutNode!!)
                    }
                    return toPutNode!!.value.value!!
                }
            } else {
                val takenNode = tryDequeue(curHead, curTail) ?: continue
                val element = takenNode.value.value ?: continue
                if (takenNode.value.compareAndSet(element, null)) {
                    takenNode.cont!!.resume(true)
                    return element
                }
            }
        }
    }


    private fun tryEnqueue(cur_tail: Node, new_node: Node): Boolean {
        val next = cur_tail.next.value
        if (next == null) {
            if (cur_tail.next.compareAndSet(next, new_node)) {
                tail.compareAndSet(cur_tail, new_node)
                return true
            }
            return false
        } else {
            tail.compareAndSet(cur_tail, next)
            return false
        }
    }

    private fun tryDequeue(cur_head: Node, cur_tail:Node): Node? {
        if (cur_head != head.value || cur_tail != tail.value) {
            return null
        }
        val next_head_node = cur_head.next.value
        if (cur_head == cur_tail) {
            if (next_head_node == null) {
                return null
            } else {
                tail.compareAndSet(cur_tail, next_head_node)
                return null
            }
        } else {
            if (head.compareAndSet(cur_head, next_head_node!!)) {
                return next_head_node
            }
            return null
        }
    }

}
