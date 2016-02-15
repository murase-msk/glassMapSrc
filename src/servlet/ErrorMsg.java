package servlet;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ErrorMsg {

	public ErrorMsg() {
		
	}
	
	/**
	 * 何かしらのエラーで
	 * @param request
	 * @param response
	 */
	public static void errorResponse(HttpServletRequest request, HttpServletResponse response, String errorMsg){
		try{
			response.setContentType("text/xml; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<?xml version = \"1.0\" encoding = \"UTF-8\" ?>");
			out.println("<error>");
			out.println(errorMsg);
			out.println("</error>");
			out.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
