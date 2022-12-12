package in.pratanumandal.unique4j;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

class Unique4jIpcLock implements Unique4jLock {

    /** system temporary directory path */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public final String appId;
    public final File lockFile;
    private final IpcFactory ipcFactory;
    private final ExecutorService executorService;
    private final FirstInstance firstInstanceHandler;
    private final OtherInstance otherInstanceHandler;
    private final UnexpectedExceptionHandler exceptionHandler;


    private final AtomicBoolean locked = new AtomicBoolean();
    /** lock server socket */
    private IpcServer server;

    /** lock file RAF object */
    private RandomAccessFile lockRaf;

    /** file lock for the lock file RAF object */
    private FileLock fileLock;

    /**
     * Parameterized constructor.<br>
     * This constructor allows to explicitly specify the exit strategy for subsequent instances.<br><br>
     *
     * The appId must be as unique as possible.
     * Avoid generic names like "my_app_id" or "hello_world".<br>
     * A good strategy is to use the entire package name (group ID + artifact ID) along with some random characters.
     *
     * @param appId Unique string representing the application ID
     */
    public Unique4jIpcLock(String appId,
                           File lockFile,
                           IpcFactory ipcFactory,
                           ExecutorService executorService,
                           FirstInstance firstInstanceHandler,
                           OtherInstance otherInstanceHandler,
                           UnexpectedExceptionHandler exceptionHandler) {
        this.appId = appId;
        this.lockFile = lockFile;
        this.ipcFactory = ipcFactory;
        this.executorService = executorService;
        this.firstInstanceHandler = firstInstanceHandler;
        this.otherInstanceHandler = otherInstanceHandler;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public boolean tryLock() {
        // try to lock file
        boolean locked0;
        try {
            lockRaf = new RandomAccessFile(lockFile, "rws");
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
            server = ipcFactory.createIpcServer(new File(TEMP_DIR), appId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // server created successfully; this is the first instance
        // keep listening for data from other instances
        executorService.submit(() -> {
            while (!server.isClosed()) {
                final IpcClient client0;
                try {
                    // establish connection
                    client0 = server.accept();
                } catch (IOException e) {
                    if (!server.isClosed())
                        exceptionHandler.unexpectedException(server, null, e);
                    continue;
                }

                // handle socket on a different thread to allow parallel connections
                executorService.submit(() -> {
                    try(
                            final IpcClient client = client0;
                            final DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                            final DataInputStream dis = new DataInputStream(client.getInputStream())
                    ) {
                        try {
                            // read message length from client
                            dis.readInt();
                            // write response to client
                            dos.writeUTF(appId);
                            dos.flush();
                        } catch (IOException e) {
                            exceptionHandler.unexpectedException(server, client, e);
                            return;
                        }

                        if(firstInstanceHandler != null)
                            firstInstanceHandler.onOtherInstanceStarted(client);
                    } catch (IOException e) {
                        exceptionHandler.unexpectedException(server, null, e);
                    }
                });
            }
        });
    }

    private void doClient() {
        // try to establish connection to server
        final IpcClient client0;
        try {
            client0 = ipcFactory.createIpcClient(new File(TEMP_DIR), appId);
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

            validResponseFound = response != null && response.equals(appId);

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
            Files.deleteIfExists(lockFile.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
