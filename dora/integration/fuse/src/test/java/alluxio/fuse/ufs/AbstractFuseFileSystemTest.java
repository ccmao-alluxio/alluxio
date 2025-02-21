/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.fuse.ufs;

import static jnr.constants.platform.OpenFlags.O_WRONLY;

import alluxio.client.file.options.FileSystemOptions;
import alluxio.fuse.AlluxioFuseUtils;
import alluxio.fuse.AlluxioJniFuseFileSystem;
import alluxio.fuse.options.FuseOptions;
import alluxio.jnifuse.struct.FileStat;
import alluxio.util.io.BufferUtils;

import org.junit.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class AbstractFuseFileSystemTest extends AbstractTest {
  protected AlluxioJniFuseFileSystem mFuseFs;
  protected AlluxioFuseUtils.CloseableFuseFileInfo mFileInfo;
  protected FileStat mFileStat;

  /**
   * Runs FUSE with UFS related tests with different configuration combinations.
   *
   * @param localDataCacheEnabled     whether local data cache is enabled
   * @param localMetadataCacheEnabled whether local metadata cache is enabled
   */
  public AbstractFuseFileSystemTest(boolean localDataCacheEnabled,
      boolean localMetadataCacheEnabled) {
    super(localDataCacheEnabled, localMetadataCacheEnabled);
  }

  @Override
  public void beforeActions() {
    final FuseOptions fuseOptions = FuseOptions.create(
        mContext.getClusterConf(),
        FileSystemOptions.Builder.fromConf(mConf)
            .setUfsFileSystemOptions(mUfsOptions).build(),
        false
    );
    mFuseFs = new AlluxioJniFuseFileSystem(mContext, mFileSystem, fuseOptions);
    mFileStat = FileStat.of(ByteBuffer.allocateDirect(256));
    mFileInfo = new AlluxioFuseUtils.CloseableFuseFileInfo();
  }

  @Override
  public void afterActions() throws IOException {
    BufferUtils.cleanDirectBuffer(mFileStat.getBuffer());
    mFileInfo.close();
  }

  protected void createEmptyFile(String path) {
    mFileInfo.get().flags.set(O_WRONLY.intValue());
    Assert.assertEquals(0, mFuseFs.create(path, DEFAULT_MODE.toShort(), mFileInfo.get()));
    Assert.assertEquals(0, mFuseFs.release(path, mFileInfo.get()));
  }

  protected void createFile(String path, int size) {
    mFileInfo.get().flags.set(O_WRONLY.intValue());
    Assert.assertEquals(0, mFuseFs.create(path, DEFAULT_MODE.toShort(), mFileInfo.get()));
    ByteBuffer buffer = BufferUtils.getIncreasingByteBuffer(size);
    Assert.assertEquals(size,
        mFuseFs.write(FILE, buffer, size, 0, mFileInfo.get()));
    Assert.assertEquals(0, mFuseFs.release(path, mFileInfo.get()));
  }
}
