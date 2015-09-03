package org.knime.knip.qc.patching;

/*
 * Exception that is thrown if a full Counter is incremented
 */
public class CounterFullException extends Exception {

        /**
         * Default serialVersionUID
         */
        private static final long serialVersionUID = 1L;

        public CounterFullException() {
        }

        public CounterFullException(String arg0) {
                super(arg0);
        }

        public CounterFullException(Throwable arg0) {
                super(arg0);
        }

        public CounterFullException(String arg0, Throwable arg1) {
                super(arg0, arg1);
        }

        public CounterFullException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
                super(arg0, arg1, arg2, arg3);
        }

}
