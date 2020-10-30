package cs451;

import cs451.message.MessageRange;

public class Test {

    public static void main(String[] args) {
        MessageRange mr = new MessageRange();

        mr.add(1);
        System.out.println(mr);
        mr.add(2);
        System.out.println(mr);
        mr.add(3);
        System.out.println(mr);
        mr.add(3);
        System.out.println(mr);
        mr.add(0);
        System.out.println(mr);

        mr.add(-2);
        System.out.println(mr);
        mr.add(5);
        System.out.println(mr);

        System.out.println("-10: " + mr.contains(-10));
        System.out.println("-2: " + mr.contains(-2));
        System.out.println("0: " + mr.contains(0));
        System.out.println("1: " + mr.contains(1));
        System.out.println("4: " + mr.contains(4));
        System.out.println("5: " + mr.contains(5));
        System.out.println("10: " + mr.contains(10));

        mr.add(-1);
        System.out.println(mr);
        mr.add(4);
        System.out.println(mr);
        mr.peek();
        System.out.println(mr);
        mr.poll();
        System.out.println(mr);

    }
    
}
