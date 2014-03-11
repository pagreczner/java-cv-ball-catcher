//Peter Greczner

import java.awt.image.*;
import java.util.*;
import java.awt.*;

public class HoughFilter {
    
    public BufferedImage sourceImage;
    public int[][] accumulator, regionalMax;
    public int radius, threshold;
    public int[] region;
    public int[][] sourceArray;
    public Raster raster;
    public LinkedList<Integer> xlist;
    public LinkedList<Integer> ylist;
    public int height, width;
    public LinkedList<Point> possibilityList;
    public int[] masterData;
    public int[][] zeroAccumulator;
    
    public HoughFilter(BufferedImage img, int r, int thresh, int[] reg)
    {
        if(img != null)
        {
            sourceImage = img;
            raster = sourceImage.getData();
            accumulator = new int[img.getHeight()][img.getWidth()];
            height = img.getHeight();
            width = img.getWidth();
        }
        radius = r;
        threshold = thresh;
        region = reg;
        xlist = new LinkedList<Integer>();
        ylist = new LinkedList<Integer>();
        
        //createArray();
        
    }
    
    public HoughFilter(int[] data, int r, int thresh, int[] reg, int w, int h)
    {
        
        
        /*
        if(img != null)
        {
            sourceImage = img;
            raster = sourceImage.getData();
            accumulator = new int[img.getHeight()][img.getWidth()];
            height = img.getHeight();
            width = img.getWidth();
        }
         */
        
        xlist = new LinkedList<Integer>();
        ylist = new LinkedList<Integer>();
                        
        sourceArray = new int[h][w];
                       
        for(int j = 0; j<h; j ++)
        {
            for(int i = 0; i<w; i ++)
            {
                sourceArray[j][i] = (data[j*w + i] != 0) ? 255 : 0;
                if(sourceArray[j][i] == 255)
                {
                    sourceArray[j][i] = 1;
                    xlist.add(i);
                    ylist.add(j);
                }
                accumulator[j][i] = 0;
                    
            }
        }
        
        
        
        radius = r;
        threshold = thresh;
        region = reg;
        xlist = new LinkedList<Integer>();
        ylist = new LinkedList<Integer>();
        
        //createArray();
        
    }
    public void setMasterData(int[] md)
    {
        masterData = md;
    }
    public void setParams(int r, int thresh)
    {
        radius = r;
        threshold = thresh;
    }
    
    public int[][] getRegionalMax()
    {
        return regionalMax;
    }
    
    public LinkedList<Point> getPossibilityList()
    {
        return possibilityList;
    }
    
    public BufferedImage accumulatorImage()
    {
        BufferedImage toRet = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = toRet.getGraphics();
        
        int c = 0;
        int max = 0, mi = 0, mj = 0;
        for(int j = 0; j < accumulator.length; j ++)
        {
            for(int i = 0; i<accumulator[0].length; i ++)
            {
                c = accumulator[j][i];
                
                if(c == 0)
                    g.setColor(Color.black);
                else
                {
                    c = Math.min(c*8, 255);
                    g.setColor(new Color(c,c,c));
                    if(regionalMax[j][i] == 1)
                    {
                       //g.setColor(Color.white);
                        //g.fillOval(i-2, j-2, 4, 4);
                        //possibilityList.add(new Point(i,j));
                    }
                }
               
                //g.setColor(new Color(c,c,c));
                g.fillRect(i,j, 1, 1);
                
                //System.out.print(c+",");
                
            }
        }
       
        return toRet;
    }
    
