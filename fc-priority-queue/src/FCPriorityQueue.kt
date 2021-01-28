import java.util.*

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.random.Random
import kotlinx.atomicfu.atomic;

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val lock = atomic<Boolean>(false)
    private val elements = AtomicReferenceArray<CombinerOperation<E>>(QUEUE_SIZE)
//    val operationIndex = atomic(0)

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {

        val my_result: E?
        if (lock.compareAndSet(false, true)) {
            my_result = q.poll()
            combinerProcess()
            lock.compareAndSet(true, false);
            return my_result
        } else {
            val operation = PollOperation<E>()
            val index: Int = publishOperation(operation)
            while (true) {
                if (operation.result != null) {
                    elements.compareAndSet(index, operation, null)
                    return operation.result
                }
                if (lock.compareAndSet(false, true)) {
                    elements.compareAndSet(index, operation, null)
                    if (operation.result != null) {
                        my_result = operation.result
                    } else {
                        my_result = q.poll()
                    }
                    combinerProcess()
                    lock.compareAndSet(true, false);
                    return my_result
                }

            }
        }
    }

    private fun publishOperation(operation: CombinerOperation<E>): Int {
        while (true) {
            val random = Random.nextInt(0, QUEUE_SIZE)
//            val random = (0 until QUEUE_SIZE).random()
            if (elements.compareAndSet(random, null, operation)) {
                return random
            }
            for (i in random + 1 until QUEUE_SIZE) {
                if (elements.compareAndSet(i, null, operation)) {
                    return i
                }
            }
            for (i in random - 1 downTo 0) {
                if (elements.compareAndSet(i, null, operation)) {
                    return i
                }
            }
        }
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        val my_result: E?
        if (lock.compareAndSet(false,true)) {
            my_result = q.peek()
            combinerProcess()
            lock.compareAndSet(true, false);
            return my_result
        } else {
            val operation = PeekOperation<E>()
            val index: Int = publishOperation(operation)
            while (true) {
                if (operation.result != null) {
                    elements.compareAndSet(index, operation, null)
                    return operation.result
                }
                if (lock.compareAndSet(false,true)) {
                    elements.compareAndSet(index, operation, null)
                    if (operation.result != null) {
                        my_result = operation.result
                    } else {
                        my_result = q.peek()
                    }
                    combinerProcess()
                    lock.compareAndSet(true, false);
                    return my_result
                }

            }
        }
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        if (lock.compareAndSet(false,true)) {
            q.add(element)
            combinerProcess()
            lock.compareAndSet(true, false);
            return
        } else {
            val operation = AddOperation<E>(element)
            val index: Int = publishOperation(operation)
            while (true) {
                if (operation.toAdd == null) {
                    elements.compareAndSet(index, operation, null)
                    return
                }
                if (lock.compareAndSet(false,true)) {
                    elements.compareAndSet(index, operation, null)
                    if (operation.toAdd != null) {
                        q.add(element)
                    }
                    combinerProcess()
                    lock.compareAndSet(true, false);
                    return
                }

            }
        }
    }

    fun combinerProcess() {
        for (i in 0 until QUEUE_SIZE) {
            val curOperation = elements.get(i) ?: continue
            if (curOperation is PollOperation) {
                if (curOperation.result == null) {
                    curOperation.result = q.poll()
                }
                continue

            }
            if (curOperation is PeekOperation) {
                if (curOperation.result == null) {
                    curOperation.result = q.peek()
                }
                continue
            }
            if (curOperation is AddOperation) {
                if (curOperation.toAdd != null) {
                    q.add(curOperation.toAdd)
                    curOperation.toAdd = null
                }
                continue
            }
        }
    }
}

private open class CombinerOperation<E> {
}

private class PollOperation<E> : CombinerOperation<E>() {
    @Volatile
    var result: E? = null
//    var result: AtomicReference<E?> = AtomicReference(null)
}

private class PeekOperation<E> : CombinerOperation<E>() {
    @Volatile
    var result: E? = null
    //    var result: AtomicReference<E?> = AtomicReference(null)

}

private class AddOperation<E>(element: E) : CombinerOperation<E>() {
    @Volatile
    var toAdd: E? = element
}

const val QUEUE_SIZE = 100 // DO NOT CHANGE, IMPORTANT FOR TESTS
