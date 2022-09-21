/*
 * File: AircraftTransmissionSystem.java
 * Project: Milestone 2
 * Name: Satbir Singh
 * Date: 2021-11-26
 * Description: sends data to packets from different files to the ground terminal
 */

import java.io.File;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

//class for storing packet information
class Packet {
    private Header header;
    private Body body;
    private Trailer trailer;

    // header setter
    public void setHeader(Header header) {
        this.header = header;
    }

    // body setter
    public void setBody(Body body) {
        this.body = body;
    }

    // trailer setter
    public void setTrailer(Trailer trailer) {
        this.trailer = trailer;
    }

    // header getter
    public Header getHeader() {
        return this.header;
    }

    // body getter
    public Body getBody() {
        return this.body;
    }

    // trailer getter
    public Trailer getTrailer() {
        return this.trailer;
    }
}

// class for storing header information
class Header {
    private String tailNum;
    private int packSeqNum;

    // tailNum setter
    public void setTailNum(String tailNum) {
        this.tailNum = tailNum;
    }

    // packSeqNum setter
    public void setPackSeqNum(int packSeqNum) {
        this.packSeqNum = packSeqNum;
    }

    // tailNum getter
    public String getTailNum() {
        return this.tailNum;
    }

    // packSeqNum getter
    public int getPackSeqNum() {
        return this.packSeqNum;
    }
}

// class for storing body information
class Body {
    private String aircraftData;

    // aircraftData setter
    public void setAircraftData(String aircraftData) {
        this.aircraftData = aircraftData;
    }

    // aircraftData getter
    public String getAircraftData() {
        return this.aircraftData;
    }
}

// class for storing trailer information
class Trailer {
    private int checksum;

    // checksum setter
    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    // checksum getter
    public int getCheckSum() {
        return this.checksum;
    }
}

// class for Aircraft Transmission System
public class AircraftTransmissionSystem {
    private static final int SIZE = 121;
    private static final String FILE1 = "C-FGAX.txt";
    private static final String FILE2 = "C-GEFC.txt";
    private static final String FILE3 = "C-QWWT.txt";
    private static final String ip = "localhost";
    private static final int senderPort = 3321;
    private static final int receiverPort = 4455;

    /*
     * Function: main
     * Parameters: String args[]: arguments provided
     * Return: none
     * Description: sends data from different files to the ground terminal
     */
    public static void main(String args[]) {
        InetAddress receiverIP;
        try {
            receiverIP = InetAddress.getByName(ip);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

        // check if ground terminal is running
        if (available(receiverPort)) {
            System.out.println("Error: Ground Terminal is not active");
            return;
        }

        // array of file names
        String[] fileNames = new String[3];
        fileNames[0] = FILE1;
        fileNames[1] = FILE2;
        fileNames[2] = FILE3;

        // byte array for the packet
        byte[] bPacket = new byte[SIZE];
        DatagramSocket ds = null;
        DatagramPacket dp = null;

        Header header = new Header();
        Body body = new Body();
        Trailer trailer = new Trailer();
        Packet packet = new Packet();

        try {
            // creating scanners for different files
            Scanner[] scanners = new Scanner[3];
            scanners[0] = new Scanner(new File(fileNames[0]));
            scanners[1] = new Scanner(new File(fileNames[1]));
            scanners[2] = new Scanner(new File(fileNames[2]));

            Scanner scanner = null;
            int count = 0;

            // read each line until end of file, set packet, and send
            while (true) {
                // read 3 files
                for (int i = 0; i < 3; i++) {
                    scanner = scanners[i];
                    if (!(scanner.hasNextLine()))
                        continue;

                    // set header, body, and trailer information
                    header.setTailNum(parseTailNum(fileNames[i]));
                    header.setPackSeqNum(count++);
                    body.setAircraftData(scanner.nextLine());
                    trailer.setChecksum(calcChecksum(body));

                    packet.setBody(body);
                    packet.setHeader(header);
                    packet.setTrailer(trailer);

                    // get a string packet
                    String strPacket = packet.getHeader().getTailNum() + ", ";
                    strPacket += packet.getHeader().getPackSeqNum() + ", ";
                    strPacket += packet.getBody().getAircraftData();
                    strPacket += packet.getTrailer().getCheckSum() + ", ";

                    // convert the strPacket into a byte array
                    bPacket = strPacket.getBytes();

                    try {
                        ds = new DatagramSocket(senderPort);
                    } catch (Exception e) {
                        System.out.println(e);
                        return;
                    }
                    dp = new DatagramPacket(bPacket, bPacket.length, receiverIP, receiverPort);

                    send(bPacket, dp, ds);
                    ds.close();

                    TimeUnit.SECONDS.sleep(1);
                }

                if (!(scanners[0].hasNextLine() || scanners[1].hasNextLine() || scanners[2].hasNextLine())) {
                    break;
                }
            }

            // close the scanner
            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ds = new DatagramSocket(senderPort);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
        dp = new DatagramPacket(bPacket, bPacket.length, receiverIP, receiverPort);

        // send end of transmission packet
        String eot = "<EOT>";
        bPacket = eot.getBytes();
        send(bPacket, dp, ds);

        // close DatagramSocket object
        ds.close();
    }

    /*
     * Function: send
     * Parameters: byte[] bPacket: packet to be sent
     * DatagramPacket dp: datagram packet object to be used
     * DatagramSocket ds: datagram socket object to be used
     * Return: none
     * Description: // sends data to the gound terminal
     */
    public static void send(byte[] bPacket, DatagramPacket dp, DatagramSocket ds) {
        dp.setData(bPacket);
        dp.setLength(bPacket.length);

        try {
            ds.send(dp);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
    }

    /*
     * Function: available
     * Parameters: int port: ground terminal port number
     * Return: boolean: if the ground terminal is alive or not
     * Description: checks if the ground terminal is active - received from
     * stackoverflow
     */
    public static boolean available(int port) {
        // through exception if port is invalid
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        // initialize server and data sockets
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (Exception e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    /*
     * Function: parseTailNum
     * Parameters: fileToSend: the name of the file that has to be sent to the
     * terminal
     * Return: tailNum: the tail number retrieved from the file name
     * Description: parses tail number from file name
     */
    public static String parseTailNum(String fileToSend) {
        String tailNum = "";

        for (int i = 0; fileToSend.charAt(i) != '.'; i++) {
            tailNum += fileToSend.charAt(i);
        }

        return tailNum;
    }

    /*
     * Function: calcChecksum
     * Parameters: body: body of the packet
     * Return: iChecksum: checksum of the packet
     * Description: calculates checksum
     */
    public static int calcChecksum(Body body) {
        String[] values = body.getAircraftData().split(",");
        float fChecksum = Float.parseFloat(values[5]) + Float.parseFloat(values[6]) +
                Float.parseFloat(values[7]);
        fChecksum /= 3;

        int iChecksum = Math.round(fChecksum);
        return iChecksum;
    }
}
