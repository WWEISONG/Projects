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
    int firstSucc;
    int secondSucc;
    int firstPredecessor;
    int secondPredecessor;
    int portNumber;
    int countAliveFirstS;
    int countAliveSecondS;
    Boolean state;
    Boolean firstSuccState;
    Boolean secondSuccState;

    public Dht(int peerID, int firstSucc, int secondSucc) {
        this.peerID = peerID;
        this.firstSucc = firstSucc;
        this.secondSucc = secondSucc;
        this.portNumber = peerID + 20000;
        this.firstPredecessor = 300;
        this.secondPredecessor = 300;
        this.countAliveFirstS = 0;
        this.countAliveSecondS = 0;
        this.state = true;
        this.firstSuccState = false;
        this.secondSuccState = false;

    };

    /**
     * This method encapsulate the TCP communication between peers
     * IP address is localhost and port number is peerid + 20000
     * usage: request file, gracefully quit 
     */
    private void send_tcp_message(String message, int dest) throws IOException {

        Socket socket = new Socket("127.0.0.1", dest);
        OutputStream outSendForRequester = socket.getOutputStream();
        byte[] remessage = message.getBytes();
        outSendForRequester.write(remessage);
        outSendForRequester.flush();
        outSendForRequester.close();
        socket.close();
    }

    private void send_udp_message(String message, int dest) throws IOException {

        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramSocket senderSoc = new DatagramSocket();

        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, dest);

        senderSoc.send(packet);
        senderSoc.close();
    }

    private void send_file_reuest_message(int fileName) {
        int portFirstSucc = firstSucc + 20000;

        String requestMessage = "request " + fileName + " " + peerID;
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

    private void send_file(File fileToSend, int requestPeer, int fileName) throws IOException {
        char[] fileBlock = new char[400];
        InputStreamReader reader = new InputStreamReader(new FileInputStream(fileToSend));
        int charRead = 0;
        charRead = reader.read(fileBlock, 0, fileBlock.length);
        while (charRead != -1) {
            String fileBlockStr = fileName + " " + String.valueOf(fileBlock) + " " + peerID;
            send_udp_message(fileBlockStr, requestPeer);
            charRead = reader.read(fileBlock, 0, fileBlock.length);
        }
        reader.close();
    }

    private void find_file_and_send(File fileToSend, int fileName, int requestPeer)
            throws IOException {
        System.out.printf("File %d is here%n", fileName);
        System.out.printf("A response message, destined for peer %d has been sent.%n", requestPeer-20000);
        String message = fileName + " ready to send file " + peerID;
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
        int requesterPeerID = requestPeer - 20000;
        String fileNameStr = "assignment/" + String.valueOf(fileName) + ".txt";
        String fileNameStrCopy = "assignment/" + String.valueOf(fileName) + "copy.txt";
        // retrive if there is copy version or not under this peer
        File fileToSend = new File(fileNameStr);
        File fileToSendCopy = new File(fileNameStrCopy);

        if (hashFilename == peerID && fileToSend.exists()) {
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
            String message = "normal_quit " + firstSucc + " " + secondSucc + " " + peerID;
            send_tcp_message(message, firstPredecessor + 20000);
            send_tcp_message(message, secondPredecessor + 20000);
        }
    }

    private void send_query_to_succ(int succ) throws IOException {
        String message = "abnormal_leave " + peerID;
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

    private void handle_ping_response(int myPeerID) {
        System.out.println("A ping response message was received from Peer " + myPeerID);
        if (myPeerID == firstSucc) {
            firstSuccState = true;
        } else if (myPeerID == secondSucc) {
            secondSuccState = true;
        } else {
            System.out.println("message gap");
            System.out.println(myPeerID + " " + firstSucc + " " + secondSucc);
        }
    }

    /**
     * This class is used to send ping message to successors if I am live (state is
     * true) for every 20 seconds, will send one ping message. The message format I
     * defined is: "A ping request message was received from peer peerID"
     */
    public class PingSender extends Thread {
        public void run() {
            int portFirstSucc = firstSucc + 20000;
            int portSecondSucc = secondSucc + 20000;

            while (state) {
                String pingFirstS = "A ping request message p1 " + peerID;
                String pingSecondS = "A ping request message p2 " + peerID;
                try {
                    send_udp_message(pingFirstS, portFirstSucc);
                    send_udp_message(pingSecondS, portSecondSucc);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class PingAlive extends Thread {
        public void run() {
            int portFirstSucc = firstSucc + 20000;
            int portSecondSucc = secondSucc + 20000;

            while (state) {
                String pingFirstS = "A ping alive_request message p1 " + peerID;
                String pingSecondS = "A ping alive_request message p2 " + peerID;
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
     * 3. request file message
     * 4. alive_request and alive_response
     * 5. receive files and stop-wait
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

                    if (allMess.length >= 3) {
                        if (allMess.length == 3) {
                            // handle request file message
                            retrive_file(Integer.parseInt(allMess[1]), myPeerPort);
                        } else if (allMess.length == 6) {
                            // handle ping messages
                            if (allMess[2].equals("request")) {
                                // handle ping request
                                System.out.println("A ping request message was received from peer " + myPeerID);

                                if (allMess[4].equals("p1")) {
                                    firstPredecessor = myPeerID;
                                } else if (allMess[4].equals("p2")) {
                                    secondPredecessor = myPeerID;
                                }

                                byte[] response = ("A ping response message s " + peerID).getBytes();
                                DatagramPacket responsePacket = new DatagramPacket(response, response.length,
                                        myPeerAddress, myPeerPort);

                                receiveSoc.send(responsePacket);

                            } else if (allMess[2].equals("response")) {
                                // handle ping response
                                handle_ping_response(myPeerID);
                            } else if (allMess[2].equals("alive_request")) {
                                String pos = new String();
                                if (allMess[4].equals("p1")) {
                                    pos = "s1";
                                } else {
                                    pos = "s2";
                                }
                                byte[] response = ("A ping alive_response message " + pos + " " + peerID).getBytes();
                                DatagramPacket responsePacket = 
                                    new DatagramPacket(response, response.length, myPeerAddress, myPeerPort);
                                receiveSoc.send(responsePacket);
                            } else if (allMess[2].equals("alive_response")) {
                                if (allMess[4].equals("s1")) {
                                    countAliveFirstS += 1;
                                    firstSucc = myPeerID;
                                    firstSuccState = true;
                                } else if (allMess[4].equals("s2")) {
                                    countAliveSecondS += 1;
                                    secondSucc = myPeerID;
                                    secondSuccState = true;
                                }
                            } else {
                                System.out.printf("Received a response message from peer %d, which has the file %d.",
                                        myPeerID, Integer.parseInt(allMess[0]));
                                System.out.println("Start receiving the file...");
                            }
                        } else {
                            File receiveFile = new File("assignment/" + allMess[0] + "copy.txt");
                            if (!receiveFile.exists()) {
                                receiveFile.createNewFile();
                            }
                            FileOutputStream fos = new FileOutputStream(receiveFile, true);
                            fos.write(recData);
                            fos.close();
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
                    if (messageArray[0].equals("abnormal_leave")) {
                        int myPeerID = Integer.parseInt(messageArray[messageArray.length - 1]);
                        Socket myPeerSocket = new Socket("127.0.0.1", myPeerID + 20000);
                        OutputStream res = myPeerSocket.getOutputStream();
                        byte[] resmessage = 
                            ("response_for_leave " + myPeerID + " " + firstSucc).getBytes();
                        res.write(resmessage);
                        res.flush();
                        res.close();
                        myPeerSocket.close();
                    } else if (messageArray[0].equals("normal_quit")) {
                        int myPeerID = Integer.parseInt(messageArray[3]);
                        int myPeerFirstS = Integer.parseInt(messageArray[1]);
                        int myPeerSecondS = Integer.parseInt(messageArray[2]);
                        System.out.printf("Peer %d will depart from the network%n", myPeerID);
                        update_succ(myPeerID, myPeerFirstS, myPeerSecondS);
                    } else if (messageArray[0].equals("response_for_leave")) {
                        firstSucc = Integer.parseInt(messageArray[1]);
                        secondSucc = Integer.parseInt(messageArray[2]);
                    }
                }

                tcpServer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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
        int peerID = Integer.parseInt(argv[0]);
        int succssorOne = Integer.parseInt(argv[1]);
        int succssorTwo = Integer.parseInt(argv[2]);

        Dht mydht = new Dht(peerID, succssorOne, succssorTwo);

        Dht.PingSender sendThread = mydht.new PingSender();
        Dht.UDPReceiver receiverThread = mydht.new UDPReceiver();
        Dht.InputInfo inputInfoThread = mydht.new InputInfo();
        Dht.TCPReceiver tcpServerThread = mydht.new TCPReceiver();
        Dht.FailureMonitoring failureMonitoring = mydht.new FailureMonitoring();
        Dht.PingAlive pingAlive = mydht.new PingAlive();

        sendThread.start();
        receiverThread.start();
        inputInfoThread.start();
        tcpServerThread.start();
        failureMonitoring.start();
        pingAlive.start();
    } 
}