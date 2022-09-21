/*
 * File: JavaConnect2SQL.java
 * Project: Milestone 2
 * Name: Satbir Singh
 * Date: 2021-11-26
 * Description: (Ground Terminal): receives packets from the Aircraft Transmission System and displays them to the gui and also sends them to the database
*/

package net.codejava.sql;

import java.net.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.awt.*;
import java.awt.event.*;

// class for storing packet information
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

// class for the gound terminal
public class JavaConnect2SQL {

  /*
   * Function: main
   * Parameters: String args[]: arguments provided
   * Return: none
   * Description: receives data and displays to gui
   */
  public static void main(String args[]) throws Exception {
    Gui gui = new Gui();
    gui.setVisible(true);

    try {
      gui.recv();
    } catch (Exception e) {
      System.out.println(e);
      return;
    }
  }

  // class for the gui
  public static class Gui extends JFrame {
    private JButton btnMode = new JButton("Search Database");
    private JTextField txtMode = new JTextField();
    private JTextArea txtText = new JTextArea();
    private JScrollPane sp = new JScrollPane(txtText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private DefaultCaret caret = (DefaultCaret) txtText.getCaret();

    private static String[] options = { "-- Select Option --", "C-FGAX", "C-GEFC", "C-QWWT" };
    private JComboBox comboBox = new JComboBox(options);

    private static String mode = "Enabled";
    private final int SIZE = 121;
    private DatagramSocket ds = null;
    private String outFile = "received.txt";
    private byte[] bPacket = new byte[SIZE];
    private DatagramPacket dp = new DatagramPacket(bPacket, bPacket.length);

    /* class used for a thread which prints info to the screen and to a file */
    class PacketPrinter extends Gui implements Runnable {
      /*
       * Function: run
       * Parameters: none
       * Return: none
       * Description: prints packets to the screen and to a file
       */
      @Override
      public void run() {
        String strPacket = new String(bPacket);

        // return if end of transmission msg is recv'd
        if (isEot(strPacket)) {
          return;
        }

        Header header = new Header();
        Body body = new Body();
        Trailer trailer = new Trailer();
        Packet packet = new Packet();

        // set header, body, and trailer information
        header.setTailNum(parseTailNum(strPacket));
        header.setPackSeqNum(Integer.parseInt(parsePackSeqNum(strPacket)));
        body.setAircraftData(parseAircraftData(strPacket));
        trailer.setChecksum(Integer.parseInt(parseCheckSum(strPacket)));

        packet.setBody(body);
        packet.setHeader(header);
        packet.setTrailer(trailer);

        // drop packet if checksum is incorrect
        if (packet.getTrailer().getCheckSum() != calcChecksum(body)) {
          return;
        }

        // store info in database;
        try {
          // adding a space after the first comma in the aircraft Data
          String formattedAircraftData = "";
          String line = packet.getBody().getAircraftData();
          String val1 = line.substring(0, line.indexOf(","));
          val1 += ", ";
          String val2 = line.substring(line.indexOf(",") + 1);
          formattedAircraftData = val1 + val2;

          String[] aircraftDataArr = formattedAircraftData.split(", ");
          updateDatabase(packet.getHeader().getTailNum(), aircraftDataArr[1], aircraftDataArr[2], aircraftDataArr[3],
              aircraftDataArr[4], aircraftDataArr[5], aircraftDataArr[6], aircraftDataArr[7]);

        } catch (Exception e) {
          e.printStackTrace();
        }

        // print to screen if live mode is enabled
        if (mode == "Enabled") {
          txtText.setText(txtText.getText() + "\r\n" + packet.getBody().getAircraftData());
        }
      }
    }

    /*
     * Function: Gui
     * Parameters: none
     * Return: none
     * Description: constructor for the Gui class
     */
    public Gui() {
      setTitle("Ground Terminal");
      setSize(720, 480);
      setLocation(new Point(100, 100));
      setLayout(null);
      setResizable(false);

      initComponent();
      initEvent();
    }

    /*
     * Function: initComponent
     * Parameters: none
     * Return: none
     * Description: initiates components
     */
    private void initComponent() {
      btnMode.setBounds(20, 400, 140, 20);
      txtMode.setBounds(180, 400, 160, 20);
      sp.setBounds(10, 10, 680, 360);
      comboBox.setBounds(528, 400, 160, 20);

      this.add(btnMode);
      this.add(txtMode);
      this.add(sp);
      this.add(comboBox);
      comboBox.setVisible(false);

      txtMode.setEditable(false);
      txtMode.setText("Real Time Mode: Enabled");

      txtText.setLineWrap(true);
      txtText.setEditable(false);

      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    /*
     * Function: initEvent
     * Parameters: none
     * Return: none
     * Description: adds window and action listeners
     */
    private void initEvent() {
      this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(1);
        }
      });

