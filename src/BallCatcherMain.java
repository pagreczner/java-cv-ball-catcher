//Peter Greczner - December 2008


import javax.media.Player;
import javax.media.CaptureDeviceInfo;
import javax.media.MediaLocator;
import javax.media.CaptureDeviceManager;
import javax.media.Manager;
import javax.media.format.*;
import java.util.Enumeration;
import javax.media.control.*;
import javax.media.Buffer;
import javax.media.util.*;

import java.awt.image.*;
import java.util.Vector;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BallCatcherMain {

    CaptureDeviceInfo device;
    MediaLocator ml;
    Player player;
    Component videoScreen;
    FrameGrabbingControl fgc;
    JPanel pPanel;

    public int currFPS = 0, lastFPS = 0;
    public int closePort = 0;
    public int cameraPosition = 75;
    public int maxCameraPosition = 145;
    public boolean trackBall = false;
    
    public boolean isCapturingVideo = false;
    public boolean isSerialConnected = false;
    public JButton startSerialButton;
    public int serialMoveMethod = 0;
    public static final int RADIUS = 0, XSPOT = 1;
    
    public static void main(String args[]) {
        getList();
        
        new BallCatcherMain();
    }

    BallCatcherMain() {
        
        try { 
           
        Frame window = new Frame();
        window.setBounds(10,10,800,600);
        window.setTitle("Ball Catcher Application - Peter Greczner & Matt Rosoff");
        window.setLocation(0,0);
        window.setLayout(new BorderLayout());
        JPanel center = new JPanel();
        center.setLayout(new GridLayout(1,1));
        pPanel = new FilterFrame();
        center.add(pPanel);
        window.add(center, BorderLayout.CENTER);
        
        JPanel west = new JPanel();
        west.setLayout(new GridLayout(2,1));
        //west.setSize(100, 600);
        
        JPanel videoWest = new JPanel();
        JButton captureVideoButton = new JButton("Capture Video");
        captureVideoButton.addActionListener(new ActionListener(){ 
            public void actionPerformed(ActionEvent evt) {
                try{
                    isCapturingVideo = true;
                    //gets a list of devices how support the given videoformat
                    Vector deviceList = CaptureDeviceManager.getDeviceList(new YUVFormat());


                    device = (CaptureDeviceInfo) deviceList.firstElement();

                    System.out.println("Device: " + device);
                    ml = device.getLocator();

                    player = Manager.createRealizedPlayer(ml);


                    player.start();
                    fgc = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");

                    bti = new BufferToImage((VideoFormat)(fgc.grabFrame()).getFormat());
                    
                    javax.swing.Timer tu = new javax.swing.Timer(30, new Updater());
                    tu.start();
                    
                    javax.swing.Timer fpsT = new javax.swing.Timer(1000, new FPS_Listener());
                    fpsT.start();
                    
                }catch(Exception exc)
                {
                    exc.printStackTrace();
                }
        }});
        videoWest.add(captureVideoButton);
        
        startSerialButton = new JButton("Start Serial");
        startSerialButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt)
            {
                if(isSerialConnected == true)
                {
                    sm.closePort();
                    isSerialConnected = false;
                    
                }
                else
                {
                    sm = new SerialManager("COM4");
                    boolean fp = sm.findPort();
                    if(fp)
                    {
                        boolean op = sm.openPort();
                        isSerialConnected = op;
                        sm.write((byte)cameraPosition);
                        //JOptionPane.showMessageDialog(null, op);
                    }
                    else
                    {
                        javax.swing.JOptionPane.showMessageDialog(null, "Could not find serial port");
                        System.exit(1);
                    }
                }
                
                startSerialButton.setText(isSerialConnected == true ? "Close Serial Connection" : "Open Serial Connection");
                
            }
        });
        west.add(videoWest);
        JPanel serialWest = new JPanel();
        serialWest.add(startSerialButton);
        west.add(serialWest);
        window.add(west, BorderLayout.WEST);
        
        JButton test = new JButton("Track Ball");
        test.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt)
            {
                /*
                int i = Integer.parseInt(JOptionPane.showInputDialog("What value to test?"));
                byte toSend = (byte)i;
                
                System.out.println(i+" " + toSend + (char)toSend);
                //sm.writeByte(toSend);
                sm.write(toSend);
                //sm.writeChar((char)toSend);
                 */
                trackBall = !trackBall;
            }
        });
        
        
        window.add(test, BorderLayout.NORTH);
        
        JPanel east = new JPanel();
        east.setLayout(new GridLayout(5,1));
        
        javax.swing.JCheckBox cbRadiusDiff = new JCheckBox("Radius Method");
        cbRadiusDiff.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt)
            {
                
                serialMoveMethod = serialMoveMethod == RADIUS ? -1 : RADIUS;
            }
        });
        east.add(cbRadiusDiff);
        javax.swing.JCheckBox cbXSpot = new JCheckBox("XSpot Method");
        cbXSpot.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt)
            {
                
                serialMoveMethod = serialMoveMethod == XSPOT ? -1 : XSPOT;
            }
        });
        east.add(cbXSpot);
        
        //window.add(east, BorderLayout.EAST);
        
        window.setVisible(true);
          
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
    public class Updater implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            pPanel.repaint();
        }
    }
    public Buffer buf;
    public Image img;
    public BufferToImage bti;
    public Font matrix = new Font("Times New Roman", Font.PLAIN, 12);
    public int[][] lastMap;
    
    public int[][] sobelX = {{3,10,3},{0,0,0},{-3,-10,-3}};
    public int[][] sobelY = {{3,0,-3},{10,0,-10},{3,0,-3}};
    public int[][] roiMap, refinedROImap;
    public int[][][] sobelMap;
    
    public int roiBasis;
    public double roiDeviation = .20;
    public Polygon p;
    public Polygon roiPoly = new Polygon();
    public int loop = 0;
    
    public int[][][] outputImage = new int[50][][];
    public HoughFilter hf = new HoughFilter(null, 15, 25, null);
    
    public int lastRadius;
    
    public int preferredRegion;
    public int lastCenterX, lastCenterY, lastDrawYBasis;
    public int radBegin, radEnd, radIncrement;
    public int consecutiveNoFinds;
    
    public int guessMinX, guessMaxX, guessMinY, guessMaxY;
    
    public static final int TOP = 0, BOTTOM = 1, CENTER = 2, ALL = 3, COMPRESS = 4;
    public static final String[] regionList = {"TOP", "BOTTOM", "CENTER", "ALL", "COMPRESS"};
    public CED_fast canny;
    public BufferedImage cannyImage;
    
    public SerialManager sm;
    
    public class FilterFrame extends JPanel
    {
        public FilterFrame()
        {
            super();
            
            preferredRegion = BOTTOM;
            lastCenterX = -1;
            lastCenterY = -1;
            lastRadius = -1;
            lastDrawYBasis = 0;
            consecutiveNoFinds = 0;
            canny = new CED_fast();
            
        }
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            
            if(!isCapturingVideo) 
            {
                g.setColor(Color.black);
                g.drawString("Click Capture Video to begin...", 10,20);
                return;
            }
            
             buf = fgc.grabFrame();
             img = (new BufferToImage((VideoFormat) buf.getFormat())
		.createImage(buf));
             
            g.drawImage(img, 0, 0, null);
            
            
             int w = img.getWidth(null);
             int h = img.getHeight(null);
             int avgcol = 0;
             int cts = 0;
             
             //System.out.println(w+","+h);
             int[] pixels = new int[img.getWidth(null) * img.getHeight(null)];
             int[] pixels_all = new int[img.getWidth(null) * img.getHeight(null)];
             int[] pixels_region2 = new int[img.getWidth(null) * img.getHeight(null)];
             int[] pixels_region3 = new int[img.getWidth(null) * img.getHeight(null)];
             int[] pixels_compress = new int[img.getWidth(null) * img.getHeight(null)];
             int[][] map = new int[h/2][w/2];
             roiMap = new int[h/2][w/2];
             refinedROImap = new int[h/2][w/2];
             int[][] hitMap = new int[h/2][w/2];
             PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
             int reg3index = 0;
             int reg2index = 0;
             int reg1index = 0;
             int reg1index_compress = 0;
             
             
             
        try{
            
            pg.grabPixels();
            
            
            int increment = 1;
            for(int j = 0; j<h; j +=increment)
            {
                for(int i = 0; i <w; i +=increment)
                {
                    //int alpha = (pixels[i] >> 24) & 0xff;
                    float red   = (pixels[i+j*w] >> 16) & 0xff;
                    float green = (pixels[i+j*w] >>  8) & 0xff;
                    float blue  = (pixels[i+j*w]      ) & 0xff;
                    
                    
                    //g.setColor(new Color((int)red, (int)green, (int)blue));
                    //g.fillRect(i, j, 2, 2);
                    
                   
                    int gray = (int)(red*.3 + green*.59 + blue*.11);
                    //avgcol += gray;
                    //cts ++;
                 
                    map[j/2][i/2] = gray;
                    hitMap[j/2][i/2] = 0;
                    
                    if( (j%2) == 0 && (i%2) == 0)
                    {
                        pixels_compress[reg1index_compress] = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);  
                        reg1index_compress ++;
                    }
                  
                    pixels_all[j/increment * w/increment + i/increment] = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);
                    //pixels[j/increment * w/increment + i/increment] = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);             
                   if( j < h/2)
                   {
                       pixels_region2[reg2index] = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);             
                       reg2index ++;
                   }
                   else
                   {
                        pixels_region3[reg3index] = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);
                        reg3index ++;
                   }
                    if( j > h/4 && j<3*h/4)
                    {
                        pixels[reg1index] = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);             
                        reg1index ++;
                        
                        
                        
                    }
                   
                }
            }
            //System.out.println(reg1index + " >> " + h + " , " + w);
            
            
            sobelMap = sobelMapping(map);
            
          
            int averageVal = fiveRegions(map);
             for(int j = h/2-1; j >= 0; j -- )
            {
                points = 0;
                recursiveCatch(map, sobelMap, hitMap,averageVal, w/4, j);
            }
            /*
            for(int j = 0; j<hitMap.length; j ++)
            {
                for(int i = 0; i<hitMap[0].length; i ++)
                {
                    if(hitMap[j][i] == 1)
                    {
                        g.setColor(Color.blue);
                        g.drawRect(i*2,j*2,2,2);
                    }
                }
            }
             /* 
             * 
             * 
             * 
             * 
             * 
             */
            /*
            for(int j = 0; j<hitMap.length; j ++)
            {
                //System.out.println(sobelMap[1][j][59]);
                if(sobelMap[1][j][59] > 0)
                {
                    g.setColor(Color.green);
                    g.drawLine(59*2, j*2, 59*2 + sobelMap[1][j][59] / 10, j*2);
                }
                else
                {
                    g.setColor(Color.red);
                    g.drawLine(59*2, j*2, 59*2 + sobelMap[1][j][59] / 10, j*2);
                }
            }*/
            
            //g.setColor(Color.yellow);
            //g.fillRect(59*2, 0, 2, hitMap.length*2);
            
            
            Polygon[] gons = createPolygon(hitMap);
            g.setColor(Color.yellow);
            g.drawPolygon(gons[1]);
            /*
            int[] hist = new int[256];
            for(int j = 0; j<map.length; j ++)
            {
                for(int i = 0; i<map[0].length; i ++)
                {
                    if(gons[0].contains(i,j))
                    {
                        hist[map[j][i]] ++;
                        if(hitMap[j][i] == 0)
                        {
                            g.setColor(Color.green);
                            //g.drawRect(i*2, j*2, 2, 2);
                        }
                        
                        if(map[j][i] >= 180)
                        {
                            g.setColor(Color.green);
                            //g.fillRect(i*2,j*2,2,2);
                        }
                        else
                            if(map[j][i] >= 150)
                            {
                                g.setColor(Color.red);
                                //g.fillRect(i*2,j*2,2,2);
                            }
                            else
                                if(map[j][i] >=  130)
                                {
                                    g.setColor(Color.orange);
                                    //g.fillRect(i*2,j*2,2,2);
                                }
                            
                        
                    }
                }
            }
             */
            /*
            for(int i = 0; i<256; i ++)
            {
                g.setColor(Color.green);
                g.drawLine(10+i, 400, 10+i, 400-(hist[i])/5);
                if((i % 32) == 0)
                    g.drawString(""+i, 10+i, 420);
            }
            */
            
            //BufferedImage bufImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
            //Graphics bufG = (Graphics)bufImg.getGraphics();
            //bufG.drawImage(img, 0, 0, null);
            int drawY_basis = 0;
            //if(preferredRegion == COMPRESS)preferredRegion = ALL;
            switch(preferredRegion)
            {
                case ALL:
                    //System.out.println("pixels_all.length: " + pixels_all.length + ", w = " + w + " & h = " + h);
                    canny.process_fast(pixels_all, w, h);
                    radBegin = 10;   radEnd = 80;    radIncrement = 10;
                    drawY_basis = 0;
                    
                    break;
                case TOP:
                    canny.process_fast(pixels_region2, w, h/2);
                    radBegin = 10;   radEnd = 26;    radIncrement = 4;
                    drawY_basis = 0;
                    break;
                case BOTTOM:
                    canny.process_fast(pixels_region3, w, h/2);
                    radBegin = 45;   radEnd = 80;    radIncrement = 5;
                    drawY_basis = h/2;
                    break;
                case CENTER:
                    canny.process_fast(pixels, w, h/2);
                    radBegin = 25;   radEnd = 45;    radIncrement = 5;
                    drawY_basis = h/4;
                    break;
                case COMPRESS:
                    canny.process_fast(pixels_compress, w/2, h/2);
                    radBegin = 5;   radEnd = 40; radIncrement = 5;
                    drawY_basis = 0;
                    break;
                default:
                    break;
            }
            cannyImage = canny.getEdgesImage();
            g.drawImage(cannyImage, 0, img.getHeight(null), null);
            
            
            /*
            BufferedImage bufImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics bufG = (Graphics)bufImg.getGraphics();
            bufG.drawImage(img, 0, 0, null);
            CannyEdgeDetector cc = new CannyEdgeDetector();
            cc.setSourceImage(bufImg);
            cc.process();
            g.drawImage(cc.getEdgesImage(), img.getWidth(null), 0, null);
             */
            /*
             CED_fast ced1_compress = new CED_fast();
            //ced.setSourceImage(bufImg);
            ced1_compress.process_fast(pixels_compress, w/2, h/2/2 );
            BufferedImage resultImg1_compress = ced1_compress.getEdgesImage();
            g.drawImage(resultImg1_compress, img.getWidth(null), 5*img.getHeight(null)/4, resultImg1_compress.getWidth(), resultImg1_compress.getHeight(), null);
            */
            /*
            CED_fast ced1 = new CED_fast();
            //ced.setSourceImage(bufImg);
            ced1.process_fast(pixels, w, h/2 );
            BufferedImage resultImg1 = ced1.getEdgesImage();
            g.drawImage(resultImg1, 0, 5*img.getHeight(null)/4, resultImg1.getWidth(), resultImg1.getHeight(), null);
            */
            /*
            CED_fast ced = new CED_fast();
            //ced.setSourceImage(bufImg);
            ced.process_fast(pixels_region2, w, h/2 );
            BufferedImage resultImg = ced.getEdgesImage();
            g.drawImage(resultImg, img.getWidth(null), img.getHeight(null) , resultImg.getWidth(), resultImg.getHeight(), null);
            
            CED_fast ced3 = new CED_fast();
            //ced.setSourceImage(bufImg);
            ced3.process_fast(pixels_region3, w, h/2 );
            BufferedImage resultImg3 = ced3.getEdgesImage();
            g.drawImage(resultImg3, img.getWidth(null), img.getHeight(null) + 1*h/2, resultImg3.getWidth(), resultImg3.getHeight(), null);
            */
            
            /*
            CED_fast ced = new CED_fast();
            //ced.setSourceImage(bufImg);
            ced.process_fast(pixels, w/2, h/2 );
            BufferedImage resultImg = ced.getEdgesImage();
            g.drawImage(resultImg, 0, img.getHeight(null), resultImg.getWidth(), resultImg.getHeight(), null);
           */
            int bestAccumulatorValue = 0;
            int bestLocationX = 0;
            int bestLocationY = 0;
            int bestRadius = 0;
            int maxx = canny.getWidth();
            int maxy = canny.getHeight();
            int hf_width = canny.getWidth();
            int hf_height = canny.getHeight();
            
            guessMinX = Math.max(0, lastCenterX - 3*lastRadius);
            guessMaxX = Math.min(w, lastCenterX + 3*lastRadius);
            guessMinY = Math.max(0, lastCenterY - 3*lastRadius);
            guessMaxY = Math.min(h, lastCenterY + 3*lastRadius);
            
            int[] scaledRegion = new int[]{guessMinX, Math.max(0, lastCenterY - 3*lastRadius - drawY_basis), guessMaxX, Math.min(hf_height, lastCenterY + 3*lastRadius - drawY_basis) };
            
            g.setColor(Color.orange);
            g.drawRect(guessMinX, guessMinY, guessMaxX - guessMinX, guessMaxY - guessMinY);
            
            g.setColor(Color.green);
            g.drawRect(scaledRegion[0], scaledRegion[1] + img.getHeight(null), scaledRegion[2] - scaledRegion[0], scaledRegion[3] - scaledRegion[1]);
            int[] hf_region = new int[]{scaledRegion[0], scaledRegion[1], scaledRegion[2] - scaledRegion[0], scaledRegion[3] - scaledRegion[1]};
            //System.out.printf(">> %d, %d, %d, %d \n" , hf_region[0], hf_region[1], hf_region[2], hf_region[3]);
            //hf.setRegion(new int[]{0,0, maxx, maxy});
            if(preferredRegion == COMPRESS) hf_region = new int[]{0,0, maxx, maxy};
            hf.setRegion(hf_region);
            hf.createSourceArray(canny.getData(), hf_width, hf_height);
            
            
            for(int rad = radBegin; rad <= radEnd; rad += radIncrement)
            {
                //System.out.println("RAD: " + rad);
                hf.setParams(rad, rad + 7);
                //hf.setSourceImage(cannyImage, new int[]{0,0, cannyImage.getWidth(), cannyImage.getHeight()});
                
                //hf.resetChangeableArrays(new int[]{0,0, maxx, maxy});
                hf.resetChangeableArrays(hf_region);
                //hf.process(true);
                
                hf.process(false);
                
                
                //if(rad <= 20)g.drawImage(hf.accumulatorImage(), img.getWidth(null), 0, null);
                java.util.LinkedList<Point> ps = hf.getPossibilityList();
                for(int i = 0; i<ps.size(); i ++)
                {
                    Point p = ps.get(i);
                    //.setColor(Color.white);
                    double drawX = p.getX();
                   
                    
                    double drawY = p.getY() + drawY_basis;
                    
                    if(preferredRegion == COMPRESS)
                    {
                        drawX = drawX*2;
                        drawY = (drawY - drawY_basis)*2 + drawY_basis;
                    }
                    
                    //g.fillOval((int)drawX - 5, (int)drawY - 5, 10, 10);
                    //g.setColor(Color.red);
                    //g.drawOval((int)drawX - rad, (int)drawY - rad, 2*rad, 2*rad);
                    
                    if(hf.valList.get(i) > bestAccumulatorValue && gons[1].contains(drawX,drawY))
                    {
                        bestLocationX = (int)drawX;
                        bestLocationY = (int)drawY;
                        bestRadius = rad;
                        bestAccumulatorValue = hf.valList.get(i);
                    }
                    
                    //g.fillOval((int)(p.getX())*2 - 5, (int)(p.getY())*2 - 5, 10 , 10);
                    //g.setColor(Color.yellow);
                    //g.drawOval((int)(p.getX())*2 - rad*2,  (int)(p.getY())*2 - rad*2, 2*rad*2 , 2*rad*2);
                    //System.out.println(hf.valList.get(i) + " @ " + p.getX() + " , " + p.getY() + " && rad = " + rad);
                     
                    //g.drawString(acc[(int)p.getY()-1][(int)p.getX()-1] + "", img.getWidth(null) + (int)p.getX(), img.getHeight(null) + (int)p.getY() + 15);
                }
            }
            
            //g.setColor(Color.green);
            //g.drawOval((int)bestLocationX - bestRadius, (int)bestLocationY - bestRadius, 2*bestRadius, 2*bestRadius);
            
            boolean ballFound = true;
            if(bestRadius > 0)
            {
                if(preferredRegion == COMPRESS)
                {
                    lastRadius = bestRadius*2;
                    lastCenterX = bestLocationX;
                    lastCenterY = bestLocationY;
                    lastDrawYBasis = drawY_basis;
                    consecutiveNoFinds = 0;
                }
                else
                {
                    lastRadius = bestRadius;
                    lastCenterX = bestLocationX;
                    lastCenterY = bestLocationY;
                    lastDrawYBasis = drawY_basis;
                    consecutiveNoFinds = 0;
                }
                
                if(preferredRegion != COMPRESS)
                    if(lastCenterY < 2*h/5)
                    {
                        preferredRegion = TOP;
                    }
                    else
                        if(lastCenterY > 3*h/5)
                        {
                            preferredRegion = BOTTOM;
                        }
                        else
                            preferredRegion = CENTER;
                
            }
            else
            {
                preferredRegion = (preferredRegion == COMPRESS) ? ALL : COMPRESS; //COMPRESS;
                consecutiveNoFinds ++;
                
            }
            int yDist = (int)(-0.46667*(double)lastRadius+34.6667);
            //System.out.println("yDist: " + yDist);
            if(consecutiveNoFinds < 3)
            {
                g.setColor(Color.blue);
                g.drawOval((int)lastCenterX - lastRadius, (int)lastCenterY  - lastRadius, 2*lastRadius, 2*lastRadius);
                
                int cameraX = cameraPosition * 2;
                if(isSerialConnected && trackBall)
                {
                    if(lastCenterX > 170)
                    {
                        int difference = 0;
                        if(serialMoveMethod == RADIUS)
                            difference = (int)((double)(lastCenterX - 160)/(double)lastRadius * 2.25);
                        if(serialMoveMethod == XSPOT)
                            difference = (int)((double)(lastCenterX - 160)/2);
                        
                        cameraPosition += difference;
                        if(cameraPosition > maxCameraPosition)cameraPosition = maxCameraPosition;
                    }
                    if(lastCenterX < 150)
                    {
                        int difference = 0;
                        if(serialMoveMethod == RADIUS)
                            difference = (int)((double)(lastCenterX - 160)/(double)lastRadius * 2.25);
                        /*if(serialMoveMethod == XSPOT)
                            difference = (int)((double)(lastCenterX - 160)/2);*/
                        
                        cameraPosition += difference;
                        if(cameraPosition < 10) cameraPosition = 10;
                    }
                }
                if(isSerialConnected && trackBall)
                {
                    sm.write((byte)cameraPosition);
                }
                
            }
            else
            {
                g.setColor(Color.red);
                g.drawString("NOT FOUND!!!", img.getWidth(null) + 20, img.getHeight(null));
                lastCenterX = img.getWidth(null)/2;
                lastCenterY = img.getHeight(null)/2;
                lastRadius = img.getWidth(null);
                ballFound = false;
                //cameraPosition = (char)80;
                if(isSerialConnected && trackBall)
                {
                    if(cameraPosition > 80) cameraPosition -= 2;
                    if(cameraPosition < 70) cameraPosition += 2;
                    sm.write((byte)cameraPosition);
                }
            }
            //System.out.println(cameraPosition + "      " + (byte)cameraPosition);
            
            /*
            for(int rad = 10; rad <= 30; rad += 5)
            {
                hf.setParams(rad, rad + 15 );
                hf.setSourceImage(resultImg1,  new int[]{0,0,resultImg1.getWidth(),resultImg1.getHeight()});
                hf.process(true);
                //g.drawImage(hf.accumulatorImage(), img.getWidth(null),0,null);
                java.util.LinkedList<Point> ps = hf.getPossibilityList();
                //if(rad == 5)  g.setColor(Color.green);
                //if(rad == 10) g.setColor(Color.blue);
                //if(rad == 15) g.setColor(Color.red);
                //if(rad == 20) g.setColor(Color.magenta);
                //if(rad == 25) g.setColor(Color.orange);
                int[][] acc = hf.accumulator;
                for(int i = 0; i<ps.size(); i ++)
                {
                    Point p = ps.get(i);
                    //if(gons[1].contains(p))
                        g.setColor(Color.red);
                    //else 
                        g.setColor(Color.white);
                    double drawX = p.getX();
                    double drawY = p.getY() + img.getHeight(null) + img.getHeight(null)/4;
                    g.fillOval((int)drawX - 5, (int)drawY - 5, 10, 10);
                    g.setColor(Color.red);
                    g.drawOval((int)drawX - rad, (int)drawY - rad, 2*rad, 2*rad);
                    
                    //g.fillOval((int)(p.getX())*2 - 5, (int)(p.getY())*2 - 5, 10 , 10);
                    //g.setColor(Color.yellow);
                    //g.drawOval((int)(p.getX())*2 - rad*2,  (int)(p.getY())*2 - rad*2, 2*rad*2 , 2*rad*2);
                    //System.out.println(hf.valList.get(i) + " @ " + p.getX() + " , " + p.getY() + " && rad = " + rad);
                     
                    //g.drawString(acc[(int)p.getY()-1][(int)p.getX()-1] + "", img.getWidth(null) + (int)p.getX(), img.getHeight(null) + (int)p.getY() + 15);
                }
                
            }
              
             */ 
            //System.out.println("***");
             
             
            //hf.process();
            
            //g.drawImage(hf.accumulatorImage(), img.getWidth(null), 0, null);
           
           currFPS ++;
           
           g.setColor(Color.red);
           g.drawString("FPS: " + lastFPS, 10, 20);
           
           g.setColor(Color.black);
           g.drawString("Region: " + regionList[preferredRegion], img.getWidth(null) + 10, 20);
           g.drawString("Radius: " + bestRadius, img.getWidth(null) + 10, 35);
           g.drawString("Position: (" + bestLocationX + " , " + bestLocationY + ")",img.getWidth(null) + 10 , 50);
           g.drawString("Accumulator: " + bestAccumulatorValue, img.getWidth(null) + 10, 65);
           g.drawString("Camera Position: " + (byte)cameraPosition, img.getWidth(null) + 10, 80);
           g.drawString("Serial Connected: " + isSerialConnected, img.getWidth(null) + 10, 95);
           
           /*
           g.setColor(Color.gray);
           int sx = img.getWidth(null) + 120;
           int disW = 120;
           int sy = img.getHeight(null)*2 - 20;
           g.drawRect(sx, sy, disW, 20);
           g.setColor(Color.red);
           g.drawRect(sx - 10 + (int)( ((double)disW) / (double)maxCameraPosition * (double)cameraPosition), sy, 20,20);
           g.setColor(Color.black);
           int syy = (int)((((double)disW) / 18.0)*36.0);
           g.fillRect(sx, sy - syy, disW, syy);
           
           
           if(ballFound && 1 == 0)
           {
               g.setColor(Color.white);
               int cppx = (int)((float)cameraPosition * (float)disW / (float)maxCameraPosition);
               int bx = (int)(((double)disW / (double)img.getWidth(null))*(lastCenterX)) + sx - disW/2 + cppx;
               int by = (int)(((double)syy / (double)img.getHeight(null)) * ((lastCenterY) + 2*(float)lastRadius)) + sy-syy;
               g.fillOval(bx - 5, by - 5, 10, 10);
           }
            */
           
        }catch(Exception exc)
        {
            System.out.println(exc);
            exc.printStackTrace();
        }
		/*BufferedImage buffImg = new BufferedImage(img.getWidth(this), img.getHeight(this),
				BufferedImage.TYPE_INT_RGB);*/
            
           
            //g.setColor(Color.red);
            //g.drawString("PETE!!!!", 100,100);
            //System.out.println("True");
        }
    }
    public int fiveRegions(int[][] grayImage)
    {
        int w = grayImage[0].length;
        int h = grayImage.length;
        
        int avgMid = 0, avgLeft = 0, avgRight = 0, avgUp = 0, avgDown = 0;
               
        for(int j = h/2-3; j<=h/2+3; j ++)
        {
            for(int i = w/2-3; i<=w/2+3; i ++)
            {
                avgMid += grayImage[j][i];
            }

            for(int i = w/3-3; i<=w/3+3; i ++)
            {
                avgLeft += grayImage[j][i];
            }

            
            for(int i = 2*w/3-3; i<= 2*w/3+3; i ++)
            {
                avgRight += grayImage[j][i];
            }

        }
               
        for(int i = w/2-3; i<=w/2+3; i ++)
        {
            for(int j = h/3-3; j<=h/3+3; j ++)
            {
                avgUp += grayImage[j][i];
            }
            
            for(int j = 2*h/3-3; j<=2*h/3+3; j ++)
            {
                avgDown += grayImage[j][i];
            }
        }
       
        int min = Math.min(avgUp, Math.min(avgDown, Math.min(avgLeft, Math.min(avgRight, avgMid))));
        int max = Math.max(avgUp, Math.max(avgDown, Math.max(avgLeft, Math.max(avgRight, avgMid))));
        
        //System.out.println(min/49 + " , " + max/49 );
        
        int avg = ((avgUp + avgDown + avgLeft + avgRight + avgMid) - (min + max)) / 3 /49;
                
        return  avg;
        
    }
    public void findBlob(int[][] redMap, int[][] hitMap, int i, int j, int hn)
    {
        if(redMap[j][i] == 1 && hitMap[j][i] == 0) hitMap[j][i] = hn;
        else return;
        
        if(i+1 < redMap[0].length) findBlob(redMap, hitMap, i+1, j, hn); //right
        if(i-1 >= 0) findBlob (redMap, hitMap, i-1, j, hn); //left
        if(j+1 < redMap.length) findBlob(redMap, hitMap, i, j+1, hn); //down
        if(j -1 >= 0) findBlob(redMap, hitMap, i, j-1, hn); //up
        
        return;
        
       
        
    }
    public int points = 0;
    
    public Polygon[] createPolygon(int[][] hitmap)
    {
        Polygon toReturn = new Polygon();
        Polygon toReturn2 = new Polygon();
        int num_in_required = 4;
        for(int j = hitmap.length-1; j >= 0; j -- )
        {
            int inwards = 0;
            for(int i = 0; i < 2*hitmap[0].length/3; i ++)
            {
                if(hitmap[j][i] == 1)
                {
                    inwards ++;
                    if(inwards == num_in_required)
                    {
                        toReturn.addPoint(i,j);
                        toReturn2.addPoint(i*2,j*2);
                        break;
                    }
                }
            }
        }
        for(int j = 0; j < hitmap.length; j ++ )
        {
            int inwards = 0;
            for(int i = hitmap[0].length-1; i > hitmap[0].length/3; i --)
            {
                
                if(hitmap[j][i] == 1)
                {
                    inwards ++;
                    if(inwards == num_in_required)
                    {
                        toReturn.addPoint(i,j);
                        toReturn2.addPoint(i*2,j*2);
                        break;
                    }
                }
            }
        }
        return new Polygon[]{toReturn, toReturn2};
    }
    public void recursiveCatch(int[][] map, int[][][] gradient, int[][] hitMap, int avg, int i, int j)
    {
        if(points > 6000) return;
        points ++;
        if(hitMap[j][i] == 0)
        {
            if(map[j][i] <= (double)avg*1.25 && Math.abs(gradient[0][j][i]) < 200 && Math.abs(gradient[1][j][i]) < 200)
            {
                hitMap[j][i] = 1;
                if(i + 1 < map[0].length)recursiveCatch(map, gradient, hitMap, avg, i+1, j);
                if(j - 1 >= 0) recursiveCatch(map, gradient, hitMap,avg, i, j-1);
                if(i - 1 >= 0) recursiveCatch(map, gradient, hitMap,avg, i-1, j);
                if(j + 1 < map.length) recursiveCatch(map, gradient, hitMap,avg, i, j+1);
            }
            else
            {
                if(Math.abs(gradient[0][j][i]) < 120 && Math.abs(gradient[1][j][i]) < 120 && map[j][i] <= (double)avg*1.4)
                {
                    hitMap[j][i] = 1;
                    
                    if(i + 1 < map[0].length)recursiveCatch(map, gradient, hitMap,avg, i+1, j);
                    if(i - 1 >= 0) recursiveCatch(map, gradient, hitMap,avg, i-1, j);
                    if(j + 1 < map.length) recursiveCatch(map, gradient, hitMap,avg, i, j+1);
                    if(j - 1 >= 0) recursiveCatch(map, gradient, hitMap,avg, i, j-1);
                }
                else
                    return;
            }
        }
        else
        {
            return;
        }
    }
    public void tryRecur(int[][] map, int[][] sobel, int[][] out, int i, int j, int[][] hitmap)
    {
        //System.out.println(i+","+j);
        if(i < map[0].length && i>=0 && j>=0 && j<map.length && hitmap[j][i] == 0)
        {
            out[j][i] = 0;
        
            if(map[j][i] <= roiBasis + (int)((double)roiBasis * roiDeviation))
            {
                out[j][i] = 1;
                hitmap[j][i] = 1;
                    tryRecur(map, sobel, out, i + 1, j, hitmap);//right
                    tryRecur(map, sobel, out, i - 1, j, hitmap);//left
                    tryRecur(map, sobel, out, i, j + 1, hitmap); //down
                    tryRecur(map, sobel, out, i, j - 1, hitmap);//up

            }
            else
                if(sobel[j][i] <= 200)
                {
                    hitmap[j][i] = 1;
                    out[j][i] = 1;
                    tryRecur(map, sobel, out, i + 1, j, hitmap);//right
                    tryRecur(map, sobel, out, i - 1, j, hitmap);//left
                    tryRecur(map, sobel, out, i, j + 1, hitmap); //down
                    tryRecur(map, sobel, out, i, j - 1, hitmap);//up
                }
        }
        return;
        
            
       
    }
    public int[] blobArea(int[][] hitMap, int hn)
    {
        int[] res = new int[hn];
        for(int j = 0; j<hitMap.length; j ++)
        {
            for(int i = 0; i<hitMap[0].length; i ++)
            {
                res[hitMap[j][i]] ++;
            }
        }
        return res;
    }
    public int[][][] sobelMapping(int[][] grayImage)
    {
        int[][][] result = new int[2][grayImage.length][grayImage[0].length];
        //int[][] resultY = new int[grayImage.length][grayImage[0].length];
        
        for(int j = 0; j<grayImage.length; j ++)
        {
            for(int i = 0; i<grayImage[0].length; i ++)
            {
                
                            if( !(j - 1 < 0 || j+1 >= grayImage.length
                                    || i - 1 < 0 || i + 1 >= grayImage[0].length))
                            {
                                result[0][j][i] = (grayImage[j-1][i-1] *   sobelX[0][0] + 
                                                grayImage[j-1][i]   *   sobelX[0][1] +
                                                grayImage[j-1][i+1] *   sobelX[0][2] +
                                                grayImage[j][i-1]   *   sobelX[1][0] +
                                                grayImage[j][i]     *   sobelX[1][1] +
                                                grayImage[j][i+1]   *   sobelX[1][2] +
                                                grayImage[j+1][i-1] *   sobelX[2][0] +
                                                grayImage[j+1][i]   *   sobelX[2][1] +
                                                grayImage[j+1][i+1] *   sobelX[2][2]);
                                
                                result[1][j][i] = (grayImage[j-1][i-1] *   sobelY[0][0] + 
                                                grayImage[j-1][i]   *   sobelY[0][1] +
                                                grayImage[j-1][i+1] *   sobelY[0][2] +
                                                grayImage[j][i-1]   *   sobelY[1][0] +
                                                grayImage[j][i]     *   sobelY[1][1] +
                                                grayImage[j][i+1]   *   sobelY[1][2] +
                                                grayImage[j+1][i-1] *   sobelY[2][0] +
                                                grayImage[j+1][i]   *   sobelY[2][1] +
                                                grayImage[j+1][i+1] *   sobelY[2][2]);
                            }
                            else
                            {
                                result[0][j][i] = 0;
                                result[1][j][i] = 0;
                            }
                       
                
                
                
            }
        }
        
        return result;
    }
    
    public class FPS_Listener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            lastFPS = currFPS;
            currFPS = 0;
            
            if(isSerialConnected)
            {
            closePort ++;
            
                if(closePort == 60)
                {
                    //sm.closePort();
                    //isSerialConnected = false;
                    //System.out.println("port closed");
                }
                else
                {

                }
            }
            /*
            else if(closePort < 20)
            {
                System.out.println(""+5*closePort);
                sm.writeChar((char)((int)(5*closePort)));
            }
             */
        }
    }
    private static void getList() {
            // TODO Auto-generated method stub
            Vector devices =
                    (Vector) CaptureDeviceManager.getDeviceList(null);
            System.out.println(devices.size());
            Enumeration enumz = devices.elements();

            while (enumz.hasMoreElements()) {
                    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enumz.nextElement();
                    String name = cdi.getName();
                    javax.media.Format[] fmts = cdi.getFormats();

                    System.out.println(name);

                    for (int i = 0; i < fmts.length; i++) {
                            //System.out.println(fmts[i]);
                    System.out.println(fmts[i]);

                    if (name.startsWith("vfw:")){

                        System.out.println("" + fmts[i]);
                    }
                            //CaptureDeviceManager.removeDevice(cdi);
                    }

            }
    }
    
} 
