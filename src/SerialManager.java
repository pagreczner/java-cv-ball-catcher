//Peter Greczner

import javax.comm.*;
import java.io.*;
import java.util.*;

public class SerialManager {
    
    public String wantedPort;
    public SerialPort port;
    public PrintStream os;
    public CommPortIdentifier portID;
    public int baud, databits, parity, flow, stopbits;
    public char nextChar;
    public boolean bufferEmpty = true;
    
    public SerialManager(String wp)
    {
        wantedPort = wp;
        baud = 230400;
        parity = SerialPort.PARITY_NONE;
        databits = SerialPort.DATABITS_8;
        stopbits = SerialPort.STOPBITS_1;
    }
    
    public boolean findPort()
    {
        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        portID = null;
        while(portIdentifiers.hasMoreElements())
        {
            CommPortIdentifier pid = (CommPortIdentifier)portIdentifiers.nextElement();
            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL && pid.getName().equals(wantedPort))
            {
                portID = pid;
                break;
            }
        }
        if(portID == null)
        {
            System.out.println("Could not find serial port " + wantedPort);
            return false;
        }
        return true;
    }
    
    public void setParams(int b, int d, int s, int p)
    {
        baud = b;
        databits = d;
        stopbits = s;
        parity = p;
    }
    
    public boolean openPort()
    {
        port = null;
        try{
            port = (SerialPort)portID.open("WebFilter", 10000);
            
            port.setSerialPortParams(baud, databits, stopbits, parity);
            port.addEventListener(new SerialListener());
            port.notifyOnOutputEmpty(true);
            //port.notifyOnDataAvailable(true);
            bufferEmpty = true;
            os = new PrintStream(port.getOutputStream(), true);
            
            return true;
            
        }catch(Exception exc)
        {
            exc.printStackTrace();
            return false;
        }
    }
    
    public void closePort()
    {
        try{
            os.close();
            port.close();
            
            
        }catch(Exception exc)
        {
            exc.printStackTrace();
        }
    }
    
    public void writeChar(char c)
    {
        os.print(c);
        //nextChar = c;
    }
    public void writeByte(byte b)
    {
        os.print(b);
    }
    public void write(byte b)
    {
        os.write(b);
    }
    public void writeInt(int i)
    {
        os.print(i);
    }
    
    public class SerialListener implements SerialPortEventListener {
      
        public void serialEvent(SerialPortEvent event)
        {
            switch(event.getEventType()) {
                case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                    outputBufferEmpty(event);
                    break;
                case SerialPortEvent.DATA_AVAILABLE:
                    try{
                        InputStream is = port.getInputStream();
                        int c;
                         while((c = is.read()) != -1) {
                               System.out.print(""+(char)c);
                         }
                        System.out.print("\n");
                    }catch(Exception exc)
                    {

                    }
                break;
            }
        }

        protected void outputBufferEmpty(SerialPortEvent event) {
            //os.print(nextChar);
            bufferEmpty = true;
            //System.out.println("outputbufferempty");
            // Implement writing more data here
        }

    }
    

}
