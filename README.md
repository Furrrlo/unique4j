# Unique4j
![unique4j logo](unique4j.png)

## Introduction

Unique4j is a cross-platform Java library to allow only single instance of a Java application to run and enable
communication between first instance and subsequent instances.

It is compatible with Java 1.8+ and is platform independent.

## Dependency Management

### Maven
```xml
<dependencies>
    <dependency>
        <groupId>tk.pratanumandal</groupId>
        <artifactId>unique4j</artifactId>
        <version>2.0.0</version>
    </dependency>
    <!-- For unix sockets in Java < 16 -->
    <dependency>
        <groupId>tk.pratanumandal</groupId>
        <artifactId>unique4j-junixsocket</artifactId>
        <version>2.0.0</version>
    </dependency>
    <!-- For unix sockets in Java 16+ -->
    <dependency>
        <groupId>tk.pratanumandal</groupId>
        <artifactId>unique4j-unix-socket-channel</artifactId>
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
dependencies {
    implementation 'tk.pratanumandal:unique4j:2.0.0'
    // For unix sockets in Java < 16
    implementation 'tk.pratanumandal:unique4j-junixsocket:2.0.0'
    // For unix sockets in Java 16+
    implementation 'tk.pratanumandal:unique4j-unix-socket-channel:2.0.0'
}
```

## How To Use

The library offers a higher level API, which is easier to use, and a lower lever API
which offers more control.

Regardless of which one you use, the first step is declaring an application unique ID
which is a common constant for all the instances. This ID must be as unique as possible.
A good strategy is to use the entire package name (group ID + artifact ID) along with some random characters.

```java
// unique application ID
public static String APP_ID = "tk.pratanumandal.unique4j-mlsdvo-20191511-#j.8";
```

## High level API

```java

import java.io.IOException;

public class Unique4jDemo {

    // unique application ID
    public static String APP_ID = "tk.pratanumandal.unique4j-mlsdvo-20191511-#j.6";

    public static void main(String[] args) throws IOException {
        // create unique instance
        Unique4j.requestSingleInstance(APP_ID, instance -> instance.firstInstance(ctx -> {
            // This is the first app instance: here the application can be started up.
            // Make swing gui, start stuff up, etc.
            // The lock is already held, no other app instance can take it
            // Any message sent during this init phase will be queued up and re-received afterwards.
            final List<Runnable> onFrameDispose = new ArrayList<>();
            final JFrame frame = new JFrame("Test window") {
                {
                    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                }

                @Override
                public void dispose() {
                    super.dispose();
                    onFrameDispose.forEach(Runnable::run);
                }
            };
            frame.setVisible(true);

            // Register other instances listener
            ctx.otherInstancesListener(otherInstanceClient -> {
                // Make window popup/reply to client
                final DataOutputStream dos = new DataOutputStream(otherInstanceClient.getOutputStream());
                dos.writeUTF("Hello!");

                SwingUtilities.invokeLater(() -> {
                    frame.setExtendedState(JFrame.ICONIFIED);
                    SwingUtilities.invokeLater(() -> frame.setExtendedState(JFrame.NORMAL));
                });
            });

            // Do heavy lifting here:
            // - Start GUI and wait for the user to close it
            return ctx.waitForEvent(onFrameDispose::add);
            // - Do lengthy task that takes a lot of time
            return () -> {
                for (int i = 0; i < 100000; i++)
                    System.out.println("yay");
            };
            // Once this Runnable ends, the instance lock will be released automatically
        }).otherInstances(ctx -> {
            // Called in case this is not the first app instance

            // Register first instance listener, allows to send messages
            ctx.firstInstanceListener(firstInstanceClient -> {
                // Send messages to the first instance
                // There's an open connection here, so shutting down is not supposed to be happening here
                final DataOutputStream dos = new DataOutputStream(firstInstanceClient.getOutputStream());
                dos.writeUTF("I'm another instance");
            });

            // Do other stuff in here, in case it's needed (ex. shutting down, etc)
            return ctx.thenExit(0);
        }));

        // Execution will continue here once either the firstInstance inner lambda or
        // otherInstances inner lambda is executed
    }
}
```

## Low level API

The low level API follows the standard Java Lock pattern:
```java
public class Unique4jDemo {

    // unique application ID
    public static String APP_ID = "tk.pratanumandal.unique4j-mlsdvo-20191511-#j.6";

    public static void main(String[] args) throws IOException {
        final Unique4jLock lock = Unique4j.newLock(
                APP_ID,
                otherInstanceClient -> {
                    // Other instances listener
                    // Here you can reply to clients
                },
                firstInstanceClient -> {
                    // Send messages to the first instance
                    // There's an open connection here, so shutting down is not supposed to be happening here
                });

        if(lock.tryLock()) {
            try {
                // This is the first app instance: here the application can be started up.
                // Make swing gui, start stuff up, etc.
            } finally {
                lock.unlock();
            }
        } else {
            // Do other stuff in here, in case it's needed (ex. shutting down, etc)
        }
    }
}
```

## Configuration

Configuration options can be changed by using `Unique4j#withConfig(Unique4jConfig)`
```java
public class Unique4jDemo {

    // unique application ID
    public static String APP_ID = "tk.pratanumandal.unique4j-mlsdvo-20191511-#j.6";

    public static void main(String[] args) throws IOException {
        final Unique4jLock lock = Unique4j
                .withConfig(Unique4jConfig.createDefault(APP_ID))
                .requestSingleInstance(instance -> { /* ... */ });
        // or
        final Unique4jLock lock = Unique4j.withConfig(Unique4jConfig.createDefault(APP_ID)).newLock(
                otherInstanceClient -> { /* ... */ },
                firstInstanceClient -> { /* ... */ });
    }
}
```

Under the hood, the library uses sockets to do IPC between the different app instances.
By default, it will bind a TCP socket on the first available port starting from the 3000.
This behaviour can be tweaked by specifying a different `IpcFactory` in `Unique4jConfig#ipcFactory(IpcFactory)`.
The base library ships with:
- DynamicPortSocketIpcFactory: binds a TCP socket on the first port it finds available starting from the given one
- StaticPortSocketIpcFactory: binds a TCP socket on the given port

In addition, a unix socket can be used
- In Java 16+ by adding the `tk.pratanumandal:unique4j-unix-socket-channel` Maven artifact, which uses [JEP-380: Unix domain socket channels](https://openjdk.org/jeps/380)
- In Java < 16 by adding the `tk.pratanumandal:unique4j-junixsocket` Maven artifact, which uses the [junixsocket library](https://kohlschutter.github.io/junixsocket/)