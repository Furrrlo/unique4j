package in.pratanumandal.unique4j;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

class Unique4jIpcLock implements Unique4jLock {

    private static final int MAX_CLIENT_TRIES = 5;

    public final ImmutableConfig config;
    private final FirstInstance firstInstanceHandler;
    private final OtherInstance otherInstanceHandler;

    private final AtomicBoolean locked = new AtomicBoolean();
    /** lock server socket */
    private IpcServer server;

    /** lock file RAF object */
    private RandomAccessFile lockRaf;

    /** file lock for the lock file RAF object */
    private FileLock fileLock;

    public Unique4jIpcLock(Unique4jConfig config,
                           FirstInstance firstInstanceHandler,
                           OtherInstance otherInstanceHandler) {
        this(new ImmutableConfig(config), firstInstanceHandler, otherInstanceHandler);
    }

    public Unique4jIpcLock(ImmutableConfig config,
                           FirstInstance firstInstanceHandler,
                           OtherInstance otherInstanceHandler) {
        this.config = config;
        this.firstInstanceHandler = firstInstanceHandler;
        this.otherInstanceHandler = otherInstanceHandler;
    }

    @Override
    public boolean tryLock() throws IOException {
        for(int tries = 0; ; tries++) {
            // try to lock file
            boolean locked0;
            Throwable notLockedException = null;
            try {
                // Create the parent folder
                Files.createDirectories(config.getLockFolder().toPath());
                lockRaf = new RandomAccessFile(getLockFile(), "rws");
                fileLock = lockRaf.getChannel().tryLock();
                locked.set(locked0 = fileLock != null);
            } catch (IOException | OverlappingFileLockException e) {
                notLockedException = e;
                locked0 = false;
            }

            if (locked0) {
                // locked file, we are the first to arrive
                // try to start server
                startServer();
                return true;
            }

            // couldn't lock file, we are not the first instance
            // try to start client
            try {
                doClient();
                return false;
            } catch (RetryLockException ex) {
                if(tries < MAX_CLIENT_TRIES)
                    continue;

                final Throwable cause = ex.getCause();
                if(notLockedException != null)
                    cause.addSuppressed(notLockedException);

                if(cause instanceof IOException)
                    throw (IOException) cause;
                if(cause instanceof Error)
                    throw (Error) cause;
                if(cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                throw new RuntimeException("Failed to start IPC client", cause);
            }
        }
    }

    private void startServer() throws IOException {
        // try to start the server
        server = config.getIpcFactory().createIpcServer(config.getLockFolder(), config.getAppId());

        // server created successfully; this is the first instance
        // keep listening for data from other instances
        config.getExecutorService().submit(() -> {
            while (!server.isClosed()) {
                final IpcClient client0;
                try {
                    // establish connection
                    client0 = server.accept();
                } catch (IOException e) {
                    if (!server.isClosed())
                        config.getExceptionHandler().unexpectedException(server, null, e);
                    continue;
                }

                // handle socket on a different thread to allow parallel connections
                config.getExecutorService().submit(() -> {
                    try(
                            final IpcClient client = client0;
                            final DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                            final DataInputStream dis = new DataInputStream(client.getInputStream())
                    ) {
                        try {
                            // read message length from client
                            dis.readInt();
                            // write response to client
                            dos.writeUTF(config.getAppId());
                            dos.flush();
                        } catch (IOException e) {
                            config.getExceptionHandler().unexpectedException(server, client, e);
                            return;
                        }

                        if(firstInstanceHandler != null)
                            firstInstanceHandler.onOtherInstanceStarted(client);
                    } catch (Throwable t) {
                        config.getExceptionHandler().unexpectedException(server, null, t);
                    }
                });
            }
        });
    }

    private void doClient() throws RetryLockException, IOException {
        // try to establish connection to server
        final IpcClient client0;
        try {
            client0 = config.getIpcFactory().createIpcClient(config.getLockFolder(), config.getAppId());
        } catch (IOException e) {
            // connection failed, re-try to get the lock cause maybe it was just released
            throw new RetryLockException(e);
        }

        boolean validResponseFound;
        // connection successful try to connect to server
        try(
                final IpcClient client = client0;
                final DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                final DataInputStream dis = new DataInputStream(client.getInputStream())
        ) {
            // write message to server
            dos.writeInt(-1);
            dos.flush();

            // read response string from server
            String response;
            try {
                response = dis.readUTF();
            } catch (EOFException | UTFDataFormatException ex) {
                response = null;
            }

            validResponseFound = response != null && response.equals(config.getAppId());

            if(validResponseFound && otherInstanceHandler != null)
                otherInstanceHandler.onFirstInstanceFound(client);

        }

        if (!validResponseFound) {
            // validation failed, the first instance might be shutting down
            // re-try to get the lock cause maybe it was just released
            throw new RetryLockException(new IOException("Received invalid or malformed response from the first instance"));
        }
    }

    private static class RetryLockException extends Exception {

        public RetryLockException(Throwable cause) {
            super(cause);
        }
    }

    @Override
    public void unlock() throws IOException {
        if(!locked.getAndSet(false))
            throw new UnsupportedOperationException("Lock wasn't acquired by this app instance");

        server.close();
        server = null;

        // try to release file lock
        if (fileLock != null)
            fileLock.release();
        fileLock = null;

        // try to close lock file RAF object
        if (lockRaf != null)
            lockRaf.close();
        lockRaf = null;

        // try to delete lock file
        Files.deleteIfExists(getLockFile().toPath());
    }

    @Override
    public boolean isHeldByCurrentAppInstance() {
        return locked.get();
    }

    private File getLockFile() {
        return new File(config.getLockFolder(), "app.lock");
    }
}
