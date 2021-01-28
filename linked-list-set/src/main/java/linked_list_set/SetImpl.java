package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {

    private final int inf = Integer.MAX_VALUE;
    private final int minus_inf = Integer.MIN_VALUE;

    private final AtomicRef<AbstractNode> head = new AtomicRef<>((AbstractNode) new Node(minus_inf, new Node(inf, null)));

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.second.getX() == x)
                return false;
            Node node = new Node(x, w.second);
            if (w.first.getNext().compareAndSet(w.second, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.second.getX() != x) {
                return false;
            }
            AbstractNode nextNode = w.second.getNext().getValue();
            if (nextNode instanceof Removed) {
                return false;
            }
            Removed second_removed = new Removed(nextNode);
            if (w.second.getNext().compareAndSet(nextNode, second_removed)) {
                w.first.getNext().compareAndSet(w.second, nextNode);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        return findWindow(x).second.getX() == x;
    }

    Window findWindow(int x) {
        Window w = new Window();
        my_loop:
        while (true) {
            w.first = head.getValue();
            w.second = w.first.getNext().getValue();
            while (w.second.getX() < x) {
                AbstractNode hipothetic_second = w.first.getNext().getValue();
                if (hipothetic_second instanceof Removed) {
                    continue my_loop;
                }
                AbstractNode next_second = w.second.getNext().getValue();
                if (next_second instanceof Removed) {
                    AbstractNode real_next = next_second.getNext().getValue();
                    if (w.first.getNext().compareAndSet(w.second, real_next)) {
                        w.second = real_next;
                    } else {
                        continue my_loop;
                    }
                } else {
                    w.first = w.second;
                    w.second = w.first.getNext().getValue();
                }
            }
            AbstractNode nodeNext = w.second.getNext().getValue();
            if (nodeNext instanceof Removed) {
                AbstractNode nn = nodeNext.getNext().getValue();
                if (!(nn instanceof Removed))
                    w.first.getNext().compareAndSet(w.second, nn);
                continue;
            }
            return w;
        }
    }

    interface AbstractNode{
        int getX();
        AtomicRef<AbstractNode> getNext();
    }

    static class Node implements AbstractNode {
        AtomicRef<AbstractNode> next;
        int x;
        public Node(int x, AbstractNode next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }

        public Node(int x) {
            this.x = x;
            this.next = null;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public AtomicRef<AbstractNode> getNext() {
            return next;
        }
    }

    class Window {
        AbstractNode first, second;
    }

    class Removed implements AbstractNode {
        AtomicRef<AbstractNode> next;

        public Removed(AbstractNode next) {
            this.next = new AtomicRef<>(next);
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public AtomicRef<AbstractNode> getNext() {
            return next;
        }
    }
}