    public void setSourceImage(BufferedImage img, int[] reg)
    {
        sourceImage = img;
        raster = sourceImage.getData();
        accumulator = new int[img.getHeight()][img.getWidth()];
        possibilityList = new LinkedList<Point>();
        valList = new LinkedList<Integer>();
        height = img.getHeight();
        width = img.getWidth();
        region = reg;
        
    }
    public void setRegion(int[] reg)
    {
        region = reg;
    }
    public void createSourceArray(int[] data, int w, int h)
    {
        xlist = new LinkedList<Integer>();
        ylist = new LinkedList<Integer>();
        height = h;
        width = w;
        sourceArray = new int[h][w];
        zeroAccumulator = new int[h][w];   
        accumulator = new int[h][w];
        for(int j = 0; j<h; j ++)
        {
            for(int i = 0; i<w; i ++)
            {
                sourceArray[j][i] = (data[j*w + i] == -1) ? 255 : 0;
                if(sourceArray[j][i] == 255)
                {
                    if(j >= region[1] && i >= region[0] && i< (region[2] + region[0] - 1) && j < (region[3] + region[1] - 1))
                    {
                        sourceArray[j][i] = 1;
                        xlist.add(i);
                        ylist.add(j);
                    }
                }
                zeroAccumulator[j][i] = 0;
                accumulator[j][i] = 0;
                    
            }
        }
    }
    
    public void resetChangeableArrays(int[] reg)
    {
        //xlist = new LinkedList<Integer>();
        //ylist = new LinkedList<Integer>();
        valList = new LinkedList<Integer>();
        possibilityList = new LinkedList<Point>();
        for(int j = 0; j<accumulator.length; j ++)Arrays.fill(accumulator[j],0);
        //accumulator = zeroAccumulator;
        region = reg;
        
    }
    
    public void setSourceImage_fast(int[] data, int[] reg, int w, int h)
    {
        xlist = new LinkedList<Integer>();
        ylist = new LinkedList<Integer>();
        valList = new LinkedList<Integer>();
        possibilityList = new LinkedList<Point>();
        accumulator = new int[h][w];
        width = w;
        height = h;
                        
        sourceArray = new int[h][w];
                       
        for(int j = 0; j<h; j ++)
        {
            for(int i = 0; i<w; i ++)
            {
                sourceArray[j][i] = (data[j*w + i] != 0) ? 255 : 0;
                if(sourceArray[j][i] == 255)
                {
                    sourceArray[j][i] = 1;
                    xlist.add(i);
                    ylist.add(j);
                }
                accumulator[j][i] = 0;
                    
            }
        }
        
        
        
        //radius = r;
        //threshold = thresh;
        region = reg;
        //xlist = new LinkedList<Integer>();
        //ylist = new LinkedList<Integer>();
    }
    
    public void createArray()
    {
        xlist = new LinkedList<Integer>();
        ylist = new LinkedList<Integer>();
        
        int w = sourceImage.getWidth();
        int h = sourceImage.getHeight();
                
        sourceArray = new int[h][w];
        int[] pixels = new int[h * w];
        int[] dummy = new int[10];
               
        
        for(int j = 0; j<h; j ++)
        {
            for(int i = 0; i<w; i ++)
            {
                sourceArray[j][i] = raster.getPixel(i,j , dummy)[0];
                if(sourceArray[j][i] == 255)
                {
                    sourceArray[j][i] = 1;
                    xlist.add(i);
                    ylist.add(j);
                }
                accumulator[j][i] = 0;
                    
            }
        }
    
    }
   
