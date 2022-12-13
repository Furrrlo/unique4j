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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static in.pratanumandal.unique4j.Unique4jTest.getAppId;

public class DynamicPortSocketIpcFactoryTest {

    @Test
    public void testCorruptedPortFile() throws IOException {
        final Unique4jConfig config = Unique4jConfig.createDefault(getAppId());

        final File file = new File(config.lockFolder(), config.appId() + ".port");
        // if lock file exists, delete it
        if (file.exists())
            FileUtils.forceDelete(file);

        // create a corrupted lock file
        FileUtils.writeStringToFile(file, "abcdefghi\njklmnop\n\rqrst", Charset.forName("UTF-8"));

        // create instance of Unique
        Unique4jLock unique4j = Unique4j.withConfig(config).newLock(
                otherInstanceClient -> {},
                firstInstanceClient -> {});

        // try to obtain lock
        unique4j.tryLock();
        // try to free the lock before exiting program
        unique4j.unlock();
    }
}
