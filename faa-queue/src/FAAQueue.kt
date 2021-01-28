import kotlinx.atomicfu.*

class FAAQueue<T> {
    private val head: AtomicRef<Segment>
    private val tail: AtomicRef<Segment>

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val tail = tail.value
            val endInd = tail.enqIdx.addAndGet(1) - 1
            if (endInd >= SEGMENT_SIZE) {
                val new_Segment = Segment(x)
                val enqueued  = tail.next.compareAndSet(null,new_Segment)
                this.tail.compareAndSet(tail, tail.next.value!!)
                if (enqueued) return
                //todo decrease endIndx?
            } else {
                if (tail.elements[endInd].compareAndSet(null, x)) {
                    return
                }
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        myLoop@ while (true){
            val head = head.value
            val deqInd = head.deqIdx.addAndGet(1) - 1
            if (deqInd >= SEGMENT_SIZE){
                val headNext = head.next.value
                if (headNext == null){
                    return null
                }
                this.head.compareAndSet(head, headNext)
                continue
            }
            while(true){
                val cur_value = head.elements[deqInd].value
                if (head.elements[deqInd].compareAndSet(cur_value, DONE)){
                    if (cur_value == null){
                        continue@myLoop
                    }
                    return cur_value as T?
                }
            }
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            val head = head.value
            val deqInd = head.deqIdx.value
            if (deqInd >= SEGMENT_SIZE){
                val headNext = head.next.value
                if (headNext == null){
                    return true
                }
            }
            return false
        }
}

private class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val enqIdx = atomic(0) // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor()

    constructor(x: Any?) {
        enqIdx.compareAndSet(0,1)
        elements[0].compareAndSet(null, x)
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE

}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