    public void process(boolean b)
    {
        
        if(b) createArray();
        
        int xmin = region[0];
        int xmax = xmin + region[2] - 1;
        int ymin = region[1];
        int ymax = ymin + region[3] - 1;
        
        //System.out.println(xmin+","+ymin+";"+xmax+","+ymax);
        
        int xmx, xpx, ypx, ymx;
        int ypy, ymy, xpy, xmy;
        int xCenter, yCenter;
        int x, y;
        double d;
        boolean ypxin, ymxin, xpxin, xmxin;
        boolean ypyin, ymyin, xpyin, xmyin;
        
        //System.out.println("xlist: " + xlist.size());
        
        for(int count = 0; count<xlist.size(); count ++)
        {
            
            xCenter = xlist.get(count);
            yCenter = ylist.get(count);
            
            xmx = xCenter - radius;
            xpx = xCenter + radius;
            ypx = yCenter + radius;
            ymx = yCenter - radius;
            
            if(xpx < xmin || xmx>xmax || ypx<ymin || ymx>ymax)
            {
                
                break;
            }
            if(ypx < height && ymx >= 0 && xpx < width && xmx >= 0)
            {
                x = 1;
                y = radius;
                d = 5/4 - radius;
                accumulator[ypx][xCenter] = accumulator[ypx][xCenter] + 1;
                accumulator[ymx][xCenter] = accumulator[ymx][xCenter] + 1;
                accumulator[yCenter][xpx] = accumulator[yCenter][xpx] + 1;
                accumulator[yCenter][xmx] = accumulator[yCenter][xmx] + 1;
                while (y > x)
                {
                    xpx = xCenter + x;
                    xmx = xCenter - x;
                    ypy = yCenter + y;
                    ymy = yCenter - y;
                    ypx = yCenter + x;
                    ymx = yCenter - x;
                    xpy = xCenter + y;
                    xmy = xCenter - y;
                    accumulator[ypy][xpx] = accumulator[ypy][xpx] + 1;
                    accumulator[ymy][xpx] = accumulator[ymy][xpx] + 1;
                    accumulator[ypy][xmx] = accumulator[ypy][xmx] + 1;
                    accumulator[ymy][xmx] = accumulator[ymy][xmx] + 1;
                    accumulator[ypx][xpy] = accumulator[ypx][xpy] + 1;
                    accumulator[ymx][xpy] = accumulator[ymx][xpy] + 1;
                    accumulator[ypx][xmy] = accumulator[ypx][xmy] + 1;
                    accumulator[ymx][xmy] = accumulator[ymx][xmy] + 1;
                    if( d < 0)
                    {
                        d = d + x * 2 + 3;
                        x = x + 1;
                    }
                    else
                    {
                        d = d + (x - y) * 2 + 5;
                        x = x + 1;
                        y = y - 1;
                    }
                }
                if(x == y)
                {
                    xpx = xCenter+x; xmx = xCenter-x;
                    ypy = yCenter+y; ymy = yCenter-y;
                    accumulator[ypy][xpx] = accumulator[ypy][xpx] + 1;
                    accumulator[ymy][xpx] = accumulator[ymy][xpx] + 1;
                    accumulator[ypy][xmx] = accumulator[ypy][xmx] + 1;
                    accumulator[ymy][xmx] = accumulator[ymy][xmx] + 1;
                }
                
            }
            else
            {
                ypxin = ypx>=ymin && ypx<=ymax;
                ymxin = ymx>=ymin && ymx<=ymax;
                xpxin = xpx>=xmin && xpx<=xmax;
                xmxin = xmx>=xmin && xmx<=xmax;
                
                if(ypxin) accumulator[ypx][xCenter] = accumulator[ypx][xCenter] + 1;  
                if(ymxin) accumulator[ymx][xCenter] = accumulator[ymx][xCenter] + 1;   
                if(xpxin) accumulator[yCenter][xpx] = accumulator[yCenter][xpx] + 1;  
                if(xmxin) accumulator[yCenter][xmx] = accumulator[yCenter][xmx] + 1; 
                x = 1;
                y = radius;
                d = 5/4 - radius;
                while(y > x)
                {
                    xpx = xCenter+x; xpxin = xpx>=xmin & xpx<=xmax;
                    xmx = xCenter-x; xmxin = xmx>=xmin & xmx<=xmax;
                    ypy = yCenter+y; ypyin = ypy>=ymin && ypy<=ymax;
                    ymy = yCenter-y; ymyin = ymy>=ymin && ymy<=ymax;
                    ypx = yCenter+x; ypxin = ypx>=ymin && ypx<=ymax;
                    ymx = yCenter-x; ymxin = ymx>=ymin && ymx<=ymax;
                    xpy = xCenter+y; xpyin = xpy>=xmin && xpy<=xmax;
                    xmy = xCenter-y; xmyin = xmy>=xmin && xmy<=xmax;
                    if(ypyin && xpxin) accumulator[ypy][xpx] = accumulator[ypy][xpx] + 1; 
                    if(ymyin && xpxin) accumulator[ymy][xpx] = accumulator[ymy][xpx] + 1; 
                    if(ypyin && xmxin) accumulator[ypy][xmx] = accumulator[ypy][xmx] + 1; 
                    if(ymyin && xmxin) accumulator[ymy][xmx] = accumulator[ymy][xmx] + 1; 
                    if(ypxin && xpyin) accumulator[ypx][xpy] = accumulator[ypx][xpy] + 1; 
                    if(ymxin && xpyin) accumulator[ymx][xpy] = accumulator[ymx][xpy] + 1; 
                    if(ypxin && xmyin) accumulator[ypx][xmy] = accumulator[ypx][xmy] + 1; 
                    if(ymxin && xmyin) accumulator[ymx][xmy] = accumulator[ymx][xmy] + 1; 
                    if(d < 0)
                    {
                        d = d + x * 2 + 3;
                        x = x + 1;
                    }
                    else
                    {
                        d = d + (x - y) * 2 + 5;
                        x = x + 1;
                        y = y - 1;
                    }
                      
                }
                if(x == y)
                {
                    xpx = xCenter+x; xpxin = xpx>=xmin & xpx<=xmax;
                    xmx = xCenter-x; xmxin = xmx>=xmin & xmx<=xmax;
                    ypy = yCenter+y; ypyin = ypy>=ymin & ypy<=ymax;
                    ymy = yCenter-y; ymyin = ymy>=ymin & ymy<=ymax;
                    if(ypyin && xpxin) accumulator[ypy][xpx] = accumulator[ypy][xpx] + 1; 
                    if(ymyin && xpxin) accumulator[ymy][xpx] = accumulator[ymy][xpx] + 1; 
                    if(ypyin && xmxin) accumulator[ypy][xmx] = accumulator[ypy][xmx] + 1; 
                    if(ymyin && xmxin) accumulator[ymy][xmx] = accumulator[ymy][xmx] + 1; 
                }
                
            
            }
            
        }
        
        
        regionalMax = computeRegionalMax(accumulator, 16, threshold);
        
        
        
        
    }
    public LinkedList<Integer> valList;
    public int[][] computeRegionalMax(int[][] in, int window, int thresh)
    {
        int out[][] = new int[in.length][in[0].length];
        for(int j = 0; j<in.length; j += window)
        {
            for(int i = 0; i<in[0].length; i += window)
            {
                int regMax = 0;
                for(int y = j; y<j+window; y ++)
                {
                    for(int x = i; x<i+window; x ++)
                    {
                        if(x < in[0].length && x>=0 && y<in.length && y>=0)
                        {
                            //System.out.println(x+","+y+" :: " + in[0].length + ", " + in.length);
                            regMax = Math.max(regMax,in[y][x]);
                        }
                    }
                    
                }
                for(int y = j; y<j+window; y ++)
                {
                    for(int x = i; x<i+window; x ++)
                    {
                        if(x < in[0].length && x>=0 && y<in.length && y>=0)
                        {
                            if(in[y][x] == regMax && in[y][x] >= thresh)
                            {
                                out[y][x] = 1;
                                possibilityList.add(new Point(x,y));
                                valList.add(in[y][x]);
                                //System.out.println(in[y][x] + " @ ("+x+","+y+")");
                                
                            }
                            else
                            {
                                out[y][x] = 0;
                            }
                        }
                    }
                    
                }
                
                
            }
        }
        return out;
    }
    
    

}
