package cs451;

import cs451.message.MessageRange;

public class Test {

    public static void main(String[] args) {
        MessageRange mr = new MessageRange();

        for (int i = 0; i < 100; i += 2) {
            mr.add(i);
        }
        System.out.println(mr);
        mr.add(39);
        System.out.println(mr);
    }

}
