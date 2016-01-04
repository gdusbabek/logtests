import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final Random RAND = new Random(System.nanoTime());
    private static Logger log = LoggerFactory.getLogger(Main.class);
    private static final List<String> STRING_POOL = new ArrayList<String>() {{
        Random r = new Random(123456789L);
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        add(randString(10, r));
        System.out.println(this);
    }};
    
    
    private static String randString(int len) { return randString(len, RAND); }
    private static String randString(int len, Random r) {
        char[] ch = new char[len];
        for (int i = 0; i < len; i++) {
            ch[i] = CHARS[r.nextInt(CHARS.length)];
        }
        return new String(ch);
    }
    
    public static void main(String args[]) throws Exception {
        final int numThreads = 10;
        final int numMsgs = 1000;
        final long sleepMs = 10;
        final long maxWait = numMsgs * sleepMs * 2;
        
        final CountDownLatch latch = new CountDownLatch(10);
        
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread("logging-thread-" + i) {
                public void run() {
                    MDC.put("xx-pool", STRING_POOL.get(RAND.nextInt(STRING_POOL.size())));
                    for (int j = 0; j < numMsgs; j++) {
                        MDC.put("xx-txn", randString(10));
                        log.info(String.format("Msg %d from \"foo\":\"bar\", thread %s", j, getName()),
                                new LogMsg(randString(5), randString(2), randString(3), randString(5)));
                        try { sleep(sleepMs); } catch (Exception ex) {}
                    }
                    log.error("bad exception", new Exception("Something bad happened here"));
                    MDC.remove("xx-txn");
                    MDC.remove("xx-pool");
                    latch.countDown();
                }
            };
        }
        
        for (Thread th : threads) th.start();
        
        boolean success = latch.await(maxWait, TimeUnit.MILLISECONDS);
        log.info("Success " + success);
    }
    
    public static class LogMsg {
        public String txn;
        public String msg;
        public String foo;
        public String bar;
        
        public LogMsg(String txn, String msg, String foo, String bar) {
            this.txn = txn;
            this.msg = msg;
            this.foo = foo;
            this.bar = bar;
        }
        
    }
    
}
