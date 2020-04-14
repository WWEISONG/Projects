package assignment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Dht {

    int peerID;// current peer id
    int firstSucc;// ID of first successor
    int secondSucc;// ID of second successor
    int firstPredecessor;// ID of first predecessor
    int secondPredecessor;// ID of second predecessor
    int portNumber;// current peer port number = peerID + 20000
    int countAliveFirstS;// count of alive message from first succ
    int countAliveSecondS;// count of alive message from second succ
    int interval;// request-response message interval
    int filePeerID;// store peer id who has the original file
    Boolean state;// my own state--alive/true, dead/false
    Boolean firstSuccState;// state of first successor
    Boolean secondSuccState;// state of second successor

    public Dht(int peerID, int firstSucc, int secondSucc, int interval) {
        
        this.peerID = peerID;
        this.firstSucc = firstSucc;
        this.secondSucc = secondSucc;
        this.interval = interval * 1000;
        this.portNumber = peerID + 20000;
        this.firstPredecessor = 300;
        this.secondPredecessor = 300;
        this.countAliveFirstS = 0;
        this.countAliveSecondS = 0;
        this.state = false;
        this.firstSuccState = false;
        this.secondSuccState = false;

    };

    /**
     * This method encapsulate the TCP communication between peers
     * IP address is localhost and port number is peerid + 20000
     * usage: request file, gracefully quit 
     */
    public void send_tcp_message(String message, int dest) throws IOException {

        Socket socket = new Socket("127.0.0.1", dest);
        OutputStream outSendForRequester = socket.getOutputStream();
        byte[] remessage = message.getBytes();
        outSendForRequester.write(remessage);
        outSendForRequester.flush();
        outSendForRequester.close();
        socket.close();
    }

    public void join_request(int nextPeer) throws IOException {

        String message = "message join_requst message " + peerID;
        send_tcp_message(message, nextPeer+20000);
    }

    /**
     * This method encapsulate the UDP communication between peers
     * IP address is localhost and port number is peerid + 20000
     * usage: request-response message, alive message(heartbeat), 
     * sign with tracker, file exchange with other peers
     */
    private void send_udp_message(String message, int dest) throws IOException {

        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramSocket senderSoc = new DatagramSocket();

        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, dest);

        senderSoc.send(packet);
        senderSoc.close();
    }

    /**
     * User input: request file eg. request 1537 using COMMAND LINE
     * This method is used to send file requesting message to successors
     * protocol: UDP
     */
    private void send_file_reuest_message(int fileName) {

        int portFirstSucc = firstSucc + 20000;
        String requestMessage = "message file_request " + fileName + " " + peerID;
        try {
            send_udp_message(requestMessage, portFirstSucc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("File request message for %d has been sent to my succssor%n", fileName);
    }

    private void forward_file_request(int fileName) {

        send_file_reuest_message(fileName);
    }

    private int hash_function(int fileName) {

        int hashFileName = fileName % 256;
        return hashFileName;
    }

    /**
     * send file to the requester chunk by chunk, say every time the sender only 
     * send 400 bytes to receiver and wait for ACK message from receiver then it
     * can continue to sending the left data.
     */
    private void send_file(File fileToSend, int requestPeer, int fileName) throws IOException {

        char[] fileBlock = new char[400];
        InputStreamReader reader = new InputStreamReader(new FileInputStream(fileToSend));
        int charRead = 0;
        charRead = reader.read(fileBlock, 0, fileBlock.length);
        while (charRead != -1) {
            String fileBlockStr = 
                "file_block " + fileName + " " + String.valueOf(fileBlock) + " " + peerID;
            send_tcp_message(fileBlockStr, requestPeer);
            charRead = reader.read(fileBlock, 0, fileBlock.length);
        }
        reader.close();
    }

    private void find_file_and_send(File fileToSend, int fileName, int requestPeer)
            throws IOException {

        System.out.printf("File %d is here%n", fileName);
        System.out.printf("A response message, destined for peer %d has been sent.%n", requestPeer-20000);
        String message = "message file_response " + fileName + " ready to send file " + peerID;
        send_tcp_message(message, requestPeer);
        send_file(fileToSend, requestPeer, fileName);
    }


    /**
     * Still not finish
     * Still not finish
     * Still not finish
     */
    private void retrive_file(int fileName, int requestPeer) throws IOException {

        int hashFilename = hash_function(fileName);
        String fileNameStr = "assignment/" + String.valueOf(fileName) + ".txt";
        String fileNameStrCopy = "assignment/" + String.valueOf(fileName) + "copy.txt";
        // retrive if there is copy version or not under this peer
        File fileToSend = new File(fileNameStr);
        File fileToSendCopy = new File(fileNameStrCopy);

        if (hashFilename == filePeerID && fileToSend.exists()) {
            find_file_and_send(fileToSend, fileName, requestPeer);
            return;
        }
        if (fileToSendCopy.exists()) {
            find_file_and_send(fileToSend, fileName, requestPeer);
        } else {
            System.out.printf("File %d is not stored here.%n", fileName);
            forward_file_request(fileName);
        }

    }

    private void inform_predecessors() throws IOException {

        if (firstPredecessor < 257 && secondPredecessor < 257) {
            String message = "message normal_quit " + firstSucc + " " + secondSucc + " " + peerID;
            send_tcp_message(message, firstPredecessor + 20000);
            send_tcp_message(message, secondPredecessor + 20000);
        }
    }

    private void send_query_to_succ(int succ) throws IOException {

        String message = "message abnormal_leave " + peerID;
        send_tcp_message(message, succ);
    }

    private void update_succ(int myPeerID, int myPeerFirstS, int myPeerSecondS) {

        if (myPeerID == this.firstSucc) {
            this.firstSucc = myPeerFirstS;
            this.secondSucc = myPeerSecondS;
        } else if (myPeerID == this.secondSucc) {
            this.secondSucc = myPeerFirstS;
        }

        System.out.println("My first successor is now peer " + this.firstSucc);
        System.out.println("My second successor is now peer " + this.secondSucc);
    }

    /**
     * This class is used to send ping message to successors if I am live (state is
     * true) for every 20 seconds, will send one ping message. The message format I
     * defined is: "A ping request message was received from peer peerID"
     */
    public class PingRequestSender extends Thread {

        public void run() {

            int portFirstSucc = firstSucc + 20000;
            int portSecondSucc = secondSucc + 20000;

            while (state) {
                String pingFirstS = "message ping_request message p1 " + peerID;
                String pingSecondS = "message ping_request message p2 " + peerID;
                try {
                    send_udp_message(pingFirstS, portFirstSucc);
                    send_udp_message(pingSecondS, portSecondSucc);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * heartbeat message sending thread, every 2sec will send a alive_request message
     * to my two successors to check if they are alive or not,thus, every 15sec I should
     * receive at least 3 alive_response messages from my two successors.
     * 
     * countAliveFirstS +1 for every alive_response
     * countAliveSecondS +1 for every alive_response
     */
    public class PingAlive extends Thread {

        public void run() {
            int portFirstSucc = firstSucc + 20000;
            int portSecondSucc = secondSucc + 20000;

            while (state) {
                String pingFirstS = "message alive_request message p1 " + peerID;
                String pingSecondS = "message alive_request message p2 " + peerID;
                try {
                    send_udp_message(pingFirstS, portFirstSucc);
                    send_udp_message(pingSecondS, portSecondSucc);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Read info from terminal
     * 1. request fileX
     * 2. quit
     */
    public class InputInfo extends Thread {

        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (state) {
                try {
                    String inputFromUser = reader.readLine();
                    String[] inputArray = inputFromUser.split(" ");
                    if (inputArray[0].equals("request")) {
                        int fileName = Integer.parseInt(inputArray[1]);
                        File file = new File("assignment/" + fileName + "copy.txt");
                        if (file.exists()) {
                            System.out.println("The file you wanna request is existing...");
                            file.delete();
                        }
                        send_file_reuest_message(fileName);
                    } else if (inputArray[0].equals("quit")) {
                        // handle normal departrue
                        state = false;
                        inform_predecessors();
                        System.out.println();
                        System.out.println("Good Bye ~");
                        System.out.println();
                        System.exit(0);
                    } else {
                        System.out.println("I cannot understand you");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * UDP receiver server needs to handle: 
     * 1. ping request message 
     * 2. ping response message 
     * 3. alive_request and alive_response
     */
    public class UDPReceiver extends Thread {

        public void run() {
            try {
                InetAddress myAddress = InetAddress.getByName("127.0.0.1");
                DatagramSocket receiveSoc = new DatagramSocket(portNumber, myAddress);
                byte[] recData = new byte[1024];
                DatagramPacket recPacket = new DatagramPacket(recData, recData.length, myAddress, portNumber);
                while (state) {
                    receiveSoc.receive(recPacket);
                    String message = new String(recData, 0, recPacket.getLength());
                    InetAddress myPeerAddress = recPacket.getAddress();
                    String[] allMess = message.split(" ");

                    int myPeerID = Integer.parseInt(allMess[allMess.length - 1]);
                    int myPeerPort = myPeerID + 20000;

                    if (allMess[0].equals("message")) {
                        if (allMess[1].equals("file_request")) {
                            retrive_file(Integer.parseInt(allMess[1]), myPeerPort);
                        } else if (allMess[1].equals("ping_request")) {
                            System.out.println("A ping request message was received from peer " + myPeerID);
                            if (allMess[3].equals("p1")) {
                                firstPredecessor = myPeerID;
                            } else if (allMess[3].equals("p2")) {
                                secondPredecessor = myPeerID;
                            }

                            byte[] response = ("message ping_response message s " + peerID).getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(response, response.length,
                                    myPeerAddress, myPeerPort);

                            receiveSoc.send(responsePacket);

                        } else if (allMess[1].equals("ping_response")) {
                            System.out.println("Ping response message was received from Peer " + myPeerID);
                        } else if (allMess[1].equals("alive_request")) {
                                String pos = new String();
                                if (allMess[3].equals("p1")) {
                                    pos = "s1";
                                } else {
                                    pos = "s2";
                                }
                                byte[] response = ("message alive_response message " + pos + " " + peerID).getBytes();
                                DatagramPacket responsePacket = 
                                    new DatagramPacket(response, response.length, myPeerAddress, myPeerPort);
                                receiveSoc.send(responsePacket);
                        } else if (allMess[1].equals("alive_response")) {
                            if (allMess[3].equals("s1")) {
                                countAliveFirstS += 1;
                                firstSucc = myPeerID;
                                firstSuccState = true;
                            } else if (allMess[3].equals("s2")) {
                                countAliveSecondS += 1;
                                secondSucc = myPeerID;
                                secondSuccState = true;
                            }
                        }
                    } else {
                        System.out.println("Cannot handle this format of message");
                    }
                }
                receiveSoc.close();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * TCP receiver Thread, it's functions:
     * 1. handle normal leave of peers
     * 2. handle abnormal leave of peers
     * 3. new peers join request message
     * 4. file transfer
     */
    public class TCPReceiver extends Thread {

        public void run() {

            try {
                ServerSocket tcpServer = 
                    new ServerSocket(portNumber, 2, InetAddress.getByName("127.0.0.1"));
                while (state) {
                    Socket tcpClient = tcpServer.accept();
                    DataInputStream messageFromClient = new DataInputStream(
                            new BufferedInputStream(tcpClient.getInputStream()));
                    byte[] buf = new byte[1024];
                    int length = messageFromClient.read(buf);
                    String message = new String(buf, 0, length);
                    String[] messageArray = message.split(" ");
                    if (messageArray[0].equals("message")) {
                        if (messageArray[1].equals("abnormal_leave")) {
                            int myPeerID = Integer.parseInt(messageArray[messageArray.length - 1]);
                            Socket myPeerSocket = new Socket("127.0.0.1", myPeerID + 20000);
                            OutputStream res = myPeerSocket.getOutputStream();
                            byte[] resmessage = 
                                ("message response_for_leave " + myPeerID + " " + firstSucc).getBytes();
                            res.write(resmessage);
                            res.flush();
                            res.close();
                            myPeerSocket.close();
                        } else if (messageArray[1].equals("normal_quit")) {
                            int myPeerID = Integer.parseInt(messageArray[3]);
                            int myPeerFirstS = Integer.parseInt(messageArray[1]);
                            int myPeerSecondS = Integer.parseInt(messageArray[2]);
                            System.out.printf("Peer %d will depart from the network%n", myPeerID);
                            update_succ(myPeerID, myPeerFirstS, myPeerSecondS);
                        } else if (messageArray[1].equals("response_for_leave")) {
                            firstSucc = Integer.parseInt(messageArray[1]);
                            secondSucc = Integer.parseInt(messageArray[2]);
                        } else if (messageArray[1].equals("file_response")){
                            System.out.printf("Received a response message from peer %d, which has the file %d.",
                                    myPeerID, Integer.parseInt(messageArray[0]));
                            System.out.println("Start receiving the file...");
                        } else if (messageArray[1].equals("join_request")) {
                            // handle join request
                        } else if (messageArray[1].equals("join_success")) {
                            // handle join success
                        }
                    } else if (messageArray[0].equals("file_block")) {
                        File receiveFile = new File("assignment/" + messageArrays[0] + "copy.txt");
                        if (!receiveFile.exists()) {
                            receiveFile.createNewFile();
                        }
                        FileOutputStream fos = new FileOutputStream(receiveFile, true);
                        fos.write(buf);
                        fos.close();
                    }
                }

                tcpServer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * every 15sec will check wheather my two successors are alive or not
     * This is for abnormal leaving check, it's like the heartbeat messages
     */
    public class FailureMonitoring extends Thread {

        public void run() {
            try {
                Thread.sleep(30000);
                while (state) {
                    Thread.sleep(15000);
                    if (countAliveFirstS <= 3) {
                        System.out.printf("Peer %d is no longer alive", firstSucc);
                        if (secondSuccState) {
                            send_query_to_succ(secondSucc+20000);
                        }
                    }
                    if (countAliveSecondS <= 3) {
                        System.out.printf("Peer %d is no longer alive", secondSucc);
                        if (firstSuccState) {
                            send_query_to_succ(firstSucc+20000);
                        }
                    }
                    countAliveFirstS = 0;
                    countAliveSecondS = 0;
                    firstSuccState = false;
                    secondSuccState = false;
                }
            } catch (InterruptedException | IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void main(String argv[]) {

        String operation = argv[0];
        int peerID = Integer.parseInt(argv[1]);;
        int succssorOne = 300;
        int succssorTwo = 300;
        int interval = 30;

        /** initially we don't know if this peer is "init" or join, so we init it's two 
         *  successors as FAKE peers say their ID are not EXIST (a number is bigger than
         *  256 for we totally could 256 peers), here I choose 300
        */
        Dht mydht = new Dht(peerID, succssorOne, succssorTwo, interval);
        Dht.TCPReceiver tcpServerThread = mydht.new TCPReceiver();
        tcpServerThread.start();

        if (operation.equals("init")) {
            succssorOne = Integer.parseInt(argv[2]);
            succssorTwo = Integer.parseInt(argv[3]);
            interval = Integer.parseInt(argv[4]);
        } else if (operation.equals("join")) {
            interval = Integer.parseInt(argv[3]);
            mydht.join_request(Integer.parseInt(argv[2]));
        } else {
            System.out.println("Cannot provide this kind service :)");
        }

        Dht.PingRequestSender sendThread = mydht.new PingRequestSender();
        Dht.UDPReceiver receiverThread = mydht.new UDPReceiver();
        Dht.InputInfo inputInfoThread = mydht.new InputInfo();
        Dht.FailureMonitoring failureMonitoring = mydht.new FailureMonitoring();
        Dht.PingAlive pingAlive = mydht.new PingAlive();

        if (mydht.state) {
            sendThread.start();
            receiverThread.start();
            inputInfoThread.start();
            failureMonitoring.start();
            pingAlive.start();
        }
    } 
}