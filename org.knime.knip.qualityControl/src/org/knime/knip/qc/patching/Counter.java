package org.knime.knip.qc.patching;

/*
 * This class represents a counter in which each digit can have a different upper bound.
 * For example a Counter with upper bounds 2 and 3 counts 00, 10, 01, 11, 02, 12.
 */
public class Counter {
        private int[] m_counter;
        private int[] m_boundaries;
        private int m_curDigit;
        private boolean m_isFull;

        public Counter(int[] boundaries) {
                m_boundaries = boundaries;
                m_counter = new int[boundaries.length];
                m_curDigit = 0;
        }

        /*
         * Resets counter
         */
        public void reset() {
                m_counter = new int[m_boundaries.length];
                m_curDigit = 0;
                m_isFull = false;
        }

        /*
         * Increments counter
         */
        public void increment() throws CounterFullException {
                if (m_isFull) {
                        throw new CounterFullException();
                }
                // if current digit reached its boundary
                if (m_counter[m_curDigit] == m_boundaries[m_curDigit] - 1) {
                        // set current digit to 0
                        m_counter[m_curDigit] = 0;
                        // check for each following digit if it reached its limit
                        while (++m_curDigit < m_counter.length && m_counter[m_curDigit] == m_boundaries[m_curDigit] - 1)
                                m_counter[m_curDigit] = 0;
                        // increment current digit
                        m_counter[m_curDigit]++;
                        // set current digit to first digit
                        m_curDigit = 0;
                } else {
                        // increment current digit
                        m_counter[m_curDigit]++;
                }

                // check if counter is full
                boolean isFull = true;
                for (int i = 0; i < m_counter.length; i++) {
                        if (m_counter[i] < m_boundaries[i] - 1) {
                                isFull = false;
                                break;
                        }
                }
                m_isFull = isFull;
        }

        /*
         * Returns the number of digits of the counter
         * Output: number of digits
         */
        public int getNumDigits() {
                return m_counter.length;
        }

        /*
         * Returns whether counter is full
         * Output: boolean that indicates whether counter is full
         */
        public boolean isFull() {
                return m_isFull;
        }

        /*
         * Returns value of the digit at index index
         * Parameter:   index: index of the digit to return
         * Output:  value of digit
         */
        public int getDigit(int index) {
                return this.m_counter[index];
        }

        /*
         * Prints the current value for each digit
         */
        public void print() {
                for (int i = 0; i < this.m_counter.length; i++)
                        System.out.print(m_counter[i]);
        }

}
