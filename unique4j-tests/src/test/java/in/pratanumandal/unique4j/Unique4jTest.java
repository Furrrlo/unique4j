/*
 * Copyright 2022 Francesco Ferlin
 * Copyright 2019 Pratanu Mandal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package in.pratanumandal.unique4j;

import in.pratanumandal.unique4j.junixsocket.AFUNIXSocketIpcFactory;
import in.pratanumandal.unique4j.unixsocketchannel.UnixSocketChannelIpcFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class Unique4jTest {

	private static final AtomicInteger APP_ID_COUNT = new AtomicInteger();
	public static String getAppId() {
		return "in.pratanumandal.unique4j-mlsdvo-20191511-#j.6-" + APP_ID_COUNT.getAndIncrement();
	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ new DynamicPortSocketIpcFactory(InetAddress.getLoopbackAddress(), 3000) },
				{ new AFUNIXSocketIpcFactory() },
				{ new UnixSocketChannelIpcFactory() }
		});
	}

	private final IpcFactory ipcFactory;

	public Unique4jTest(IpcFactory ipcFactory) {
		this.ipcFactory = ipcFactory;
	}

	@Test
	public void testUnique4jBasic() throws IOException {
		Unique4jLock unique = Unique4jLock.create(
				Unique4jConfig.createDefault(getAppId())
						.ipcFactory(ipcFactory)
						.exceptionHandler((s, c, e) -> {}),
				otherInstanceClient -> {},
				firstInstanceClient -> {}
		);

		assertTrue(unique.tryLock());
		unique.unlock();
	}

	@Test
	public void testUnique4j() throws IOException, InterruptedException {
		doTestUnique4j("ijvnfpp389528$#$@520sdf.213sgv8");
	}

	@Test
	public void testUnique4jEmpty() throws IOException, InterruptedException {
		doTestUnique4j("");
	}

	@Test
	public void testUnique4jNull2() throws IOException, InterruptedException {
		doTestUnique4j("null");
	}

	@Test
	public void testUnique4jNewline1() throws IOException, InterruptedException {
		doTestUnique4j("hello\nworld");
	}

	@Test
	public void testUnique4jNewline2() throws IOException, InterruptedException {
		doTestUnique4j("hello\r\nworld");
	}

	private void doTestUnique4j(String message) throws IOException, InterruptedException {
		final Unique4jConfig config = Unique4jConfig.createDefault(getAppId()).ipcFactory(ipcFactory);

		final Object lock = new Object();
		final List<String> received = new CopyOnWriteArrayList<>();
		final Unique4jLock unique1 = Unique4jLock.create(
				config,
				otherInstanceClient -> {
					final DataInputStream dis = new DataInputStream(otherInstanceClient.getInputStream());
					final String receivedMsg = dis.readUTF();
					synchronized (lock) {
						// to assert on main thread
						received.add(receivedMsg);
						// notify that message has been received
						lock.notify();
					}
				},
				firstInstanceClient -> {});
		final Unique4jLock unique2 = Unique4jLock.create(
				config,
				otherInstanceClient -> {},
				firstInstanceClient -> {
					final DataOutputStream dos = new DataOutputStream(firstInstanceClient.getOutputStream());
					dos.writeUTF(message);
				});

		try {
			// try to obtain lock
			unique1.tryLock();
			unique2.tryLock();

			// wait until message is received
			synchronized (lock) {
				while (received.isEmpty()) {
					lock.wait();
				}
			}

			// assert if message is sent correctly
			assertEquals(Collections.singletonList(message), received);
		} finally {
			// try to free the locks before exiting program
			if(unique1.isHeldByCurrentAppInstance())
				unique1.unlock();
			if(unique2.isHeldByCurrentAppInstance())
				unique2.unlock();
		}
	}

	@Test
	public void testSubsequentAcquireLock() throws IOException {
		final Unique4jConfig config = Unique4jConfig.createDefault(getAppId()).ipcFactory(ipcFactory);

		Unique4jLock first = null, second = null;
		try {
			// first instance
			first = initializeUnique4j(config);

			// second instance
			second = initializeUnique4j(config);

			// release lock for last instance only
			second.unlock();
		} catch (Throwable t) {
			// If anything fails, clean the lock
			if(first != null && first.isHeldByCurrentAppInstance()) {
				try {
					first.unlock();
				} catch (IOException e0) {
					t.addSuppressed(e0);
				}
			}

			if(second != null && second.isHeldByCurrentAppInstance()) {
				try {
					second.unlock();
				} catch (IOException e0) {
					t.addSuppressed(e0);
				}
			}

			throw t;
		}
	}

	private Unique4jLock initializeUnique4j(Unique4jConfig config) throws IOException {

		final AtomicReference<Unique4jLock> uniqueRef = new AtomicReference<>();
		uniqueRef.set(Unique4jLock.create(
				config,
				otherInstanceClient -> {
					// release lock on first instance
					uniqueRef.get().unlock();
					// assert if lock has been released
					assertTrue(true);
				},
				firstInstanceClient -> {}
		));
		Unique4jLock unique = uniqueRef.get();

		// try to acquire lock
		boolean lockAcquired = unique.tryLock();

		// failed to acquire lock, first instance had lock
		// therefore, now first instance has released lock
		// try to acquire lock again
		if (!lockAcquired)
			lockAcquired = unique.tryLock();

		// assert if lock has been acquired
		assertTrue(lockAcquired);
		return unique;
	}
}
