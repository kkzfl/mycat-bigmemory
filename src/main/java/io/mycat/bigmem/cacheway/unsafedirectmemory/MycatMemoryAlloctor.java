package io.mycat.bigmem.cacheway.unsafedirectmemory;

import io.mycat.bigmem.buffer.DirectMemAddressInf;
import io.mycat.bigmem.buffer.MycatBuffer;
import io.mycat.bigmem.buffer.MycatMovableBufer;
import io.mycat.bigmem.buffer.impl.DirectMycatBufferImpl;
import io.mycat.bigmem.cacheway.CacheOperatorInf;

/**
 * java 内存池的实现
 * 源文件名：MemoryPool.java
 * 文件版本：1.0.0
 * 创建作者：liujun
 * 创建日期：2016年12月19日
 * 修改作者：liujun
 * 修改日期：2016年12月19日
 * 文件描述：TODO
 * 版权所有：Copyright 2016 zjhz, Inc. All Rights Reserved.
 */
public class MycatMemoryAlloctor implements CacheOperatorInf {

    /**
    * 内存池对象信息
    * @字段说明 pool
    */
    private final UnsafeDirectBufferPage[] POOL;

    /**
    * 每个chunk的大小
    * @字段说明 CHUNK_SIZE
    */
    private final int CHUNK_SIZE;

    /**
    * 用来构建内存池对象信息
    * 构造方法
    * @param chunkSize
    * @param memorySize
    * @param poolSize
    */
    public MycatMemoryAlloctor(int chunkSize, int memorySize, short poolSize) {
        CHUNK_SIZE = chunkSize;
        // 进行每个内存页的初始化
        POOL = new UnsafeDirectBufferPage[poolSize];
        // 进行每个chunk的页面的分配内存操作
        for (int i = 0; i < poolSize; i++) {
            POOL[i] = new UnsafeDirectBufferPage(new DirectMycatBufferImpl(memorySize), CHUNK_SIZE);
        }
    }

    /**
    * 进行内存分配操作
    * 方法描述
    * @param size 需要的内存大小,最好CHUNK_SIZE的倍数，以方便 内存的回收利用
    * @return
    * @创建日期 2016年12月19日
    */
    public MycatBuffer allocationMemory(int size, long timeout) {
        // 计算需要的chunk大小
        int needChunk = size % CHUNK_SIZE == 0 ? size / CHUNK_SIZE : size / CHUNK_SIZE + 1;
        // 取得内存页信息
        UnsafeDirectBufferPage page = null;
        for (UnsafeDirectBufferPage pageMemory : POOL) {
            if (pageMemory.checkNeedChunk(needChunk)) {
                page = pageMemory;
                break;
            }
        }

        // 如果能找合适的内存空间，则进行分配
        if (null != page) {
            // 针对当前的chunk进行内存的分配操作
            MycatBuffer buffer = page.alloactionMemory(needChunk, timeout);
            return buffer;
        }
        return null;

    }

    /**
    * 进行内存的归还操作
    * 方法描述
    * @param buffer
    * @创建日期 2016年12月19日
    */
    public boolean recycleAll(MycatBuffer buffer) {

        // 计算chunk归还的数量
        int chunkNum = (int) buffer.capacity() / CHUNK_SIZE;

        // 获得内存buffer
        DirectMemAddressInf thisNavBuf = (DirectMemAddressInf) buffer;
        // attachment对象在buf.slice();的时候将attachment对象设置为总的buff对象
        DirectMemAddressInf parentBuf = (DirectMemAddressInf) thisNavBuf.getAttach();
        // 已经使用的地址减去父类最开始的地址，即为所有已经使用的地址，除以chunkSize得到chunk当前开始的地址,得到整块内存开始的地址
        int startChunk = (int) (thisNavBuf.address() - parentBuf.address()) / chunkNum;

        boolean recyProc = false;

        for (UnsafeDirectBufferPage pageMemory : POOL) {
            if ((recyProc = pageMemory.recycleBuffer((MycatMovableBufer) parentBuf, startChunk, chunkNum)) == true) {
                break;
            }
        }

        if (!recyProc) {
            System.out.println("memory recycle fail");
            return false;
        }

        return true;
    }

    /**
    * 进行内存的归还操作
    * 方法描述
    * @param buffer
    * @创建日期 2016年12月19日
    */
    public boolean recycleNotUse(MycatBuffer buffer) {

        if (buffer.limit() < buffer.capacity()) {

            // 计算chunk归还的数量
            int chunkNum = (int) (buffer.capacity() - buffer.limit()) / CHUNK_SIZE;

            // 获得内存buffer
            DirectMemAddressInf thisNavBuf = (DirectMemAddressInf) buffer;
            // attachment对象在buf.slice();的时候将attachment对象设置为总的buff对象
            DirectMemAddressInf parentBuf = (DirectMemAddressInf) thisNavBuf.getAttach();

            int chunkAdd = buffer.limit() % CHUNK_SIZE == 0 ? (int) buffer.limit() / CHUNK_SIZE
                    : (int) buffer.limit() / CHUNK_SIZE + 1;
            // 已经使用的地址减去父类最开始的地址，即为所有已经使用的地址，除以chunkSize得到chunk当前开始的地址,得到整块内存开始的地址
            int startChunk = (int) ((thisNavBuf.address() - parentBuf.address()) / CHUNK_SIZE) + chunkAdd;

            boolean recyProc = false;

            for (UnsafeDirectBufferPage pageMemory : POOL) {
                if ((recyProc = pageMemory.recycleBuffer((MycatMovableBufer) parentBuf, startChunk,
                        chunkNum)) == true) {
                    break;
                }
            }

            if (!recyProc) {
                System.out.println("memory recycle fail");
                return false;
            }
            return true;
        } else {
            System.out.println("not memory recycle");
        }

        return false;
    }

}