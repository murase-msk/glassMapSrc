package servlet;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import servlet.sample.DrawSimpleRoad;
import servlet.sample.DrawSimplifiedStroke;
import servlet.sample.Test;
import servlet.sample.HelloWorld;



/**
 * メインのサーブレット
 * Servlet implementation class MainServlet
 */
@WebServlet(name="MainServlet",urlPatterns={"/MainServlet"})// このアノテーションでweb.xml不要になる.
public class MainServlet extends HttpServlet{

    /**
     * @see HttpServlet#HttpServlet()
     */
    public MainServlet() {
        super();
    }

    /**
	 * @see HttpServlet#HttpServlet()
	 * getリクエスト  http://localhost/projectName/MainServlet?
	 * type=...&.....
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String url = ((HttpServletRequest)request).getRequestURL().toString();
		String queryString = ((HttpServletRequest)request).getQueryString();
		System.out.println(url + "?" + queryString); 
		System.out.println("client IP address : "+getClientIpAddr(request));
		
		// パラメータの受け取り.
		String type="";	// サーバのやること.
		
		if(request.getParameter("type")==null){
			ErrorMsg.errorResponse(request, response, "typeパラメータがありません");
			return;
		}
		long start = System.currentTimeMillis();

		
		type = request.getParameter("type");
		switch(type){
		case "DrawGlue_v2":
			new DrawGlue_v2(request, response);
			break;
		case "DrawMitinariSenbetuAlgorithm":
			new DrawMitinariSenbetuAlgorithm(request, response);
			break;
		case "DrawElasticStrokeConnectivity":
			new DrawElasticStrokeConnectivity_v2(request, response);
			break;
		case "DrawElasticStroke_v2":
			new DrawElasticStroke_v2(request, response);
			break;
		case "DrawElasticRoad":
			new DrawElasticRoad(request, response);
			break;
		case "DrawSimplifiedStroke":
			new DrawSimplifiedStroke(request, response);
			break;
		case "DrawSimpleRoad":
			new DrawSimpleRoad(request, response);
			break;
		case "ConvertElasticPoints":
			new ConvertElasticPoints(request, response);
			break;
		case "Test":
			new Test(request, response);
			break;
		case "HelloWorld":
			new HelloWorld(request, response);
			break;
		default:
			ErrorMsg.errorResponse(request, response, "typeパラメータの値が正しくありません");
			return;
		}
		
		//計測したい処理を記述

		long end = System.currentTimeMillis();
		System.out.println((end - start)  + "ms");

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * postリクエスト
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	
	public static String getClientIpAddr(HttpServletRequest request) {  
        String ip = request.getHeader("X-Forwarded-For");  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }  
        return ip;  
    }  
}
