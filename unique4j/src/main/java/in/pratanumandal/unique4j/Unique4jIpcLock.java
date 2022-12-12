package in.pratanumandal.unique4j;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

class Unique4jIpcLock implements Unique4jLock {

    /** system temporary directory path */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

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
    public boolean tryLock() {
        // try to lock file
        boolean locked0;
        try {
            lockRaf = new RandomAccessFile(config.getLockFile(), "rws");
            fileLock = lockRaf.getChannel().tryLock();
            locked.set(locked0 = fileLock != null);
        } catch (IOException | OverlappingFileLockException e) {
            locked0 = false;
        }

        if (locked0) {
            // locked file, we are the first to arrive
            // try to start server
            startServer();
        } else {
            // couldn't lock file, we are not the first instance
            // try to start client
            doClient();
        }

        return locked0;
    }

    private void startServer() {
        // try to start the server
        try {
            server = config.getIpcFactory().createIpcServer(new File(TEMP_DIR), config.getAppId());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

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
                    } catch (IOException e) {
                        config.getExceptionHandler().unexpectedException(server, null, e);
                    }
                });
            }
        });
    }

    private void doClient() {
        // try to establish connection to server
        final IpcClient client0;
        try {
            client0 = config.getIpcFactory().createIpcClient(new File(TEMP_DIR), config.getAppId());
        } catch (IOException e) {
            // connection failed try to start server
            startServer();
            return;
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

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!validResponseFound) {
            // validation failed, this is the first instance
            startServer();
        }
    }

    @Override
    public void unlock() {
        if(!locked.getAndSet(false))
            throw new UnsupportedOperationException("Lock wasn't acquired by this app instance");

        try {
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
            Files.deleteIfExists(config.getLockFile().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
