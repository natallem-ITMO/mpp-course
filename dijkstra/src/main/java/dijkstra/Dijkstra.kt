package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }
val debug: Boolean = false;

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0

    val multiQueue = arrayListOf(PriorityQueue<Node>(NODE_DISTANCE_COMPARATOR))
    for (i in 0 until (2 * workers)) {
        multiQueue.add(PriorityQueue(NODE_DISTANCE_COMPARATOR));
    }
    multiQueue[0].add(start)
    val counter = AtomicInteger(1);

    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (counter.get() != 0) {
                val cur_node: Node = getNode(multiQueue) ?: continue
               //println("cur_node $cur_node thread ${Thread.currentThread().getId()}")
                for (e in cur_node.outgoingEdges) {
                    while (true) {
                        val cur_dist = cur_node.distance;
                        val edge_w = e.weight;
                        val next_node = e.to;
                        val next_node_dist = next_node.distance
                        if (cur_dist + edge_w < next_node_dist) {
                            if (next_node.casDistance(next_node_dist, cur_dist + edge_w)) {
                                counter.incrementAndGet();
                               //println("(parent $cur_node thread ${Thread.currentThread().getId()}) decreased key for node $next_node from ${next_node_dist} to ${cur_dist + edge_w} and increment ${counter.value}")
                                add_node(multiQueue, next_node);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
                counter.decrementAndGet();
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

fun add_node(list: ArrayList<PriorityQueue<Node>>, nextNode: Node) {
    val random1 = (0 until list.size).random()
    synchronized(list[random1]) {
       //println("added node $nextNode to list[$random1] in thread ${Thread.currentThread().getId()}")
        list[random1].add(nextNode);
    }
}

fun getNode(list: ArrayList<PriorityQueue<Node>>): Node? {
    val random1 = (1 until list.size).random()
    val random2 = (0 until random1).random()
    synchronized(list[random1]) {
        var cur1: Node? = list[random1].peek()
        synchronized(list[random2]) {
            val cur2: Node? = list[random2].peek()
            if (cur2 == null) {
                if (cur1 == null) {
                    return null;
                } else {
                    list[random1].poll();
                    return cur1;
                }
            } else {
                if (cur1 == null) {
                    list[random2].poll();
                    return cur2;
                } else {
                    if (cur1.distance < cur2.distance) {
                        list[random1].poll();
                        return cur1;
                    } else {
                        list[random2].poll();
                        return cur2;
                    }
                }
            }
        }
    }
}