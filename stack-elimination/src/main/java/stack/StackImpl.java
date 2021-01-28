package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StackImpl implements Stack {

    static final int eliminationListSize = 100;
    static final int waitingInLoopOperationNumber = 10000;
    static final int watchInListNumber = eliminationListSize / 2;
    static final long emptyEliminationValue = Integer.MAX_VALUE + 1L;


    static final Random random = new Random();
    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final List<AtomicRef<Long>> eliminationList = new ArrayList<>(Collections.nCopies(eliminationListSize, new AtomicRef<>(emptyEliminationValue)));


    private int getRandomNum() {
        return random.nextInt(eliminationListSize);
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }


    @Override
    public void push(int x) {
        int s = getRandomNum();
        for (int i = 0; i < watchInListNumber; i++) {
            int currentIndex = (s + i) % eliminationListSize;
            Long cur_value = eliminationList.get(currentIndex).getValue();
            if (cur_value != emptyEliminationValue) {
                continue;
            }
            if (eliminationList.get(currentIndex).compareAndSet(emptyEliminationValue, (long) x)) {
                int counter = 0;
                while (counter != waitingInLoopOperationNumber) {
                    ++counter;
                }
                if (eliminationList.get(currentIndex).compareAndSet(emptyEliminationValue, emptyEliminationValue)) {
                    return;
                } else {
                    simplePush(x);
                }
            }
        }
        simplePush(x);
    }

    private void simplePush(int x) {
        while (true) {
            Node curHead = head.getValue();
            Node newNode = new Node(x, curHead);
            if (head.compareAndSet(curHead, newNode)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int s = getRandomNum();
        for (int i = 0; i < watchInListNumber; i++) {
            int currentIndex = (s + i) % eliminationListSize;
            Long cur_value = eliminationList.get(currentIndex).getValue();
            if (cur_value == emptyEliminationValue) {
                continue;
            }
            if (eliminationList.get(currentIndex).compareAndSet(cur_value, emptyEliminationValue)) {
                return cur_value.intValue();
            }
        }
        return simplePop();
    }

    private int simplePop() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }


    public static void main(String[] args) {
        StackImpl impl = new StackImpl();
        impl.check();
    }

    private void check() {
        System.out.println(Integer.MAX_VALUE);
        System.out.println(Long.MAX_VALUE);
    }
}