package cn.ycl.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * 客户端设计
 * 1，创建SocketChannel,去连接服务器
 *
 *
 *
 *
 */
public class MyClient {
    private String remoteIp;//远程ip
    private int remotePort;//远程端口
    private SocketChannel socketChannel;
    private Selector selector;
    private ByteBuffer readBuffer;//读缓冲器
    private ByteBuffer writeBuffer;//写缓冲区

    public MyClient(String remoteIp,int remotePort) throws IOException {
        readBuffer = ByteBuffer.allocate(1024);//1k的缓冲区，容量有限，当读取大于1k的数据就读取不到
        writeBuffer = ByteBuffer.allocate(1024);
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        //开启多路复用轮询器
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);//注册连接成功事件
        try {
            socketChannel.connect(new InetSocketAddress(remoteIp,remotePort));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("连接服务器失败");
        }
        System.out.println("客户端启动成功");
        listen();//开始监听
    }

    private void listen() throws IOException {
        while (true){
            selector.select(1000);//选择一组
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectionKeys.iterator();
            if(it.hasNext()){
                SelectionKey key = it.next();
                it.remove();
                handle(key);
            }
        }
    }

    private void handle(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        if(key.isConnectable()){
            System.out.println("服务器同意了你的连接");
            // 判断此通道上是否正在进行连接操作。
            // 完成套接字通道的连接过程。
            if (client.isConnectionPending()) {
                client.finishConnect();
                System.out.println("完成连接!");
                writeBuffer.clear();
                writeBuffer.put("Hello,Server".getBytes());
                writeBuffer.flip();
                client.write(writeBuffer);
            }
            socketChannel.register(selector,SelectionKey.OP_READ);//注册读事件
        }else if(key.isReadable()){
            //有数据来了
            readBuffer.clear();
            int count = socketChannel.read(readBuffer);
            String msg = new String(readBuffer.array(),0,count,"utf-8");
            System.out.println("来自服务器的消息：" +  msg);
            //注册写事件
            socketChannel.register(selector,SelectionKey.OP_WRITE);
        }else if(key.isWritable()){
            System.out.println("请您输入发送到服务器的指令:");
            Scanner scanner = new Scanner(System.in);
            String code = scanner.next();
            writeBuffer.clear();
            writeBuffer.put(code.getBytes("utf-8"));
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
            if("bye".equals(code)){
                System.exit(0);
            }
            //注册读事件
            socketChannel.register(selector,SelectionKey.OP_READ);
        }
    }

    public static void main(String[] args) throws Exception {
        new MyClient("127.0.0.1",8080);
    }
}
