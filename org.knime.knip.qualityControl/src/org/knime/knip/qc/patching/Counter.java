package org.knime.knip.qc.patching;

public class Counter {
        private int[] counter;
        private int[] boundary;
        private int curDigit;

        public Counter(int[] boundary) {
                this.boundary = boundary;
                this.counter = new int[boundary.length];
                this.curDigit = 0;
        }

        /*
         * Resets counter
         */
        public void reset() {
                this.counter = new int[boundary.length];
                this.curDigit = 0;
        }

        /*
         * Increments counter
         */
        public void increment() {
                if (curDigit == counter.length) {
                        return;
                }
                if (counter[curDigit] == boundary[curDigit] - 1) {
                        counter[curDigit] = 0;
                        while (++curDigit < counter.length && counter[curDigit] == boundary[curDigit] - 1)
                                counter[curDigit] = 0;
                        if (curDigit == counter.length) {
                                return;
                        }
                        counter[curDigit]++;
                        curDigit = 0;
                } else {
                        counter[curDigit]++;
                }

        }

        /*
         * Returns value of the digit at index index
         * Parameter:   index: index of the digit to return
         * Output:  value of digit
         */
        public int getDigit(int index) {
                return this.counter[index];
        }

        /*
         * Prints the current value for each digit
         */
        public void print() {
                for (int i = 0; i < this.counter.length; i++)
                        System.out.print(counter[i]);
        }

}