      btnMode.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          btnModeClick(e);
        }
      });

      comboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (e.getSource() == comboBox) {
            optionSelected(e);
          }
        }
      });
    }

    /*
     * Function: btnModeClick
     * Parameters: ActionEvent evt
     * Return: none
     * Description: enables/disables real-time mode
     */
    private void btnModeClick(ActionEvent evt) {
      if (mode.equals("Enabled")) {
        mode = "Disabled";
        txtText.setText("");
        btnMode.setText("Real Time");
        btnMode.setSize(100, 20);
        comboBox.setVisible(true);

        printDatabase("");
      } else {
        mode = "Enabled";
        txtText.setText("");
        btnMode.setText("Search Database");
        btnMode.setSize(140, 20);
        comboBox.setVisible(false);
      }

      txtMode.setText("Real Time Mode: " + mode);
    }

    /*
     * Function: printDatabase
     * Parameters: tailNum: aircraft tail number
     * Return: none
     * Description: prints database to the gui and a log file
     */
    public void printDatabase(String tailNum) {
      Connection conn = null;
      try {
        // String url =
        // "com.microsoft.sqlsever.jdbc:sqlserver://localhost/sqlexpress:1433;databaseName=ASQ_TERM_PROJECT;instance=SQLEXPRESS01";
        String url = "jdbc:sqlserver://localhost\\SQLEXPRESS01;databaseName=ASQ_TERM_PROJECT;integratedSecurity=true;";

        String user = "groundTerminal";
        String pass = "ASQ2021";
        conn = DriverManager.getConnection(url, user, pass);

        if (conn != null) {
          // create a Statement from the connection
          Statement statement = conn.createStatement();

          // get data from data base
          ResultSet rs;
          if (tailNum.equals("")) {
            rs = statement.executeQuery("SELECT * FROM FullTelemetry");
          } else {
            rs = statement.executeQuery(
                "Select * from FullTelemetry where AircraftTailNumber = '" + tailNum + "' Order by TimeStamp ASC");
          }

          ResultSetMetaData rsmd = rs.getMetaData();

          int columnsCount = rsmd.getColumnCount();

          txtText.setText("");
          while (rs.next()) {
            for (int i = 1; i <= columnsCount; i++) {
              // print aircraft data to file print aircraft data to gui
              txtText.append((rs.getString(i) + " "));

              // print aircraft data to file
              try {
                printToFile(rs.getString(i) + " ", outFile);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            txtText.append("\r\n");

            try {
              printToFile("\r\n", outFile);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          conn.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    /*
     * Function: optionSelected
     * Parameters: ActionEvent e
     * Return: none
     * Description: Event handler for the database search dropdown menu
     */
    public void optionSelected(ActionEvent e) {
      int option = comboBox.getSelectedIndex();

      switch (option) {
        case 0:
          // display entire content in database
          printDatabase("");
          break;

        // display content of specific tail numbers
        case 1:
          printDatabase("C-FGAX");
          break;
        case 2:
          printDatabase("C-GEFC");
          break;
        case 3:
          printDatabase("C-QWWT");
          break;
      }
    }

    /*
     * Function: updateDatabase
     * Parameters: String tailNum, String x, String y, String z, String weight,
     * String altitude, String pitch, String bank
     * Return: none
     * Description: sends data to the database via a sql procedure
     */
    public static void updateDatabase(String tailNum, String x, String y, String z, String weight, String altitude,
        String pitch, String bank) {
      CallableStatement cstmt = null;
      ResultSet rs = null;

      // initialize the connection url
      String url = "jdbc:sqlserver://localhost\\SQLEXPRESS01;databaseName=ASQ_TERM_PROJECT;integratedSecurity=true;";
      String user = "groundTerminal";
      String pass = "ASQ2021";
      Connection connection = null;

      // try to make a connection and send values
      try {
        connection = DriverManager.getConnection(url, user, pass);
        cstmt = connection.prepareCall(
            "{call dbo.InsertTelemetry(?, ?, ?, ?, ?, ?, ?, ?)}",
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);

        cstmt.setString("AircraftTailNumber", tailNum);
        cstmt.setString("AccelX", x);
        cstmt.setString("AccelY", y);
        cstmt.setString("AccelZ", z);
        cstmt.setString("Weight", weight);
        cstmt.setString("Altitude", altitude);
        cstmt.setString("Pitch", pitch);
        cstmt.setString("Bank", bank);

        cstmt.execute();

      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        if (rs != null) {
          try {
            // close the ResultSet object
            rs.close();
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
        if (cstmt != null) {
          try {
            // close the CallableStatement object
            cstmt.close();
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
      }
    }

    /*
     * Function: formatTimeStamp
     * Parameters: timeStamp: date and time value of the telemetry
     * Return: newStamp: the formated time stamp
     * Description: formats time stamp by replacing '_' with '-'
     */
    public static String formatTimeStamp(String timeStamp) {
      String newStamp = "";
      for (int i = 0; i < timeStamp.length(); i++) {
        if (timeStamp.charAt(i) == '_') {
          newStamp += '-';
        } else {
          newStamp += timeStamp.charAt(i);
        }
      }

      return newStamp;
    }

    /*
     * Function: isEot
     * Parameters: strPacket: the packet to check
     * Return: boolean isEOT: if the packet is an end of transmission packet
     * Description: checks if packet is the end of transmission packet
     */
    public boolean isEot(String strPacket) {
      String eot = "<EOT>";
      boolean isEOT = false;

      if (strPacket.substring(0, 5).equals(eot)) {
        isEOT = true;
      }

      return isEOT;
    }

    /*
     * Function: recv
     * Parameters: none
     * Return: none
     * Description: receives data from aircraft transmission system and displays it
     * to gui, and saves it to a text file
     */
    public void recv() throws Exception {
      int port = 4455;

      while (true) {
        // make a new Datagram socket to receive a packet
        ds = new DatagramSocket(port);
        ds.receive(dp);

        // start thread
        Runnable r = new PacketPrinter();
        Thread task = new Thread(r);
        task.start();
        task.join();

        // close DatagramSocket object
        ds.close();
      }
    }

    /*
     * Function: parseTailNum
     * Parameters: str: the telemetry received
     * Return: values[0]: tail number
     * Description: parses tail num from recv'd data
     */
    public String parseTailNum(String str) {
      String[] values = str.split(", ");
      return values[0];
    }

    /*
     * Function: parsePackSeqNum
     * Parameters: str: the telemetry received
     * Return: values[1]: packet sequence number
     * Description: parses sequence num from recv'd data
     */
    public String parsePackSeqNum(String str) {
      String[] values = str.split(", ");
      return values[1];
    }

    /*
     * Function: parseAircraftData
     * Parameters: str: the telemetry received
     * Return: ret: aircraft data
     * Description: parses aircraft data from recv'd data
     */
    public String parseAircraftData(String str) {
      String[] values = str.split(", ");
      String ret = "";

      for (int i = 2; i < 9; i++) {
        ret += values[i] + ", ";
      }

      return ret;
    }

    /*
     * Function: parseCheckSum
     * Parameters: str: the telemetry received
     * Return: ret: checksum
     * Description: parses checksum from recv'd data
     */
    public String parseCheckSum(String str) {
      String[] values = str.split(",");
      String ret = values[10].substring(1);

      return ret;
    }

    /*
     * Function: calcChecksum
     * Parameters: body: packet body
     * Return: iChecksum: the calculated checksum
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

    /*
     * Function: printToFile
     * Parameters: strPacket: packet number
     * fileName: name of the file
     * Return: none
     * Description: prints packet to a file
     */
    public void printToFile(String strPacket, String fileName) {
      try {
        FileWriter fw = new FileWriter(fileName, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);
        out.print(strPacket);

        out.flush();
        out.close();
      } catch (Exception e) {
        System.out.println(e);
        return;
      }
    }
  }
}
