package assignment;

import java.util.ArrayList;

public class DhtTracker {

    int numberPeers;
    ArrayList<Integer> peersList = new ArrayList<Integer>(); 

    public class Receiver extends Thread {

        @Override
        public void run() {

        }
    }

    public class Sender extends Thread {

        @Override
        public void run() {

        }
    }

    public class StateChecker extends Thread {

        @Override
        public void run() {

        }
    }

    public static void main() {

        DhtTracker dhtTracker = new DhtTracker();
        DhtTracker.Receiver receiver = dhtTracker.new Receiver();
        DhtTracker.Sender sender = dhtTracker.new Sender();
        DhtTracker.StateChecker stateChecker = dhtTracker.new StateChecker();

        receiver.start();
        sender.start();
        stateChecker.start();
    }
}