package cn.ycl.nio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 消息发送队列，底层是将要发送的字节数据传入队列，发完为止
 */
public class FileDown implements Runnable{
    private String filePath;
    private SocketChannel socketChannel;
    private File downFile;
    public FileDown(String filePath,SocketChannel socketChannel) throws Exception {
        this.filePath = filePath;
        downFile = new File(filePath);
        if(!(downFile.exists() && downFile.isFile())){
            throw new Exception("要下载的文件不存在");
        }
        if(!socketChannel.isOpen()){
            throw new Exception("通道处于关闭状态，无法传输文件");
        }
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        /**
         * 下载流程
         * 1，向客户端发送请求下载文件的指令
         * 2，等待客户端返回下载文件的指令
         * 3，向客户端的输出流写入文件数据
         */
        InputStream fileIn = null;
        byte[] temp = new byte[1025];
        ByteBuffer byteBuffer = ByteBuffer.allocate(1025);
        int len = -1;
        try {

            fileIn = new FileInputStream(downFile);
            byteBuffer.clear();
            byteBuffer.put((downFile.getName() + "!" + downFile.length()).getBytes());
            byteBuffer.flip();
            socketChannel.write(byteBuffer);
          /*

            if((len = socketIn.read(temp)) != -1){
                if(! "ready".equals(new String(temp,0,len))){
                    System.out.println("客户端没有准备好连接，下载线程退出");
                    return;
                }
            }*/

            //向客户端输出流写入文件数据
            while ((len = fileIn.read(temp)) != -1){
                byteBuffer.clear();
                byteBuffer.put(temp,0,len);
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fileIn != null){
                try {
                    fileIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
