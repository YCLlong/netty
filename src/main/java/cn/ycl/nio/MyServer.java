package cn.ycl.nio;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO服务器创建思路
 * 1，创建服务器ServerSocketChannel
 * 2，绑定端口，设置为非阻塞
 * 3，将channel通道注册到多路复用Selector
 * 4，开始轮询SelectKey（如果完成了某个事件就会被轮询到）
 * 5，对于轮询到的key，进行处理
 */
public class MyServer {
    private int port;//端口号
    private ServerSocketChannel serverSocketChannel;//服务器channel通道
    private ByteBuffer readBuffer;//读缓冲器
    private ByteBuffer writeBuffer;//写缓冲区
    private Selector selector;//轮询器
    public MyServer(int port) throws IOException {
        this.port = port;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        selector = Selector.open();
        serverSocketChannel.register(selector,SelectionKey.OP_ACCEPT);//注册请求连接时间
        readBuffer = ByteBuffer.allocate(1024);//1k的缓冲区，容量有限，当读取大于1k的数据就读取不到
        writeBuffer = ByteBuffer.allocate(1024);
        System.out.println("服务器开启成功,监听端口：" + port );
        listen();
    }


    public void listen() throws IOException {
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
        ServerSocketChannel server;
        SocketChannel client;
        if(key.isAcceptable()){
            server = (ServerSocketChannel) key.channel();
            client = server.accept();
            client.configureBlocking(false);
            System.out.println("连接成功。" + "ip地址：" + client.socket().getInetAddress().getHostAddress() + "端口号：" + client.socket().getPort());
            //发送握手消息
            writeBuffer.clear();
            //TODO 罪魁祸首在这儿。。。wrap 方法会返回当前字节数的长度的byte缓冲区
           // writeBuffer = ByteBuffer.wrap("success".getBytes("utf-8"));
            writeBuffer.put("success".getBytes("utf-8"));
            writeBuffer.flip();
            client.write(writeBuffer);
            client.register(selector,SelectionKey.OP_READ);//注册读事件
        }else if(key.isReadable()){
            //有数据来了
            client = (SocketChannel) key.channel();
            readBuffer.clear();
            int count =  client.read(readBuffer);
            String code = new String(readBuffer.array(),0,count,"utf-8");
            System.out.println("来自ip" + client.socket().getInetAddress().toString() + ":" + client.socket().getPort() + "的数据：" +  code);
            //根据读取的指令返回客户端对应的信息
            if("now".equals(code)){
                //向客户端返回当前服务器时间
                writeBuffer.clear();
                writeBuffer.put(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()).getBytes("utf-8"));
                writeBuffer.flip();
                client.write(writeBuffer);
            }else if("version".equals(code)){
                writeBuffer.clear();
                writeBuffer.put("v1.0  by ycl".getBytes());
                writeBuffer.flip();
                client.write(writeBuffer);
            }else if("bye".equals(code)){
                System.out.println("断开连接，ip" + client.socket().getInetAddress().toString() + ":" + client.socket().getPort());
                client.close();
            }else if(code.startsWith("down")){
                String[] s = code.split("-");
                if(s != null && s.length == 2){
                    try {
                        new Thread(new FileDown(s[1],client)).start();
                    } catch (Exception e) {
                        writeBuffer.clear();
                        writeBuffer.put(e.toString().getBytes("UTF-8"));
                        writeBuffer.flip();
                        client.write(writeBuffer);
                    }
                }else {
                    writeBuffer.clear();
                    writeBuffer.put("请检查下载文件的指令".getBytes("UTF-8"));
                    writeBuffer.flip();
                    client.write(writeBuffer);
                }
            }else {
                writeBuffer.clear();
                writeBuffer.put("I'm sorry".getBytes("UTF-8"));
                writeBuffer.flip();
                client.write(writeBuffer);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new MyServer(8080);
    }
}
