package msqueue;

import kotlinx.atomicfu.AtomicRef;
//import AtomicMarkableReference;

public class MSQueue implements Queue {
    private final AtomicRef<Node> head;
    private final AtomicRef<Node> tail;
    private static final int empty_value = Integer.MIN_VALUE;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node new_node = new Node(x);
        Node cur_tail;
        while (true) {
            cur_tail = tail.getValue();
            Node next = cur_tail.next.getValue();
            if (next == null) {
                if (cur_tail.next.compareAndSet(next, new_node)) {
                    break;
                }
            } else {
                tail.compareAndSet(cur_tail, next);
            }
        }
        tail.compareAndSet(cur_tail, new_node);
    }

    @Override
    public int dequeue() {
        while (true) {
            Node cur_head = head.getValue();
            Node cur_tail = tail.getValue();
            Node next_head_node = cur_head.next.getValue();
            if (cur_head == cur_tail) {
                if (next_head_node == null) {
                    return empty_value;
                }
                tail.compareAndSet(cur_tail, next_head_node);
            } else {
                int res = next_head_node.x;
                if (head.compareAndSet(cur_head, next_head_node)) {
                    return res;
                }
            }
        }
    }

    @Override
    public int peek() {
        while (true) {
            Node cur_head = head.getValue();
            Node cur_tail = tail.getValue();
            Node next_head_node = cur_head.next.getValue();
            if (head.getValue() == cur_head) {
                if (cur_head == cur_tail) {
                    if (next_head_node == null) {
                        return empty_value;
                    }
                    tail.compareAndSet(cur_tail, next_head_node);
                } else {
                    return next_head_node.x;
                }
            }
        }
    }

    public static void main(String[] args) {
        MSQueue queue = new MSQueue();
        for (int i = 0; i < 10; i++) {
            queue.enqueue(i + 10);
        }
        for (int i = 0; i < 15; i++) {
            System.out.println(queue.peek());
            System.out.println(queue.dequeue());
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            next = new AtomicRef<>(null);

        }
    }
}