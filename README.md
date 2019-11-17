# Unique4j

Unique4j is a cross-platform Java library to allow only single instance of a Java application to run and enable communication between first instance and subsequent instances.

It is compatible with Java 1.6+ and is platform independent.

<br>

## Dependency Management

### Maven

    <dependency>
        <groupId>tk.pratanumandal</groupId>
        <artifactId>unique4j</artifactId>
        <version>1.2.1</version>
    </dependency>

### Gradle

    dependencies {
        implementation 'tk.pratanumandal:unique4j:1.2.1'
    }

<br>

## How To Use

Declare an application unique ID which is a common constant for all the instances. This ID must be as unique as possible. A good strategy is to use the entire package name (group ID + artifact ID) along with some random characters.

    // unique application ID
    public static String APP_ID = "tk.pratanumandal.unique4j-mlsdvo-20191511-#j.6";

<br>

Create an instance of <code>Unique</code> class.

    // create unique instance
    Unique unique = new Unique(APP_ID) {
        @Override
        public void receiveMessage(String message) {
            // print received message (timestamp)
            System.out.println(message);
        }

        @Override
        public String sendMessage() {
            // send timestamp as message
            Timestamp ts = new Timestamp(new Date().getTime());
            return "Another instance launch attempted: " + ts.toString();
        }
        
        /* It is not mandatory to override this method
         * By default, the stack trace is printed
         */
        @Override
        public void handleException(Exception exception) {
            // display the exception message
            System.out.println(exception.getMessage());
        }

        /* It is not mandatory to override this method
         * By default, the subsequent instance simply exits
         * This method is not invoked if AUTO_EXIT is turned off
         */
        @Override
        public void beforeExit() {
            // display exit message
            System.out.println("Exiting subsequent instance.");
        }
    };
   
<br>

Alternatively, you can declare to turn off automatic exit for subsequent instances.

    // create unique instance with AUTO_EXIT turned off
    Unique unique = new Unique(APP_ID,
                               false) // second parameter is for AUTO_EXIT (false turns it off)
    { 
        ...
        // Note: beforeExit() method, even if overridden, is never invoked if AUTO_EXIT is turned off
    }
    
<br>

Try to obtain a lock using the <code>Unique</code> object.
    
    // try to obtain lock
    boolean lockFlag = false;
    try {
        lockFlag = unique.acquireLock();
    } catch (Unique4jException e) {
        e.printStackTrace();
    }
    
    // perform long running tasks here
    
<br>

Free the lock using the <code>Unique</code> object.
    
    // long running tasks end here
    
    // try to free the lock before exiting program
    boolean lockFreeFlag = false;
    try {
        lockFreeFlag = unique.freeLock();
    } catch (Unique4jException e) {
        e.printStackTrace();
    }

<br>

## Demonstration

To put it all together, the following is a complete example of the usage and capabilities of Unique4j.

    import java.sql.Timestamp;
    import java.util.Date;
    
    import tk.pratanumandal.unique4j.Unique;
    import tk.pratanumandal.unique4j.exception.Unique4jException;
    
    public class Unique4jDemo {
    
        // unique application ID
        public static String APP_ID = "tk.pratanumandal.unique4j-mlsdvo-20191511-#j.6";

        public static void main(String[] args) throws Unique4jException, InterruptedException {

            // create unique instance
            Unique unique = new Unique(APP_ID) {
                @Override
                public void receiveMessage(String message) {
                    // print received message (timestamp)
                    System.out.println(message);
                }

                @Override
                public String sendMessage() {
                    // send timestamp as message
                    Timestamp ts = new Timestamp(new Date().getTime());
                    return "Another instance launch attempted: " + ts.toString();
                }

                @Override
                public void handleException(Exception exception) {  // this method is optional
                    // display the exception message
                    System.out.println(exception.getMessage());
                }

                @Override
                public void beforeExit() {  // this method is optional
                    // display exit message
                    System.out.println("Exiting subsequent instance.");
                }
            };

            // try to obtain lock
            boolean lockFlag = unique.acquireLock();

            // sleep the main thread for 30 seconds to simulate long running tasks
            Thread.sleep(30000);

            // try to free the lock before exiting program
            boolean lockFreeFlag = unique.freeLock();

        }
	
    }
