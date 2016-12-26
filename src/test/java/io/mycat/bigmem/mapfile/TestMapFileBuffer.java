package io.mycat.bigmem.mapfile;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.mycat.bigmem.buffer.MyCatCallbackInf;
import io.mycat.bigmem.buffer.MycatBufferBase;
import io.mycat.bigmem.buffer.MycatSwapBufer;
import io.mycat.bigmem.cacheway.alloctor.MycatMemoryAlloctor;
import io.mycat.bigmem.console.LocatePolicy;

public class TestMapFileBuffer {

    public static void main(String[] args) throws IOException {

        try {
            // 获取文件映射的内存操作
            MycatBufferBase memorybuffer = MycatBufferBase.getMyCatBuffer(LocatePolicy.Normal, 1024);

            // 管理池对象信息
            MycatMemoryAlloctor poolBuffer = new MycatMemoryAlloctor(memorybuffer, 128, (short) 1);

            // 进行内存的申请
            MycatBufferBase mybuffer = poolBuffer.allocationMemory(1024, System.currentTimeMillis());

            mybuffer.beginOp();

            mybuffer.putByte((byte) 10);
            mybuffer.putByte((byte) 12);
            mybuffer.putByte((byte) 120);
            mybuffer.putByte((byte) 100);
            mybuffer.putByte((byte) 90);

            for (int i = 0; i < mybuffer.limit(); i++) {
                System.out.println(mybuffer.get());
            }

            System.out.println("当前写入的游标：" + mybuffer.putPosition());
            System.out.println("当前读取的游标：" + mybuffer.getPosition());

            ByteBuffer bufferValue = ByteBuffer.allocateDirect(1024);

            mybuffer.copyTo(bufferValue);

            System.out.println(bufferValue);

            for (int i = 0; i < bufferValue.position(); i++) {
                System.out.println(bufferValue.get(i));
            }
            //
            // mybuffer.recycleUnuse();

            MycatSwapBufer mybufferSwap = (MycatSwapBufer) mybuffer;

            // 测试swapin以及swapOut
            // 数据写入交换到磁盘
            mybufferSwap.swapOut();

            // 加载到内存
            mybufferSwap.swapln();

            System.out.println("交换后的结果:加载");

            for (int i = 0; i < mybuffer.limit(); i++) {
                System.out.println(mybuffer.get());
            }

            // 进行异步的通知
            mybufferSwap.swapOut(new MyCatCallbackInf() {
                @Override
                public void callBack() throws Exception {
                    System.out.println("当前异步交换到磁盘");
                }
            });

            mybufferSwap.swapIn(new MyCatCallbackInf() {

                @Override
                public void callBack() throws Exception {
                    System.out.println("异步交换到内存中");
                }
            });

            System.out.println("交换后的结果:加载");

            for (int i = 0; i < mybuffer.limit(); i++) {
                System.out.println(mybuffer.get());
            }

            mybuffer.commitOp();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
