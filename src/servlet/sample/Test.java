package servlet.sample;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omg.CORBA.PRIVATE_MEMBER;

import sun.java2d.pipe.DrawImage;

/**
 * 
 * @author murase
 *
 */
public class Test {

	public Test(HttpServletRequest request, HttpServletResponse response) {
		
		
		try{
			OutputStream out=response.getOutputStream();
			//BufferedImage img = emgd.getEmGlueImage(param);
			ImageIO.write( drawImage(), "png", out);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		
		
	}
	
	private BufferedImage drawImage(){
		BufferedImage bfImage=null;
		bfImage=new BufferedImage( 100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2d = (Graphics2D) bfImage.getGraphics();
		
		graphics2d.setColor(Color.red);
		graphics2d.drawLine(0,0,10,10);
		System.out.println("ok");
		
		return bfImage;
	}

}
